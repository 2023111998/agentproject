package com.campus.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/** 订单模型 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {
    private String id;
    private String userId;
    private String storeId;
    private String riderId;
    private String items;       // JSON字符串
    private double amount;
    private String type;        // 外卖/电商
    private String status;
    private String address;
    private int placedMinAgo;
    private LocalDateTime createdAt;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
