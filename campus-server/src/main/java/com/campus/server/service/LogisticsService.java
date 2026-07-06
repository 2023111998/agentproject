package com.campus.server.service;

import com.campus.server.client.LogisticsServiceClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 物流服务：封装对 logistics-service 的远程调用。
 *
 * SOA 改进: 通过 Feign 客户端调用，含断路器保护 + 降级。
 */
@Service
public class LogisticsService {

    private final LogisticsServiceClient client;

    public LogisticsService(LogisticsServiceClient client) { this.client = client; }

    public Map<String, Object> track(String orderId) {
        return client.track(orderId);
    }

    public Map<String, Object> route(String orderId) {
        return client.route(orderId);
    }
}
