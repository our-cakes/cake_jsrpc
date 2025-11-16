package cake.jsrpc.websocket.controller;

import cake.jsrpc.websocket.RpcClientProxy;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC测试控制器，提供HTTP接口供Burp插件调用
 */
@RestController
@RequestMapping("/api/rpc")
public class RpcTestController {

    /**
     * 测试调用浏览器方法
     * GET /api/rpc/test
     */
    @GetMapping("/test")
    public Map<String, Object> testRpcCall() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查是否有浏览器客户端连接
            int clientCount = RpcClientProxy.getClientCount();
            result.put("clientCount", clientCount);
            
            if (clientCount == 0) {
                result.put("success", false);
                result.put("message", "没有浏览器客户端连接");
                return result;
            }
            
            // 调用base64方法
            String testString = "Hello from Burp via HTTP!";
            Object base64Result = RpcClientProxy.call("base64", testString);
            result.put("base64_input", testString);
            result.put("base64_output", base64Result);
            
            // 调用addSafe方法
            Object addResult = RpcClientProxy.call("addSafe", 100, 200);
            result.put("add_input", "100 + 200");
            result.put("add_output", addResult);
            
            result.put("success", true);
            result.put("message", "RPC调用成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "RPC调用失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
        }
        
        return result;
    }
    
    /**
     * 调用指定的浏览器方法
     * POST /api/rpc/call
     * Body: {"action": "base64", "params": ["test"]}
     */
    @PostMapping("/call")
    public Map<String, Object> callBrowserMethod(@RequestBody Map<String, Object> request) {
        System.out.println("[调试] 接收到的请求: " + request);
        Map<String, Object> result = new HashMap<>();
        
        try {
            String action = (String) request.get("action");
            Object[] params = request.containsKey("params") 
                ? ((java.util.List<?>) request.get("params")).toArray() 
                : new Object[0];
            
            Object callResult = RpcClientProxy.call(action, params);
            
            result.put("success", true);
            result.put("action", action);
            result.put("result", callResult);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "调用失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取客户端连接数
     * GET /api/rpc/clients
     */
    @GetMapping("/clients")
    public Map<String, Object> getClientCount() {
        Map<String, Object> result = new HashMap<>();
        int count = RpcClientProxy.getClientCount();
        result.put("clientCount", count);
        result.put("connected", count > 0);
        return result;
    }
    
    /**
     * 获取客户端注册的方法列表
     * GET /api/rpc/methods
     */
    @GetMapping("/methods")
    public Map<String, Object> getRegisteredMethods() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查是否有客户端连接
            int clientCount = RpcClientProxy.getClientCount();
            result.put("clientCount", clientCount);
            
            if (clientCount == 0) {
                result.put("success", false);
                result.put("message", "没有浏览器客户端连接");
                result.put("methods", new java.util.ArrayList<>());
                return result;
            }
            
            // 获取注册的方法列表
            java.util.List<String> methods = RpcClientProxy.getRegisteredMethods();
            
            result.put("success", true);
            result.put("methods", methods);
            result.put("count", methods.size());
            result.put("message", "成功获取注册方法列表");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
            result.put("methods", new java.util.ArrayList<>());
        }
        
        return result;
    }
}
