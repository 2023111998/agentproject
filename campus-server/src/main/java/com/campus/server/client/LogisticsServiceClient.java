package com.campus.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 物流微服务 Feign 客户端 — SOA 服务契约 */
@FeignClient(name = "logistics-service", url = "${logistics.service.url:http://logistics-service:8003}")
public interface LogisticsServiceClient {

    @GetMapping("/track/{orderId}")
    Map<String, Object> track(@PathVariable("orderId") String orderId);

    @GetMapping("/route/{orderId}")
    Map<String, Object> route(@PathVariable("orderId") String orderId);
}
