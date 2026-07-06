package com.campus.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 订单微服务 Feign 客户端 — SOA 服务契约 */
@FeignClient(name = "order-service", url = "${order.service.url:http://order-service:8001}")
public interface OrderServiceClient {

    @GetMapping("/orders/{id}")
    Map<String, Object> getOrder(@PathVariable("id") String orderId);

    @GetMapping("/users/{uid}/orders")
    List<Map<String, Object>> getUserOrders(@PathVariable("uid") String userId);

    @GetMapping("/store/{sid}/orders")
    List<Map<String, Object>> getStoreOrders(@PathVariable("sid") String storeId);

    @GetMapping("/rider/{rid}/orders")
    List<Map<String, Object>> getRiderOrders(@PathVariable("rid") String riderId);

    @GetMapping("/available")
    List<Map<String, Object>> getAvailableOrders();

    @PostMapping("/orders")
    Map<String, Object> createOrder(@RequestBody Map<String, Object> body);

    @PostMapping("/orders/{id}/refund")
    Map<String, Object> refund(@PathVariable("id") String orderId);

    @PostMapping("/orders/{id}/accept")
    Map<String, Object> accept(@PathVariable("id") String orderId, @RequestBody Map<String, Object> body);

    @PostMapping("/orders/{id}/status")
    Map<String, Object> updateStatus(@PathVariable("id") String orderId, @RequestBody Map<String, Object> body);
}
