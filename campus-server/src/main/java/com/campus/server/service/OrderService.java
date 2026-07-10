package com.campus.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 订单服务：封装订单相关业务逻辑 */
@Service
public class OrderService {

    private final JdbcTemplate db;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(JdbcTemplate db) { this.db = db; }

    /** 查询用户订单列表 */
    public List<Map<String, Object>> getUserOrders(String userId) {
        return db.queryForList("SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC", userId);
    }

    /** 查询商家订单列表 */
    public List<Map<String, Object>> getStoreOrders(String storeId) {
        return db.queryForList("SELECT * FROM orders WHERE store_id=? ORDER BY created_at DESC", storeId);
    }

    /** 查询骑手订单列表 */
    public List<Map<String, Object>> getRiderOrders(String riderId) {
        return db.queryForList("SELECT * FROM orders WHERE rider_id=? ORDER BY created_at DESC", riderId);
    }

    /** 查询可接订单（状态=已下单） */
    public List<Map<String, Object>> getAvailableOrders() {
        return db.queryForList("SELECT * FROM orders WHERE status='已下单' ORDER BY created_at DESC");
    }

    /** 创建订单 */
    public Map<String, Object> createOrder(Map<String, Object> body) {
        String oid = (String) body.getOrDefault("order_id", "ORD" + System.currentTimeMillis());
        String itemsJson;
        try {
            Object itemsObj = body.get("items");
            if (itemsObj instanceof String s) {
                itemsJson = s;
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

    /** 查询单个订单 */
    public Map<String, Object> getOrder(String orderId) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", orderId);
        return rows.isEmpty() ? Map.of("error", "订单不存在") : rows.get(0);
    }

    /** 骑手接单 */
    public Map<String, Object> riderAccept(String orderId, String riderId) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", orderId);
        if (rows.isEmpty()) return Map.of("error", "订单不存在");
        if (!"已下单".equals(rows.get(0).get("status"))) return Map.of("error", "无法接单");
        db.update("UPDATE orders SET rider_id=?, status='配送中' WHERE id=?", riderId, orderId);
        db.update("UPDATE logistics SET rider_id=?, status='配送中' WHERE order_id=?", riderId, orderId);
        return Map.of("order_id", orderId, "status", "配送中", "rider_id", riderId);
    }

    /** 更新配送状态 */
    public Map<String, Object> updateStatus(String orderId, String status) {
        db.update("UPDATE orders SET status=? WHERE id=?", status, orderId);
        db.update("UPDATE logistics SET status=? WHERE order_id=?", status, orderId);
        return Map.of("order_id", orderId, "status", status);
    }

    /** 商家统计 */
    public Map<String, Object> storeStats(String storeId) {
        Map<String, Object> total = db.queryForMap("SELECT COUNT(*) as n FROM orders WHERE store_id=?", storeId);
        Map<String, Object> today = db.queryForMap(
            "SELECT COUNT(*) as n, COALESCE(SUM(amount),0) as revenue FROM orders WHERE store_id=? AND DATE(created_at)=CURDATE()", storeId);
        return Map.of("total_orders", total.get("n"), "today_orders", today.get("n"), "today_revenue", today.get("revenue"));
    }

    /** 人工退款 */
    public Map<String, Object> manualRefund(String orderId, String storeId) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", orderId);
        if (rows.isEmpty()) return Map.of("error", "订单不存在");
        Map<String, Object> o = rows.get(0);
        if (!storeId.isEmpty() && !storeId.equals(o.get("store_id")))
            return Map.of("error", "无权操作该订单");
        String status = (String) o.get("status");
        if (List.of("退款中", "已退款", "已送达").contains(status))
            return Map.of("error", "订单状态为" + status + "，无法退款");
        db.update("UPDATE orders SET status='退款中' WHERE id=?", orderId);
        return Map.of("order_id", orderId, "status", "退款中", "msg", "人工审核通过，已发起退款");
    }
}
