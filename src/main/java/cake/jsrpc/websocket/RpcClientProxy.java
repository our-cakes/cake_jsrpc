package cake.jsrpc.websocket;

import cake.jsrpc.websocket.handler.RpcWebSocketHandler;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC客户端代理类，供外部（如Burp插件）调用
 * 通过Spring上下文获取Handler并调用其方法
 */
public class RpcClientProxy {
    
    private static ApplicationContext applicationContext;
    
    /**
     * 设置Spring应用上下文（在应用启动时调用）
     */
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
        System.out.println("[DEBUG] RpcClientProxy.setApplicationContext() 被调用");
        System.out.println("[DEBUG] ApplicationContext: " + (context != null ? context.getClass().getSimpleName() : "null"));
    }
    
    /**
     * 调用浏览器端的RPC方法
     * @param action 方法名
     * @param params 参数列表
     * @return 执行结果
     * @throws Exception 调用异常
     */
    public static Object call(String action, Object... params) throws Exception {
        if (applicationContext == null) {
            throw new RuntimeException("RPC服务器未初始化，请先启动WebSocket RPC Application");
        }
        
        RpcWebSocketHandler handler = applicationContext.getBean(RpcWebSocketHandler.class);
        return handler.invokeRemoteMethod(action, params);
    }
    
    /**
     * 获取当前连接的客户端数量
     */
    public static int getClientCount() {
        System.out.println("[DEBUG] RpcClientProxy.getClientCount() 被调用");
        System.out.println("[DEBUG] applicationContext 是否为null: " + (applicationContext == null));
        
        if (applicationContext == null) {
            System.out.println("[DEBUG] applicationContext为null，返回0");
            return 0;
        }
        
        try {
            RpcWebSocketHandler handler = applicationContext.getBean(RpcWebSocketHandler.class);
            System.out.println("[DEBUG] 获取到RpcWebSocketHandler: " + handler);
            int count = handler.getConnectedClientCount();
            System.out.println("[DEBUG] 返回客户端数量: " + count);
            return count;
        } catch (Exception e) {
            System.err.println("[ERROR] 获取Handler失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
        
    /**
     * 获取客户端注册的方法列表
     * 
     * @return 方法名列表
     */
    public static List<String> getRegisteredMethods() {
        System.out.println("[DEBUG] RpcClientProxy.getRegisteredMethods() 被调用");
            
        if (applicationContext == null) {
            System.out.println("[DEBUG] applicationContext为null，返回空列表");
            return new ArrayList<>();
        }
            
        try {
            RpcWebSocketHandler handler = applicationContext.getBean(RpcWebSocketHandler.class);
            List<String> methods = handler.getRegisteredMethods();
            System.out.println("[DEBUG] 获取到注册方法: " + methods);
            return methods;
        } catch (Exception e) {
            System.err.println("[ERROR] 获取注册方法失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
