package com.campus.server.sla;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** SLA 请求记录器 — 单例，供所有 Controller 注入 */
@Service
public class SlaRecorder {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final ConcurrentHashMap<String, List<Long>> responseTimes = new ConcurrentHashMap<>();
    private final Instant startTime = Instant.now();

    public void record(String endpoint, long latencyMs, boolean success) {
        totalRequests.incrementAndGet();
        if (success) successfulRequests.incrementAndGet();
        else failedRequests.incrementAndGet();
        responseTimes.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>())).add(latencyMs);
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessful() { return successfulRequests.get(); }
    public long getFailed() { return failedRequests.get(); }
    public long getUptimeSeconds() { return Duration.between(startTime, Instant.now()).getSeconds(); }
    public List<Long> getAllLatencies() {
        return responseTimes.values().stream().flatMap(List::stream).toList();
    }
}
