package com.campus.server.controller;

import com.campus.server.agent.AgentOrchestrator;
import com.campus.server.client.LogisticsServiceClient;
import com.campus.server.client.OrderServiceClient;
import com.campus.server.client.ProductServiceClient;
import com.campus.server.guardrails.Guardrails;
import com.campus.server.rag.RagService;
import com.campus.server.service.*;
import com.campus.server.session.SessionStore;
import com.campus.server.sla.SlaRecorder;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 主控制器：页面路由 + 15+ REST API + 智能助理。
 * 职责：HTTP 请求/响应处理，业务逻辑委托给 Service 层。
 */
@RestController
public class ServerController {

    private final AgentOrchestrator agent;
    private final Guardrails guard;
    private final OrderService orderService;
    private final ProductService productService;
    private final SessionStore sessionStore;
    private final SlaRecorder slaRecorder;

    public ServerController(JdbcTemplate db, RagService rag,
                            OrderService orderService, ProductService productService,
                            OrderServiceClient orderClient, ProductServiceClient productClient,
                            LogisticsServiceClient logisticsClient,
                            SessionStore sessionStore, SlaRecorder slaRecorder) {
        this.agent = new AgentOrchestrator(db, rag, orderClient, productClient, logisticsClient);
        this.guard = new Guardrails(db);
        this.orderService = orderService;
        this.productService = productService;
        this.sessionStore = sessionStore;
        this.slaRecorder = slaRecorder;
    }

    // ====== 页面路由 ======
    @GetMapping("/")
    public ResponseEntity<String> index() { return servePage("customer.html"); }
    @GetMapping("/merchant")
    public ResponseEntity<String> merchant() { return servePage("merchant.html"); }
    @GetMapping("/rider")
    public ResponseEntity<String> rider() { return servePage("rider.html"); }
    @GetMapping("/chat")
    public ResponseEntity<String> chat() { return servePage("index.html"); }

    private ResponseEntity<String> servePage(String filename) {
        try {
            ClassPathResource res = new ClassPathResource("static/" + filename);
            String html = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok().header("Content-Type", "text/html; charset=utf-8").body(html);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ====== 商品 API ======
    @GetMapping("/api/products")
    public List<Map<String, Object>> listProducts() {
        return productService.listProducts();
    }

    @PostMapping("/api/products")
    public Map<String, Object> addProduct(@RequestBody Map<String, Object> body) {
        return productService.addProduct(body);
    }

    @PutMapping("/api/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return productService.updateProduct(id, body);
    }

    @DeleteMapping("/api/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable int id) {
        return productService.deleteProduct(id);
    }

    // ====== 订单 API ======
    @GetMapping("/api/orders")
    public List<Map<String, Object>> listOrders(@RequestParam(defaultValue = "") String uid) {
        if (uid.isEmpty()) return List.of();
        return orderService.getUserOrders(uid);
    }

    @PostMapping("/api/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        return orderService.createOrder(body);
    }

    // ====== 商家 API ======
    @GetMapping("/api/store/orders")
    public List<Map<String, Object>> storeOrders(@RequestParam(defaultValue = "") String sid) {
        if (sid.isEmpty()) return List.of();
        return orderService.getStoreOrders(sid);
    }

    @GetMapping("/api/store/stats")
    public Map<String, Object> storeStats(@RequestParam(defaultValue = "") String sid) {
        if (sid.isEmpty()) return Map.of();
        return orderService.storeStats(sid);
    }

    @PostMapping("/api/store/orders/{id}/manual-refund")
    public Map<String, Object> manualRefund(@PathVariable String id,
                                            @RequestBody(required = false) Map<String, Object> body) {
        String reqSid = body != null ? (String) body.getOrDefault("store_id", "") : "";
        return orderService.manualRefund(id, reqSid);
    }

    // ====== 骑手 API ======
    @GetMapping("/api/rider/available")
    public List<Map<String, Object>> riderAvailable() {
        return orderService.getAvailableOrders();
    }

    @PostMapping("/api/rider/accept")
    public Map<String, Object> riderAccept(@RequestBody Map<String, Object> body) {
        String oid = (String) body.get("order_id");
        String rid = (String) body.getOrDefault("rider_id", "r001");
        return orderService.riderAccept(oid, rid);
    }

    @PostMapping("/api/rider/status")
    public Map<String, Object> riderStatus(@RequestBody Map<String, Object> body) {
        String oid = (String) body.get("order_id");
        String st = (String) body.getOrDefault("status", "");
        return orderService.updateStatus(oid, st);
    }

    @GetMapping("/api/rider/history")
    public List<Map<String, Object>> riderHistory(@RequestParam(defaultValue = "") String rid) {
        if (rid.isEmpty()) return List.of();
        return orderService.getRiderOrders(rid);
    }

    // ====== 智能助理 API (核心) ======
    @PostMapping("/api/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        String uid = (String) body.getOrDefault("user_id", "u001");
        String msg = (String) body.getOrDefault("message", "");
        if (msg == null || msg.isBlank()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("reply", "请说点什么吧");
            empty.put("intent", "EMPTY");
            return empty;
        }

        long t0 = System.currentTimeMillis();

        // 1. 输入护栏
        if (!guard.inputGuard(msg)) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("reply", "检测到可疑指令，已拦截");
            blocked.put("intent", "BLOCKED");
            blocked.put("trace", "[输入护栏] 命中提示注入");
            blocked.put("latency", 0.0);
            return blocked;
        }

        // 2. 会话记忆 (SOA: SessionStore 支持 Redis 共享)
        sessionStore.add(uid, "user", msg);

        // 3. Agent 编排
        Map<String, Object> result = agent.orchestrate(uid, msg);

        // 4. 输出脱敏
        String reply = guard.piiMask((String) result.getOrDefault("answer", ""));
        sessionStore.add(uid, "assistant", reply);

        double latency = (System.currentTimeMillis() - t0) / 1000.0;
        slaRecorder.record("/api/chat", (long) (latency * 1000), true);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("reply", reply);
        resp.put("intent", result.getOrDefault("intent", ""));
        resp.put("trace", result.getOrDefault("trace", ""));
        resp.put("latency", Math.round(latency * 1000.0) / 1000.0);
        resp.put("map_data", result.get("map_data"));  // 允许 null
        return resp;
    }

    // ====== 健康检查 ======
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "timestamp", System.currentTimeMillis());
    }
}
