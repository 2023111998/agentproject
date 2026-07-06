package com.campus.server.bpmn;

import com.campus.server.client.LogisticsServiceClient;
import com.campus.server.client.OrderServiceClient;
import com.campus.server.rag.RagService;

import java.util.*;
import java.util.function.Function;

/**
 * BPMN 处理器集合 — 8 个 handler，对应 BPMN 文件中的 delegateExpression。
 *
 * SOA 改进: 通过 Feign 客户端调用微服务，替代 RestTemplate 裸调用。
 */
public class BpmnHandlers {

    private final RagService rag;
    private final OrderServiceClient orderClient;
    private final LogisticsServiceClient logisticsClient;

    public BpmnHandlers(RagService rag,
                        OrderServiceClient orderClient,
                        LogisticsServiceClient logisticsClient) {
        this.rag = rag;
        this.orderClient = orderClient;
        this.logisticsClient = logisticsClient;
    }

    public Map<String, Function<Map<String, Object>, String>> buildHandlers() {
        Map<String, Function<Map<String, Object>, String>> map = new LinkedHashMap<>();
        map.put("h_query_order", this::hQueryOrder);
        map.put("h_classify_reason", this::hClassifyReason);
        map.put("h_delivery_policy", this::hDeliveryPolicy);
        map.put("h_product_policy", this::hProductPolicy);
        map.put("h_auto_refund", this::hAutoRefund);
        map.put("h_manual_review", this::hManualReview);
        map.put("h_notify", this::hNotify);
        map.put("h_query_logistics", this::hQueryLogistics);
        map.put("h_build_route_map", this::hBuildRouteMap);
        return map;
    }

    // ── 售后流程处理器 ──

    private String hQueryOrder(Map<String, Object> ctx) {
        String oid = (String) ctx.get("order_id");
        if (oid == null) return "缺少 order_id";
        // Feign 调用含断路器保护
        Map<String, Object> order = orderClient.getOrder(oid);
        if (order == null || order.containsKey("error")) {
            ctx.put("_order_error", true);
            return "订单" + oid + "不存在或服务不可用";
        }
        ctx.put("order", order);
        ctx.put("order_id", oid);
        ctx.put("amount", order.getOrDefault("amount", 0));
        ctx.put("status", order.getOrDefault("status", ""));
        ctx.put("type", order.getOrDefault("type", ""));
        ctx.put("rider_id", order.getOrDefault("rider_id", ""));
        Map<String, Object> tl = logisticsClient.track(oid);
        if (tl != null) ctx.put("timed_out", tl.getOrDefault("timed_out", false));
        return "订单" + oid + " (状态=" + ctx.get("status") + ", 金额=" + ctx.get("amount") + ")";
    }

    private String hClassifyReason(Map<String, Object> ctx) {
        String text = (String) ctx.getOrDefault("reason", "");
        boolean timedOut = Boolean.TRUE.equals(ctx.get("timed_out"));
        String[] productKw = {"质量","损坏","不满意","不好吃","变质","异味","退货","换货","坏","破","瑕疵","洒漏","洒了","漏了","破损","不新鲜","变味","凉了","少了","缺","不对","发霉","过期"};
        String[] deliveryKw = {"超时","迟到","送错","错送","漏送","少送","配送","物流","骑手","太久","延误","还没到","送晚了","没收到","丢件"};
        boolean isProduct = Arrays.stream(productKw).anyMatch(text::contains);
        boolean isDelivery = Arrays.stream(deliveryKw).anyMatch(text::contains);
        boolean isDeliveryIssue = (isDelivery && !isProduct) || (!isProduct && !isDelivery && timedOut);
        if (isProduct && !isDelivery) isDeliveryIssue = false;
        if ("洒漏霉馊".chars().anyMatch(c -> text.indexOf(c) >= 0)) isDeliveryIssue = false;
        if ("慢迟".chars().anyMatch(c -> text.indexOf(c) >= 0)) isDeliveryIssue = true;
        ctx.put("is_delivery_issue", isDeliveryIssue);
        return "原因分类: " + (isDeliveryIssue ? "配送类问题" : "商品类问题");
    }

    private String hDeliveryPolicy(Map<String, Object> ctx) {
        List<String> results = rag.retrieve("超时补偿 配送时效 外卖", 2);
        String policyText = results.isEmpty() ? "未找到相关政策" : results.get(0);
        ctx.put("policy", policyText);
        return policyText.length() > 40 ? policyText.substring(0, 40) + "..." : policyText;
    }

    private String hProductPolicy(Map<String, Object> ctx) {
        List<String> results = rag.retrieve("退款政策 商品质量 退货 换货", 2);
        String policyText = results.isEmpty() ? "未找到相关政策" : results.get(0);
        ctx.put("policy", policyText);
        return policyText.length() > 40 ? policyText.substring(0, 40) + "..." : policyText;
    }

    private String hAutoRefund(Map<String, Object> ctx) {
        String oid = (String) ctx.get("order_id");
        orderClient.refund(oid); // Feign 调用含降级
        ctx.put("refund_result", "已自动发起退款(状态=退款中, 退款申请已提交,1-3个工作日原路退回)");
        return "自动退款已发起";
    }

    private String hManualReview(Map<String, Object> ctx) {
        Object amount = ctx.getOrDefault("amount", 0);
        double amt = amount instanceof Number n ? n.doubleValue() : 0;
        ctx.put("refund_result", "金额" + amt + "元,已达人工审核标准,已转人工坐席待确认");
        return "已转人工审核";
    }

    private String hNotify(Map<String, Object> ctx) {
        String result = (String) ctx.getOrDefault("refund_result", "");
        boolean isDelivery = Boolean.TRUE.equals(ctx.get("is_delivery_issue"));
        String policy = (String) ctx.getOrDefault("policy", "");
        StringBuilder reply = new StringBuilder();
        reply.append("【售后·BPMN流程】");
        reply.append("订单").append(ctx.get("order_id")).append("售后处理结果: ").append(result);
        reply.append("。原因分类: ").append(isDelivery ? "配送类问题" : "商品类问题");
        reply.append("。相关政策: ").append(policy);
        ctx.put("reply", reply.toString());
        return "已通知用户";
    }

    // ── 物流流程处理器 ──

    private String hQueryLogistics(Map<String, Object> ctx) {
        String oid = (String) ctx.get("order_id");
        Map<String, Object> o = orderClient.getOrder(oid);
        Map<String, Object> tl = logisticsClient.track(oid);
        Map<String, Object> route = logisticsClient.route(oid);
        ctx.put("order", o);
        ctx.put("track", tl);
        ctx.put("route", route);
        boolean hasMap = route != null && Boolean.TRUE.equals(route.get("has_map_data"));
        ctx.put("has_map_data", hasMap);
        return "物流查询完成 (状态=" + (o != null ? o.get("status") : "?") + ")";
    }

    private String hBuildRouteMap(Map<String, Object> ctx) {
        @SuppressWarnings("unchecked")
        Map<String, Object> route = (Map<String, Object>) ctx.get("route");
        if (route == null) return "无路线数据";
        Object riderObj = route.get("rider");
        String riderName = "未知";
        if (riderObj instanceof Map<?, ?> rm) {
            Object nameObj = rm.get("name");
            if (nameObj instanceof String s) riderName = s;
        }
        Object progress = route.get("progress");
        double prog = progress instanceof Number n ? n.doubleValue() * 100 : 0;
        // 从 order 和 route 获取状态和地址
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) ctx.get("order");
        String status = order != null ? (String) order.getOrDefault("status", "") : "";
        String address = "";
        Object destObj = route.get("destination");
        if (destObj instanceof Map<?, ?> dm) {
            Object label = dm.get("label");
            if (label instanceof String s) address = s;
        }
        ctx.put("map_data", route);
        String reply = "【物流·BPMN地图流程】订单" + ctx.get("order_id") + " " + status
            + (address.isEmpty() ? "" : " → " + address)
            + ": 骑手" + riderName + "正在配送中, 已完成" + (int) prog + "%";
        ctx.put("reply", reply);
        return "路线地图构建完成 (骑手=" + riderName + ", 进度=" + (int) prog + "%)";
    }
}
