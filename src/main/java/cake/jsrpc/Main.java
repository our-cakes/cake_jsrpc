package cake.jsrpc;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import cake.jsrpc.websocket.WebSocketRpcApplication;
//public class Main {
//    public static void main(String[] args) {
//        System.out.println("Main!!!");
//    }
//}

public class Main {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        // 确保正确启动 WebSocketRpcApplication
        context = SpringApplication.run(WebSocketRpcApplication.class, args);
        
        // 注册应用上下文到RpcClientProxy
        cake.jsrpc.websocket.RpcClientProxy.setApplicationContext(context);

        // 添加自定义启动信息
        System.out.println("WebSocket RPC 服务已启动，地址: ws://0.0.0.0:10087/ws");
        System.out.println("等待浏览器客户端连接...");

        // 保持应用运行
        //registerShutdownHandler();
    }

    private static void keepApplicationRunning() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private static void  registerShutdownHandler() {
        if (context != null && context.isRunning()) {
            SpringApplication.exit(context);
            //api.logging().logToOutput("RPC服务器已停止");
        }
    }
}