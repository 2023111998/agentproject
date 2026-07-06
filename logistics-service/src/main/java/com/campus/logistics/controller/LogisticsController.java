package com.campus.logistics.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class LogisticsController {

    private final JdbcTemplate db;
    private static final Map<String, Map<String, Object>> LOCATIONS = Map.of(
        "canteen",    Map.of("lat", 34.1505, "lng", 108.8505, "label", "学生食堂"),
        "dorm3",      Map.of("lat", 34.1490, "lng", 108.8520, "label", "三号宿舍楼"),
        "gate",       Map.of("lat", 34.1510, "lng", 108.8480, "label", "校门"),
        "lib",        Map.of("lat", 34.1520, "lng", 108.8490, "label", "图书馆"),
        "digi_shop",  Map.of("lat", 34.1530, "lng", 108.8550, "label", "商业街数码店")
    );
    private static final List<Map<String, Object>> ROUTE_TEMPLATE = List.of(
        Map.of("lat", 34.1505, "lng", 108.8505), Map.of("lat", 34.1505, "lng", 108.8510),
        Map.of("lat", 34.1500, "lng", 108.8515), Map.of("lat", 34.1495, "lng", 108.8520),
        Map.of("lat", 34.1490, "lng", 108.8520)
    );

    public LogisticsController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/track/{orderId}")
    public Map<String, Object> track(@PathVariable String orderId) {
        List<Map<String, Object>> logs = db.queryForList("SELECT * FROM logistics WHERE order_id=?", orderId);
        Map<String, Object> logi = logs.isEmpty() ? Map.of() : logs.get(0);
        List<Map<String, Object>> orders = db.queryForList("SELECT type, status, placed_min_ago FROM orders WHERE id=?", orderId);
        Map<String, Object> order = orders.isEmpty() ? Map.of() : orders.get(0);

        boolean timedOut = "外卖".equals(order.get("type"))
            && (order.get("placed_min_ago") instanceof Number n && n.intValue() > 30)
            && "配送中".equals(order.get("status"));

        Map<String, Object> result = new HashMap<>(logi);
        result.put("timed_out", timedOut);
        result.putIfAbsent("status", order.getOrDefault("status", ""));
        return result;
    }

    @GetMapping("/route/{orderId}")
    public Map<String, Object> route(@PathVariable String orderId) {
        List<Map<String, Object>> orders = db.queryForList("SELECT * FROM orders WHERE id=?", orderId);
        if (orders.isEmpty()) return Map.of("has_map_data", false, "msg", "订单不存在");

        Map<String, Object> o = orders.get(0);
        String storeId = (String) o.getOrDefault("store_id", "m001");
        String address = (String) o.getOrDefault("address", "");
        String storeKey = "m001".equals(storeId) ? "canteen" : "digi_shop";
        String destKey = address.contains("三号") ? "dorm3" : address.contains("图书馆") ? "lib" : "gate";

        // 模拟骑手位置: 根据 placed_min_ago 计算进度
        int minAgo = o.get("placed_min_ago") instanceof Number n ? n.intValue() : 0;
        int riderIdx = Math.min(minAgo / 10, 4);
        double progress = riderIdx / 4.0;
        Map<String, Object> riderPos = ROUTE_TEMPLATE.get(riderIdx);
        double riderLat = riderPos.get("lat") instanceof Number n ? n.doubleValue() : 34.15;
        double riderLng = riderPos.get("lng") instanceof Number n ? n.doubleValue() : 108.85;

        return Map.of(
            "has_map_data", true,
            "store", LOCATIONS.getOrDefault(storeKey, Map.of()),
            "destination", LOCATIONS.getOrDefault(destKey, Map.of()),
            "waypoints", ROUTE_TEMPLATE,
            "rider", Map.of("name", "张师傅", "lat", riderLat, "lng", riderLng),
            "rider_idx", riderIdx,
            "progress", progress,
            "status", o.getOrDefault("status", "")
        );
    }
}
