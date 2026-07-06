package com.campus.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 订单服务响应 DTO — 强类型服务契约。
 * 替代原有的 Map<String, Object> 裸类型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String orderId;
    private String userId;
    private String storeId;
    private String riderId;
    private List<String> items;
    private double amount;
    private String type;        // 外卖/电商
    private String status;
    private String address;
    private int placedMinAgo;
    private String createdAt;
    private String error;       // 非空表示异常

    public OrderResponse() {}

    // -- getters/setters --
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getPlacedMinAgo() { return placedMinAgo; }
    public void setPlacedMinAgo(int placedMinAgo) { this.placedMinAgo = placedMinAgo; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    /** 是否成功 */
    public boolean isSuccess() { return error == null; }
}
