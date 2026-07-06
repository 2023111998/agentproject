package com.campus.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 商品响应 DTO — 类型化商品查询结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private int id;
    private String storeId;
    private String name;
    private double price;
    private int stock;
    private double rating;
    private String tag;
    private String msg;         // 操作结果消息

    public ProductResponse() {}

    // -- getters/setters --
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}
