package cake.jsrpc.websocket.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 忽略未知字段（避免后续扩展字段报错）
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResponse {
    private String callbackId; // 对应请求的ID
    private int status; // 状态码（200成功、404方法不存在、500错误）
    private Object result; // 响应结果

    // 必须有默认构造函数（Jackson解析需要）
    public RpcResponse() {}

    // getter和setter
    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}