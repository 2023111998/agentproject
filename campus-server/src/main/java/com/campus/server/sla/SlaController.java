package com.campus.server.sla;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/** SLA 评价 REST API — 调用 SlaRecorder 获取统计数据 */
@RestController
@RequestMapping("/api/sla")
public class SlaController {

    private final RestTemplate http = new RestTemplate();
    private final SlaRecorder recorder;

    public SlaController(SlaRecorder recorder) { this.recorder = recorder; }

    @GetMapping("/report")
    public Map<String, Object> slaReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("service", "校园电商/外卖智能服务平台 - Java 重构版");
        report.put("report_time", java.time.Instant.now().toString());
        report.put("uptime_seconds", recorder.getUptimeSeconds());

        // 微服务健康检查
        Map<String, Object> services = new LinkedHashMap<>();
        services.put("order-service", check("http://order-service:8001/available"));
        services.put("product-service", check("http://product-service:8002/products"));
        services.put("logistics-service", check("http://logistics-service:8003/track/20260601001"));
        services.put("campus-server", check("http://localhost:8000/api/health"));
        report.put("services", services);

        // 可用性
        long total = recorder.getTotalRequests();
        long success = recorder.getSuccessful();
        long failed = recorder.getFailed();
        double avail = total > 0 ? (double) success / total * 100 : 100.0;
        report.put("availability_pct", Math.round(avail * 100.0) / 100.0);
        report.put("total_requests", total);
        report.put("successful", success);
        report.put("failed", failed);

        // 响应时间
        List<Long> times = recorder.getAllLatencies();
        Map<String, Object> lat = new LinkedHashMap<>();
        if (!times.isEmpty()) {
            long sum = 0, min = Long.MAX_VALUE, max = 0;
            for (long t : times) { sum += t; if (t < min) min = t; if (t > max) max = t; }
            lat.put("avg_ms", times.size() > 0 ? sum / times.size() : 0);
            lat.put("min_ms", min);
            lat.put("max_ms", max);
            lat.put("p50_ms", percentile(times, 50));
            lat.put("p95_ms", percentile(times, 95));
            lat.put("p99_ms", percentile(times, 99));
            lat.put("sample_count", times.size());
        }
        report.put("latency", lat);

        // 吞吐量
        long uptime = Math.max(1, recorder.getUptimeSeconds());
        report.put("throughput_rps", Math.round((double) total / uptime * 100.0) / 100.0);

        // SLA 评级
        report.put("sla_compliance", evaluate(avail, lat));
        return report;
    }

    private Map<String, Object> check(String url) {
        Map<String, Object> r = new LinkedHashMap<>();
        long t0 = System.currentTimeMillis();
        try {
            String body = http.getForObject(url, String.class);
            r.put("status", body != null ? "UP" : "DEGRADED");
        } catch (Exception e) {
            r.put("status", "DOWN");
        }
        r.put("latency_ms", System.currentTimeMillis() - t0);
        return r;
    }

    private long percentile(List<Long> times, int p) {
        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        int idx = Math.max(0, (int) Math.ceil(p / 100.0 * sorted.size()) - 1);
        return sorted.get(Math.min(idx, sorted.size() - 1));
    }

    private Map<String, Object> evaluate(double avail, Map<String, Object> lat) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("availability_grade", avail >= 99.99 ? "A+ (4个9)" : avail >= 99.9 ? "A (3个9)"
            : avail >= 99.0 ? "B" : avail >= 95.0 ? "C" : "D");
        double avg = lat.isEmpty() ? 0 : ((Number) lat.getOrDefault("avg_ms", 0L)).doubleValue();
        r.put("latency_grade", avg == 0 ? "N/A" : avg < 100 ? "A+" : avg < 300 ? "A"
            : avg < 1000 ? "B" : avg < 3000 ? "C" : "D");
        String ag = (String) r.get("availability_grade");
        String lg = (String) r.get("latency_grade");
        r.put("overall_grade", ag.startsWith("A") && lg.startsWith("A") ? "优秀"
            : ag.startsWith("A") || lg.startsWith("A") ? "良好"
            : ag.startsWith("B") || lg.startsWith("B") ? "及格" : "需改进");
        r.put("avg_latency_ms", avg);
        return r;
    }
}
