package com.campus.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 物流模型 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Logistics {
    private int id;
    private String orderId;
    private String riderId;
    private String status;
    private String eta;
    private Double lat;
    private Double lng;
    private boolean timedOut;

    public Logistics() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
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
}
