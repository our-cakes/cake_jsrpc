package cake.jsrpc.websocket.config;

import cake.jsrpc.websocket.handler.RpcWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RpcWebSocketHandler rpcWebSocketHandler;

    // 注入自定义处理器
    public WebSocketConfig(RpcWebSocketHandler rpcWebSocketHandler) {
        this.rpcWebSocketHandler = rpcWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册处理器，映射路径为 "/ws"，允许跨域
        registry.addHandler(rpcWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
    
    // 配置WebSocket容器属性，增加消息缓冲区大小
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置文本消息缓冲区大小为10MB（默认通常为8KB）
        container.setMaxTextMessageBufferSize(10 * 1024 * 1024);
        // 设置二进制消息缓冲区大小为10MB
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        // 设置连接空闲超时时间为5分钟
        container.setMaxSessionIdleTimeout(5 * 60 * 1000L);
        // 设置消息部分发送超时时间
        container.setAsyncSendTimeout(30000L);
        return container;
    }
}