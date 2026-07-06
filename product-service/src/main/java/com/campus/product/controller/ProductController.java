package com.campus.product.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class ProductController {

    private final JdbcTemplate db;
    public ProductController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/products")
    public List<Map<String, Object>> listProducts() {
        return db.queryForList("SELECT * FROM products WHERE stock>0");
    }

    @GetMapping("/products/{name}")
    public Map<String, Object> getProduct(@PathVariable String name) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM products WHERE name=?", name);
        return rows.isEmpty() ? Map.of("error", "商品不存在") : rows.get(0);
    }

    @GetMapping("/store/{sid}/products")
    public List<Map<String, Object>> storeProducts(@PathVariable String sid) {
        return db.queryForList("SELECT * FROM products WHERE store_id=? AND stock>0", sid);
    }

    @PostMapping("/products")
    public Map<String, Object> addProduct(@RequestBody Map<String, Object> body) {
        db.update("INSERT INTO products(store_id,name,price,stock,tag) VALUES(?,?,?,?,?)",
            body.get("store_id"), body.get("name"), body.getOrDefault("price", 0),
            body.getOrDefault("stock", 0), body.getOrDefault("tag", ""));
        return Map.of("msg", "商品已上架");
    }

    @PutMapping("/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable int id, @RequestBody Map<String, Object> body) {
        if (body.containsKey("name"))    db.update("UPDATE products SET name=? WHERE id=?", body.get("name"), id);
        if (body.containsKey("price"))   db.update("UPDATE products SET price=? WHERE id=?", body.get("price"), id);
        if (body.containsKey("stock"))   db.update("UPDATE products SET stock=? WHERE id=?", body.get("stock"), id);
        if (body.containsKey("tag"))     db.update("UPDATE products SET tag=? WHERE id=?", body.get("tag"), id);
        return Map.of("id", id, "msg", "商品已更新");
    }

    @DeleteMapping("/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable int id) {
        db.update("UPDATE products SET stock=0 WHERE id=?", id);
        return Map.of("msg", "商品已下架");
    }
}
