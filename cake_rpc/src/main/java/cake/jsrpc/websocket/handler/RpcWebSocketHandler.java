package cake.jsrpc.websocket.handler;

import cake.jsrpc.websocket.model.RpcRequest;
import cake.jsrpc.websocket.model.RpcResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RpcWebSocketHandler extends TextWebSocketHandler {

    // 存储连接的客户端会话（线程安全）
    private final Map<String, WebSocketSession> connectedClients = new ConcurrentHashMap<>();
    // 存储等待响应的请求（请求ID -> 结果容器）
    private final Map<String, Object[]> pendingRequests = new ConcurrentHashMap<>();
    // JSON 序列化工具
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    
    // 最大消息大小（10MB）
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;
    
    // 心跳检测执行器
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 启动心跳检测任务，每30秒检查一次连接状态
        heartbeatExecutor.scheduleAtFixedRate(this::checkConnections, 30, 30, TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void destroy() {
        heartbeatExecutor.shutdown();
    }
    
    /**
     * 检查所有连接的状态
     */
    private void checkConnections() {

        //System.out.println("[心跳检测] 当前连接数: " + connectedClients.size());
        for (Map.Entry<String, WebSocketSession> entry : connectedClients.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                System.out.println("[心跳检测] 发现已关闭的会话: " + entry.getKey());
                // 移除已关闭的会话
                connectedClients.remove(entry.getKey());
            }
        }
    }

    /**
     * 公共方法：供外部调用，向第一个连接的客户端发送RPC请求
     * @param action 要调用的方法名
     * @param params 参数列表
     * @return 调用结果
     */
    public Object invokeRemoteMethod(String action, Object... params) throws Exception {
        // 获取第一个可用的会话
        WebSocketSession session = getFirstAvailableSession();
        if (session == null) {
            throw new RuntimeException("没有可用的WebSocket客户端连接");
        }
        return callBrowserMethod(session, action, params);
    }

    /**
     * 获取第一个可用的WebSocket会话
     */
    private WebSocketSession getFirstAvailableSession() {
        for (WebSocketSession session : connectedClients.values()) {
            if (session.isOpen()) {
                return session;
            }
        }
        return null;
    }

    /**
     * 获取当前连接的客户端数量
     */
    public int getConnectedClientCount() {
        int count = connectedClients.size();
        System.out.println("[DEBUG] getConnectedClientCount() 被调用, 当前连接数: " + count);
        System.out.println("[DEBUG] 连接的客户端ID: " + connectedClients.keySet());
        return count;
    }
    
    /**
     * 获取客户端注册的所有方法列表
     * 通过调用浏览器端的 getRegisteredMethods 方法获取
     * 
     * @return 方法名列表
     * @throws Exception 调用失败时抛出异常
     */
    public List<String> getRegisteredMethods() throws Exception {
        // 获取第一个可用的客户端会话
        WebSocketSession session = getFirstAvailableSession();
        if (session == null) {
            throw new RuntimeException("没有可用的WebSocket客户端连接");
        }
        
        // 调用浏览器端的特殊方法获取注册的方法列表
        Object result = callBrowserMethod(session, "getRegisteredMethods");
        
        // 解析返回结果（假设浏览器返回逗号分隔的方法名字符串）
        if (result instanceof String) {
            String methodsStr = (String) result;
            if (methodsStr.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(methodsStr.split(","));
        }
        
        return new ArrayList<>();
    }

    /**
     * 客户端连接建立后触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientId = UUID.randomUUID().toString().substring(0, 8);
        connectedClients.put(clientId, session);
        System.out.printf("\n[INFO] 客户端 %s 已连接，当前在线: %s%n", clientId, connectedClients.keySet());
        System.out.printf("[INFO] WebSocket会话信息: remoteAddress=%s, id=%s%n", 
            session.getRemoteAddress(), session.getId());
    }

    /**
     * 处理客户端发送的消息（包括响应）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientId = getClientIdBySession(session);
        String payload = message.getPayload();
        
        // 检查消息大小
        if (payload.length() > MAX_MESSAGE_SIZE) {
            System.err.println("[错误] 接收到的消息超过最大大小限制: " + payload.length());
            return;
        }
        
        //System.out.printf("\n[客户端 %s 消息] 原始数据: %s%n", clientId, payload);

        try {
            // 尝试解析为响应（优先处理响应）
            RpcResponse response = objectMapper.readValue(payload, RpcResponse.class);
            if (response.getCallbackId() != null) {
                // 唤醒等待该响应的线程
                Object[] holder = pendingRequests.remove(response.getCallbackId());
                if (holder != null) {
                    synchronized (holder) {
                        holder[0] = response.getResult(); // 存储结果
                        holder.notify(); // 唤醒等待
                    }
                }
                return;
            }
        } catch (Exception e) {
            // 不是响应格式，可能是客户端主动发送的请求
            try {
                RpcRequest request = objectMapper.readValue(payload, RpcRequest.class);
                System.out.printf("[客户端 %s 请求] 方法=%s, 参数数量=%d, ID=%s%n",
                        clientId, request.getAction(), request.getParams().size(), request.getId());
            } catch (Exception ex) {
                // 可能是心跳消息或其他非RPC消息，直接忽略
                System.out.printf("[客户端 %s] 接收到非RPC消息，长度=%d%n", clientId, payload.length());
                return;
            }
        }
    }

    /**
     * 客户端连接关闭后触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String clientId = getClientIdBySession(session);
        if (clientId != null) {
            connectedClients.remove(clientId);
            System.out.printf("\n客户端 %s 已断开连接，当前在线: %s%n", clientId, connectedClients.keySet());
            System.out.printf("关闭状态: %s, 代码: %d%n", status.getReason(), status.getCode());
        }
    }
    
    /**
     * 传输错误时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String clientId = getClientIdBySession(session);
        System.err.printf("客户端 %s 传输错误: %s%n", clientId, exception.getMessage());
        super.handleTransportError(session, exception);
    }

    /**
     * 调用浏览器方法并返回结果（移除TimeUnit依赖）
     * 修改为public，允许外部通过invokeRemoteMethod间接调用
     */
    public Object callBrowserMethod(WebSocketSession session, String action, Object... params) throws Exception {
        String requestId = UUID.randomUUID().toString().substring(0, 10);

        // 构建请求
        RpcRequest request = new RpcRequest();
        request.setId(requestId);
        request.setAction(action);
        request.setParams(Arrays.asList(params)); // 兼容Java 8的集合操作

        // 发送请求
        String requestJson = objectMapper.writeValueAsString(request);
        
        // 检查请求大小
        if (requestJson.length() > MAX_MESSAGE_SIZE) {
            return "错误: 请求数据过大: " + requestJson.length() + " 字节";
        }
        
        session.sendMessage(new TextMessage(requestJson));
        System.out.printf("[发送请求] 方法: %s, 参数数量: %d, 请求ID: %s, 请求大小: %d 字节%n",
                action, params.length, requestId, requestJson.length());

        // 初始化结果容器并等待响应
        Object[] resultHolder = new Object[1]; // 用数组存储结果（可修改）
        pendingRequests.put(requestId, resultHolder);

        synchronized (resultHolder) {
            try {
                // 设置合理的超时时间
                long timeout = 30000; // 30秒超时
                
                // 如果参数中包含大量数据，适当增加超时时间
                for (Object param : params) {
                    if (param instanceof String) {
                        String strParam = (String) param;
                        // 如果字符串长度超过10KB，增加超时时间
                        if (strParam.length() > 10000) {
                            timeout = 60000; // 60秒超时
                            break;
                        }
                    }
                }
                
                resultHolder.wait(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "请求被中断";
            }
        }

        // 处理结果
        if (resultHolder[0] != null) {
            return resultHolder[0];
        } else {
            pendingRequests.remove(requestId); // 清理超时请求
            return "超时: 未收到响应";
        }
    }

    /**
     * 根据会话获取客户端ID
     */
    private String getClientIdBySession(WebSocketSession session) {
        for (Map.Entry<String, WebSocketSession> entry : connectedClients.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }
}