
**最后更新**：2025-11-07
# WebSocket RPC 服务端

基于 Spring Boot 的 WebSocket RPC 服务器，实现 Java 与浏览器 JavaScript 之间的双向远程过程调用。

## 📋 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [开发指南](#开发指南)
- [常见问题](#常见问题)

---

## 🛠 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Spring WebSocket | 2.7.18 | WebSocket 支持 |
| Jackson | 2.13.x | JSON 序列化 |
| Lombok | 1.18.x | 代码简化 |
| Java | 1.8+ | 运行环境 |

---

## 📁 项目结构（可直接看api）

```
java_rpc_spring/
├── src/main/java/org/example/
│   ├── Main.java                           # 主入口
│   └── websocket/
│       ├── WebSocketRpcApplication.java    # Spring Boot 启动类
│       ├── RpcClientProxy.java             # RPC 客户端代理（静态调用入口）
│       ├── config/
│       │   └── WebSocketConfig.java        # WebSocket 配置
│       ├── handler/
│       │   └── RpcWebSocketHandler.java    # WebSocket 消息处理器
│       ├── model/
│       │   ├── RpcRequest.java             # RPC 请求模型
│       │   └── RpcResponse.java            # RPC 响应模型
│       └── controller/
│           └── RpcTestController.java      # HTTP 测试接口
└── src/main/resources/
    └── application.properties              # 应用配置
```

---

## 🚀 快速开始

### 1. 构建项目

```bash
cd java_rpc_spring
mvn clean package -DskipTests
```

生成文件：`target/cake_jsprc.jar`

### 2. 启动服务

```bash
java -jar target/cake_jsprc.jar
```

启动成功后会看到：
```
WebSocket RPC 服务已启动，地址: ws://0.0.0.0:10087/ws
等待浏览器客户端连接...
```

### 3. 验证服务

```bash
# 检查服务状态
curl http://localhost:10087/api/rpc/clients

# 响应示例
{"clientCount":0,"connected":false}
```

---

## 📡 API 文档

### WebSocket 接口

#### 连接地址
```
ws://localhost:10087/ws
```

#### 消息协议

**1. RPC 请求（服务器 → 浏览器）**
```json
{
  "id": "abc123",           // 请求ID（用于匹配响应）
  "action": "methodName",   // 要调用的方法名
  "params": [arg1, arg2]    // 参数列表
}
```

**2. RPC 响应（浏览器 → 服务器）**
```json
{
  "callbackId": "abc123",   // 对应请求的ID
  "status": 200,            // 状态码（200成功、404方法不存在、500错误）
  "result": "返回值"        // 执行结果
}
```

### HTTP REST API

#### 1. 获取客户端连接数
```http
GET /api/rpc/clients
```

**响应**：
```json
{
  "clientCount": 1,
  "connected": true
}
```

#### 2. 测试 RPC 调用
```http
GET /api/rpc/test
```

**功能**：测试调用浏览器注册的 `base64` 和 `addSafe` 方法

**响应**：
```json
{
  "success": true,
  "clientCount": 1,
  "base64_input": "Hello from Burp via HTTP!",
  "base64_output": "SGVsbG8gZnJvbSBCdXJwIHZpYSBIVFRQIQ==",
  "add_input": "100 + 200",
  "add_output": 300,
  "message": "RPC调用成功"
}
```

#### 3. 动态调用方法
```http
POST /api/rpc/call
Content-Type: application/json

{
  "action": "base64",
  "params": ["test"]
}
```

**响应**：
```json
{
  "success": true,
  "action": "base64",
  "result": "dGVzdA=="
}
```

#### 4. 获取客户端注册的方法列表
```http
GET /api/rpc/methods
```

**功能**：获取浏览器端已注册的所有方法名列表

**响应**：
```json
{
  "success": true,
  "methods": ["base64", "addSafe", "getRegisteredMethods"],
  "count": 3,
  "clientCount": 1,
  "message": "成功获取注册方法列表"
}
```

---


## ⚙️ 注册方法：
```js
client.register('a', (resolve, a, b) => {
  var res = exec
  resolve(res); // 计算并返回结果
});

function addSafe(a, b) {//自定义方法
  const numA = Number(a);
  const numB = Number(b);

  if (isNaN(numA) || isNaN(numB)) {
    throw new Error('参数必须是有效的数字');
  }

  return numA + numB;
}
client.register('addSafe', (resolve,a,b) => {//addSafe，注册完成后在yakit调用 ，参考3. 动态调用方法
  var res = addSafe(a,b)
  resolve(res); // 返回页面标题给服务器
});
```
## yakit语法：enc为js注册函数名、data为要加密的值。可自定义添加到afterRequest、beforeRequest、hijackHTTPResponse
```coderc为例
# codec plugin

/*
Codec Plugin 可以支持在 Codec 中自定义编码解码，自定义 Bypass 与字符串处理函数

函数定义非常简单

func(i: string) string
*/

handle = func(data) {  
    # 构造请求体  
    //dataa = {}
    requestBody = {  
        "action": "enc",  
        "params": [data]  
    }  
    # 发送 HTTP POST 请求  
    rsp, err = poc.Post(  
        "http://127.0.0.1:10087/api/rpc/call",  
        poc.json(requestBody),  
        poc.timeout(30000)  
    )~
      
    // if err != nil {  
    //     return sprintf("请求失败: %v", err)  
    // }  
      
    # 解析响应  
    result = rsp.GetBody()  
    return json.Find(result, "$.result") 
}

```

## ⚙️ 配置说明

### application.properties

```properties
# 服务端口
server.port=10087

# 日志级别
logging.level.cake.jsrpc.websocket=DEBUG
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SERVER_PORT` | 服务端口 | 10087 |
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | - |

**使用示例**：
```bash
java -jar  cake_jsprc.jar
```

---

## 💻 开发指南

### 核心组件说明

#### 1. RpcClientProxy（外部调用入口）

**用途**：供外部代码（如 Burp 插件）调用浏览器方法

```java
// 设置 ApplicationContext（应用启动时调用）
RpcClientProxy.setApplicationContext(context);

// 调用浏览器方法
Object result = RpcClientProxy.call("base64", "hello");

// 获取连接数
int count = RpcClientProxy.getClientCount();
```

#### 2. RpcWebSocketHandler（核心处理器）

**职责**：
- 管理 WebSocket 客户端连接
- 处理客户端消息
- 实现同步 RPC 调用

**关键方法**：
```java
// 调用浏览器方法（同步，阻塞等待结果）
public Object callBrowserMethod(WebSocketSession session, String action, Object... params)

// 获取连接的客户端数量
public int getConnectedClientCount()

// 获取客户端注册的方法列表
public List<String> getRegisteredMethods()
```

#### 3. RpcTestController（测试接口）

提供 HTTP 接口方便测试和调试，适合：
- 验证 RPC 功能
- 检查客户端连接
- 自动化测试

### 添加新的 HTTP 接口

```java
@RestController
@RequestMapping("/api/rpc")
public class RpcTestController {
    
    @GetMapping("/custom")
    public Map<String, Object> customMethod() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 调用浏览器方法
            Object data = RpcClientProxy.call("yourMethod", param1, param2);
            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
```

### 自定义 WebSocket 路径

修改 `WebSocketConfig.java`：

```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(rpcWebSocketHandler, "/your-custom-path")
            .setAllowedOrigins("*");
}
```

---

## 🔍 调试指南

### 查看日志

所有日志输出到控制台，包括：
- 客户端连接/断开信息
- RPC 请求和响应
- 调试信息（`[DEBUG]` 标记）

**示例**：
```
[INFO] 客户端 a1b2c3d4 已连接，当前在线: [a1b2c3d4]
[发送请求] 方法: base64, 参数: [hello], 请求ID: xyz789
[客户端 a1b2c3d4 消息] 原始数据: {"callbackId":"xyz789","status":200,"result":"aGVsbG8="}
```

### 调试技巧

1. **启用详细日志**：
```properties
logging.level.cake.jsrpc.websocket=DEBUG
logging.level.org.springframework.web.socket=DEBUG
```

2. **使用 WebSocket 客户端工具**：
   - Chrome 扩展：Simple WebSocket Client
   - 在线工具：websocket.org/echo.html

3. **测试命令**：
```bash
# 查看连接状态
watch -n 1 'curl -s http://localhost:10087/api/rpc/clients'

# 测试 RPC 调用
curl http://localhost:10087/api/rpc/test

# 获取注册的方法列表
curl http://localhost:10087/api/rpc/methods
```

---

## 🐛 常见问题

### Q1: 启动失败，提示端口被占用

**错误信息**：
```
Web server failed to start. Port 10087 was already in use.
```

**解决方案**：
```bash
# 方案1：更改端口
java -jar -Dserver.port=8080 cake_jsprc.jar

# 方案2：查找并终止占用进程
lsof -ti:10087 | xargs kill -9
```

### Q2: RPC 调用超时

**可能原因**：
1. 浏览器客户端未连接
2. 方法未在浏览器端注册
3. 网络延迟过高

**排查步骤**：
```bash
# 1. 检查客户端连接
curl http://localhost:10087/api/rpc/clients

# 2. 查看服务端日志
# 确认是否收到客户端响应

# 3. 增加超时时间（修改 RpcWebSocketHandler.java）
resultHolder.wait(30000); // 改为30秒
```

### Q3: 无法连接 WebSocket

**检查清单**：
- [ ] 服务是否正常启动（检查日志）
- [ ] 防火墙是否允许 10087 端口
- [ ] WebSocket URL 是否正确（`ws://` 不是 `wss://`）
- [ ] 跨域配置是否正确

**测试连接**：
```javascript
// 浏览器控制台执行
const ws = new WebSocket('ws://localhost:10087/ws');
ws.onopen = () => console.log('连接成功');
ws.onerror = (e) => console.error('连接失败', e);
```

### Q4: JSON 解析错误

**错误信息**：
```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
```

**解决方案**：
在 `RpcResponse.java` 添加注解：
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResponse {
    // ...
}
```

### Q5: ApplicationContext 为 null

**错误信息**：
```
RPC服务器未初始化，请先启动WebSocket RPC Application
```

**原因**：`Main.java` 未正确设置 ApplicationContext

**确认代码**：
```java
public static void main(String[] args) {
    context = SpringApplication.run(WebSocketRpcApplication.class, args);
    RpcClientProxy.setApplicationContext(context); // ← 必须调用
}
```

---

## 📊 性能优化

### 1. 连接池配置

**application.properties**：
```properties
# WebSocket 连接池
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
```

### 2. 超时配置

**修改等待时间**（`RpcWebSocketHandler.java`）：
```java
// 调整等待时间（毫秒）
resultHolder.wait(10000); // 10秒
```

### 3. 内存优化

**启动参数**：
```bash
java -Xms256m -Xmx512m -jar cake_jsprc.jar
```

---

## 🔐 安全建议

1. **生产环境配置**：
   - 限制 WebSocket 允许的源（修改 `setAllowedOrigins("*")`）
   - 启用 HTTPS/WSS
   - 添加认证机制

2. **限制访问**：
```java
registry.addHandler(rpcWebSocketHandler, "/ws")
        .setAllowedOrigins("https://yourdomain.com");
```

3. **输入验证**：
   - 验证 action 方法名（防止任意方法调用）
   - 限制参数大小
   - 添加请求频率限制

---

## 📞 技术支持

- **日志位置**：控制台输出
- **配置文件**：`src/main/resources/application.properties`
- **源码位置**：`src/main/java/org/example/websocket/`

---

## 📝 更新日志

### v0.0.1-SNAPSHOT
- ✅ 实现 WebSocket RPC 基础功能
- ✅ 添加 HTTP REST API
- ✅ 支持多客户端连接
- ✅ 同步 RPC 调用机制
- ✅ 自动重连支持（客户端）

---

**最后更新**：2025-11-07
