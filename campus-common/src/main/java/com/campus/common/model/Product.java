package com.campus.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 商品模型 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {
    private int id;
    private String storeId;
    private String name;
    private double price;
    private int stock;
    private double rating;
    private String tag;
    private String image;

    public Product() {}

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
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
