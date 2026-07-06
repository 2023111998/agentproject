package com.campus.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class OrderController {

    private final JdbcTemplate db;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public OrderController(JdbcTemplate db) { this.db = db; }

    // GET /orders/{id}
    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrder(@PathVariable String id) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", id);
        return rows.isEmpty() ? Map.of("error", "订单不存在") : rows.get(0);
    }

    // GET /users/{uid}/orders
    @GetMapping("/users/{uid}/orders")
    public List<Map<String, Object>> userOrders(@PathVariable String uid) {
        return db.queryForList("SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC", uid);
    }

    // GET /store/{sid}/orders
    @GetMapping("/store/{sid}/orders")
    public List<Map<String, Object>> storeOrders(@PathVariable String sid) {
        return db.queryForList("SELECT * FROM orders WHERE store_id=? ORDER BY created_at DESC", sid);
    }

    // GET /rider/{rid}/orders
    @GetMapping("/rider/{rid}/orders")
    public List<Map<String, Object>> riderOrders(@PathVariable String rid) {
        return db.queryForList("SELECT * FROM orders WHERE rider_id=? ORDER BY created_at DESC", rid);
    }

    // GET /available
    @GetMapping("/available")
    public List<Map<String, Object>> available() {
        return db.queryForList("SELECT * FROM orders WHERE status='已下单' ORDER BY created_at DESC");
    }

    // POST /orders
    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        String oid = (String) body.getOrDefault("order_id", "");
        if (oid.isEmpty()) return Map.of("error", "缺少order_id");
        // 正确序列化 items 为 JSON 字符串 (MySQL JSON 类型需要合法 JSON)
        String itemsJson;
        try {
            Object itemsObj = body.get("items");
            if (itemsObj instanceof String s) {
                itemsJson = s;  // 已经是 JSON 字符串
            } else if (itemsObj != null) {
                itemsJson = objectMapper.writeValueAsString(itemsObj);
            } else {
                itemsJson = "[]";
            }
        } catch (Exception e) {
            itemsJson = "[]";
        }
        db.update(
            "INSERT INTO orders(id,user_id,store_id,items,amount,type,status,address) VALUES(?,?,?,?,?,?,?,?)",
            oid, body.get("user_id"), body.get("store_id"),
            itemsJson,
            body.getOrDefault("amount", 0), body.getOrDefault("type", "外卖"),
            "已下单", body.getOrDefault("address", ""));
        db.update("INSERT IGNORE INTO logistics(order_id,status) VALUES(?,?)", oid, "待配送");
        return db.queryForMap("SELECT * FROM orders WHERE id=?", oid);
    }

    // POST /orders/{id}/refund
    @PostMapping("/orders/{id}/refund")
    public Map<String, Object> refund(@PathVariable String id) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", id);
        if (rows.isEmpty()) return Map.of("error", "订单不存在");
        db.update("UPDATE orders SET status='退款中' WHERE id=?", id);
        return Map.of("order_id", id, "status", "退款中", "msg", "退款申请已提交,1-3个工作日原路退回");
    }

    // POST /orders/{id}/accept
    @PostMapping("/orders/{id}/accept")
    public Map<String, Object> accept(@PathVariable String id, @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", id);
        if (rows.isEmpty()) return Map.of("error", "订单不存在");
        String status = (String) rows.get(0).get("status");
        if (!"已下单".equals(status)) return Map.of("error", "订单状态为" + status + "，无法接单");
        String rid = (String) body.getOrDefault("rider_id", "r001");
        db.update("UPDATE orders SET rider_id=?, status='配送中' WHERE id=?", rid, id);
        db.update("UPDATE logistics SET rider_id=?, status='配送中' WHERE order_id=?", rid, id);
        return Map.of("order_id", id, "status", "配送中", "rider_id", rid);
    }

    // POST /orders/{id}/status
    @PostMapping("/orders/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String st = (String) body.getOrDefault("status", "");
        db.update("UPDATE orders SET status=? WHERE id=?", st, id);
        db.update("UPDATE logistics SET status=? WHERE order_id=?", st, id);
        return Map.of("order_id", id, "status", st);
    }
}
