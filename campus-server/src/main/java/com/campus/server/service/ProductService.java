package com.campus.server.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 商品服务：封装商品相关业务逻辑 */
@Service
public class ProductService {

    private final JdbcTemplate db;

    public ProductService(JdbcTemplate db) { this.db = db; }

    public List<Map<String, Object>> listProducts() {
        return db.queryForList("SELECT * FROM products WHERE stock>0");
    }

    public Map<String, Object> addProduct(Map<String, Object> body) {
        db.update("INSERT INTO products(store_id,name,price,stock,tag) VALUES(?,?,?,?,?)",
            body.get("store_id"), body.get("name"), body.getOrDefault("price", 0),
            body.getOrDefault("stock", 0), body.getOrDefault("tag", ""));
        return Map.of("msg", "商品已上架");
    }

    public Map<String, Object> updateProduct(int id, Map<String, Object> body) {
        if (body.containsKey("name"))  db.update("UPDATE products SET name=? WHERE id=?", body.get("name"), id);
        if (body.containsKey("price")) db.update("UPDATE products SET price=? WHERE id=?", body.get("price"), id);
        if (body.containsKey("stock")) db.update("UPDATE products SET stock=? WHERE id=?", body.get("stock"), id);
        if (body.containsKey("tag"))   db.update("UPDATE products SET tag=? WHERE id=?", body.get("tag"), id);
        return Map.of("id", id, "msg", "商品已更新");
    }

    public Map<String, Object> deleteProduct(int id) {
        db.update("UPDATE products SET stock=0 WHERE id=?", id);
        return Map.of("msg", "商品已下架");
    }
}
