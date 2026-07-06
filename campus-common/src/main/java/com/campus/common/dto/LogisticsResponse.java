package com.campus.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * 物流响应 DTO — 类型化物流查询结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogisticsResponse {

    // 跟踪信息
    private String orderId;
    private String riderId;
    private String status;
    private String eta;
    private Double lat;
    private Double lng;
    private boolean timedOut;

    // 路线地图
    private boolean hasMapData;
    private Map<String, Object> store;
    private Map<String, Object> destination;
    private List<Map<String, Object>> waypoints;
    private Map<String, Object> rider;
    private int riderIdx;
    private double progress;

    private String msg;         // 操作结果或错误消息

    public LogisticsResponse() {}

    // -- getters/setters --
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public boolean isTimedOut() { return timedOut; }
    public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }
    public boolean isHasMapData() { return hasMapData; }
    public void setHasMapData(boolean hasMapData) { this.hasMapData = hasMapData; }
    public Map<String, Object> getStore() { return store; }
    public void setStore(Map<String, Object> store) { this.store = store; }
    public Map<String, Object> getDestination() { return destination; }
    public void setDestination(Map<String, Object> destination) { this.destination = destination; }
    public List<Map<String, Object>> getWaypoints() { return waypoints; }
    public void setWaypoints(List<Map<String, Object>> waypoints) { this.waypoints = waypoints; }
    public Map<String, Object> getRider() { return rider; }
    public void setRider(Map<String, Object> rider) { this.rider = rider; }
    public int getRiderIdx() { return riderIdx; }
    public void setRiderIdx(int riderIdx) { this.riderIdx = riderIdx; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}
