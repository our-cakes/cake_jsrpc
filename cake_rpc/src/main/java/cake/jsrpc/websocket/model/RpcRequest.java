package cake.jsrpc.websocket.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 服务器接收的客户端请求格式
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RpcRequest {
    private String id;         // 请求ID（用于匹配响应）
    private String action;     // 要调用的方法名
    private List<Object> params; // 方法参数列表
}