package cake.jsrpc.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebSocketRpcApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketRpcApplication.class, args);
        System.out.println("WebSocket RPC 服务已启动，地址: ws://0.0.0.0:10087/ws");
        System.out.println("等待浏览器客户端连接...");
    }
}