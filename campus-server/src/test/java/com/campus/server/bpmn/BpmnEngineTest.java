package com.campus.server.bpmn;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** BPMN 引擎单元测试 */
class BpmnEngineTest {

    @Test
    void testLoadLogisticsMap() throws Exception {
        BpmnEngine.BpmnModel model = BpmnEngine.load("flows/logistics_map.bpmn");
        assertNotNull(model);
        assertNotNull(model.startId());
        assertFalse(model.nodes().isEmpty());
        assertFalse(model.flows().isEmpty());

        // 验证关键节点存在
        assertTrue(model.nodes().values().stream()
            .anyMatch(n -> "查询物流与路线数据".equals(n.name())));
        assertTrue(model.nodes().values().stream()
            .anyMatch(n -> "构建配送路线地图".equals(n.name())));
    }

    @Test
    void testLoadAftersaleRefund() throws Exception {
        BpmnEngine.BpmnModel model = BpmnEngine.load("flows/aftersale_refund.bpmn");
        assertNotNull(model);
        assertNotNull(model.startId());
        assertFalse(model.nodes().isEmpty());
        assertFalse(model.flows().isEmpty());

        // 验证关键节点存在
        assertTrue(model.nodes().values().stream()
            .anyMatch(n -> "查询订单".equals(n.name())));
        assertTrue(model.nodes().values().stream()
            .anyMatch(n -> "分类售后原因".equals(n.name())));
    }

    @Test
    void testSafeEval() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("is_delivery_issue", true);
        ctx.put("amount", 100.0);

        assertTrue(BpmnEngine.safeEval("is_delivery_issue == True", ctx));
        assertFalse(BpmnEngine.safeEval("is_delivery_issue == False", ctx));
        assertTrue(BpmnEngine.safeEval("amount >= 100", ctx));
        assertTrue(BpmnEngine.safeEval("amount >= 50", ctx));
        assertFalse(BpmnEngine.safeEval("amount < 50", ctx));
    }

    @Test
    void testRunSimpleFlow() throws Exception {
        // 构建一个简单的模拟处理器映射
        Map<String, Function<Map<String, Object>, String>> handlers = new LinkedHashMap<>();
        handlers.put("h_query_order", ctx -> {
            ctx.put("amount", 50);
            ctx.put("status", "已下单");
            return "查询完成";
        });
        handlers.put("h_classify_reason", ctx -> {
            ctx.put("is_delivery_issue", true);
            return "配送类";
        });
        handlers.put("h_delivery_policy", ctx -> {
            ctx.put("policy", "超时可补偿10%-30%");
            return "查政策完成";
        });
        handlers.put("h_product_policy", ctx -> "商品政策");
        handlers.put("h_auto_refund", ctx -> {
            ctx.put("refund_result", "已退款");
            return "退款完成";
        });
        handlers.put("h_manual_review", ctx -> "人工审核");
        handlers.put("h_notify", ctx -> {
            ctx.put("reply", "售后处理完成");
            return "已通知";
        });

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("order_id", "20260601001");
        ctx.put("reason", "超时了没送到");

        StringBuilder trace = new StringBuilder();
        Map<String, Object> result = BpmnEngine.run(
            "flows/aftersale_refund.bpmn", handlers, ctx, trace);

        assertNotNull(result);
        assertFalse(trace.toString().isEmpty());
        // 因为金额<100，应该走自动退款分支
        assertTrue(trace.toString().contains("配送"));
    }
}
