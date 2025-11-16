class WebSocketRpcClient {
	/**
	 * 初始化WebSocket RPC客户端
	 * @param {string} wsUrl - WebSocket服务器地址（如ws://localhost:10087/ws?group=test）
	 */
	constructor(wsUrl) {
		this.wsUrl = wsUrl;
		this.socket = null;
		this.methods = {}; // 存储注册的方法
		this.reconnectAttempts = 0; // 重连尝试次数
		this.maxReconnectAttempts = 10; // 最大重连尝试次数
		this.reconnectInterval = 3000; // 重连间隔时间（毫秒）
		this.heartbeatInterval = 25000; // 心跳间隔时间（毫秒）
		this.heartbeatTimer = null; // 心跳定时器
		this.connect();
	}

	/**
	 * 建立WebSocket连接
	 */
	connect() {
		// 检查是否超过最大重连尝试次数
		if (this.reconnectAttempts >= this.maxReconnectAttempts) {
			console.error("超过最大重连尝试次数，停止重连");
			return;
		}

		this.socket = new WebSocket(this.wsUrl);

		// 连接成功
		this.socket.onopen = (event) => {
			console.log("WebSocket连接已建立");
			this.reconnectAttempts = 0; // 重置重连尝试次数
		};

		// 接收服务器消息（处理RPC调用）
		this.socket.onmessage = (event) => {
			// 重置重连尝试次数
			this.reconnectAttempts = 0;
			
			// 添加调试信息
			console.log("[调试] 接收到的原始数据:", event.data);
			console.log("[调试] 接收到的数据长度:", event.data.length);
			try {
				const request = JSON.parse(event.data);
				this.handleRequest(request);
			} catch (error) {
				console.error("解析消息失败:", error);
			}
		};

		// 连接关闭时自动重连
		this.socket.onclose = (event) => {
			console.log("连接已关闭，尝试重连... 尝试次数:", this.reconnectAttempts + 1);
			this.reconnectAttempts++;
			
			// 检查关闭原因
			if (event.code === 1006) {
				console.warn("连接异常关闭，可能是网络问题或服务器问题");
			}
			
			// 延迟重连，避免过于频繁
			setTimeout(() => this.connect(), this.reconnectInterval);
		};
		
		// 错误处理
		this.socket.onerror = (error) => {
			console.error("WebSocket错误:", error);
		};
	}

	/**
	 * 注册供服务器调用的本地方法
	 * @param {string} name - 方法名
	 * @param {Function} func - 方法实现，第一个参数为返回结果的回调
	 */
	register(name, func) {
		this.methods[name] = func;
		console.log(`已注册方法: ${name}`);
	}

	/**
	 * 处理服务器的RPC请求
	 * @param {Object} request - 服务器发送的请求对象
	 */
	handleRequest(request) {
		// 请求格式: { id: "请求ID", action: "方法名", params: [参数1, 参数2] }
		const {
			id,
			action,
			params = []
		} = request;
		console.log("[调试] 接收到的请求:", request);
		console.log("[调试] 请求参数:", params);

		// 检查方法是否存在
		if (!this.methods[action]) {
			this.sendResponse(id, 404, `方法 ${action} 未注册`);
			return;
		}

		// 执行方法并返回结果
		try {
			const resolve = (result) => this.sendResponse(id, 200, result);
			console.log(resolve);
			//console.log(...params);
			this.methods[action](resolve, ...params);
		} catch (error) {
			this.sendResponse(id, 500, `执行错误: ${error.message}`);
		}
	}

	/**
	 * 向服务器发送响应
	 * @param {string} id - 对应请求的ID
	 * @param {number} status - 状态码(200成功, 404未找到, 500错误)
	 * @param {any} data - 响应数据
	 */
	sendResponse(id, status, data) {
		if (this.socket.readyState !== WebSocket.OPEN) {
			console.warn("WebSocket连接未打开，无法发送响应");
			return;
		}

		const response = {
			callbackId: id,
			status: status,
			result: data
		};
		
		// 添加调试信息
		console.log("[调试] 发送的响应:", response);
		const responseJson = JSON.stringify(response);
		console.log("[调试] 发送的JSON响应:", responseJson);
		
		// 发送响应
		try {
			this.socket.send(responseJson);
		} catch (error) {
			console.error("发送响应失败:", error);
		}
	}
}

// 使用示例
const client = new WebSocketRpcClient('ws://localhost:10087/ws');

// 注册一个特殊方法，返回所有已注册的方法名
client.register('getRegisteredMethods', (resolve) => {
	// 获取所有方法名，但排除 getRegisteredMethods 自身
	const methodNames = Object.keys(client.methods)
		.filter(name => name !== 'getRegisteredMethods')
		.join(',');
	resolve(methodNames);
});

//注册方法
client.register('base64', (resolve,a) => {
  var res = btoa(a)
  resolve(res); // 返回页面标题给服务器
});


console.log(`client.register('enc', (resolve, a) => {
	var aa = JSON.parse(a)
	var res = enc(aa)
	resolve(res); // 返回页面标题给服务器
});`);
// client.register('a', (resolve, a, b) => {
//   var res = exec
//   resolve(res); // 计算并返回结果
// });

// function addSafe(a, b) {
//   const numA = Number(a);
//   const numB = Number(b);

//   if (isNaN(numA) || isNaN(numB)) {
//     throw new Error('参数必须是有效的数字');
//   }

//   return numA + numB;
// }
// client.register('addSafe', (resolve,a,b) => {
//   var res = addSafe(a,b)
//   resolve(res); // 返回页面标题给服务器
// });