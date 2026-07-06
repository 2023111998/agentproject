package com.campus.server.agent;

import com.campus.server.bpmn.BpmnEngine;
import com.campus.server.bpmn.BpmnHandlers;
import com.campus.server.client.LogisticsServiceClient;
import com.campus.server.client.OrderServiceClient;
import com.campus.server.client.ProductServiceClient;
import com.campus.server.rag.RagService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/**
 * Agent 编排引擎: 路由 → 分派专家 → BPMN/ReAct/smart_resolve/smart_order。
 *
 * SOA 改进:
 * - 通过 Feign 客户端调用微服务（替代 RestTemplate 裸调用）
 * - 服务 URL 由 application.properties 管理（配置外部化）
 * - 断路器自动生效（Feign + Resilience4j）
 */
public class AgentOrchestrator {

    private final JdbcTemplate db;
    private final RagService rag;
    private final BpmnHandlers bpmnHandlers;
    // SOA: Feign 客户端（含断路器降级）
    private final OrderServiceClient orderClient;
    private final ProductServiceClient productClient;
    private final LogisticsServiceClient logisticsClient;

    public AgentOrchestrator(JdbcTemplate db, RagService rag,
                             OrderServiceClient orderClient,
                             ProductServiceClient productClient,
                             LogisticsServiceClient logisticsClient) {
        this.db = db;
        this.rag = rag;
        this.bpmnHandlers = new BpmnHandlers(rag, orderClient, logisticsClient);
        this.orderClient = orderClient;
        this.productClient = productClient;
        this.logisticsClient = logisticsClient;
    }

    // ====== 意图路由 ======
    public String router(String text) {
        if (Pattern.compile("退|赔|补偿|售后|发票|换货").matcher(text).find()) return "售后";
        if (Pattern.compile("有问题|不满意|洒|漏|坏|破|缺|不对|处理|洒漏|坏了|不好").matcher(text).find()) return "售后";
        if (Pattern.compile("到哪|物流|配送|送到|快递|多久|什么时候到|路线|地图|在哪|位置|到哪了|送哪").matcher(text).find()) return "物流";
        if (Pattern.compile("多少钱|价格$|什么价|想买|买什么|推荐|有没有").matcher(text).find()) return "导购";
        return "其他";
    }

    private String extractOid(String text) {
        Matcher m = Pattern.compile("\\d{8,}").matcher(text);
        if (m.find()) return m.group();
        m = Pattern.compile("(?:#\\s*)?([A-Za-z]{2,}\\d{3,})").matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    // ====== 主编排入口 ======
    public Map<String, Object> orchestrate(String userId, String text) {
        StringBuilder trace = new StringBuilder();

        String intent = router(text);
        trace.append("[路由] 判定意图 = ").append(intent).append("\n");

        if (isOrdering(text)) {
            trace.append("[路由] 检测到下单意图 → smart_order\n");
            return smartOrder(userId, text, trace);
        }

        if (("售后".equals(intent) || "其他".equals(intent)) && isOpenEnded(text)) {
            trace.append("[路由] 开放式售后 → smart_resolve\n");
            return smartResolve(userId, text, trace);
        }

        if ("物流".equals(intent)) {
            String oid = extractOid(text);
            if (oid != null) return expertLogisticsBpmn(oid, trace);
            return response("物流", "请提供订单号查询物流", trace);
        }

        if ("售后".equals(intent)) {
            String oid = extractOid(text);
            if (oid != null) return expertAftersaleBpmn(oid, text, trace);
            return reactAgent(text, "售后", trace);
        }

        if ("导购".equals(intent)) return reactAgent(text, "导购", trace);

        return response(intent, "您好,我可以帮您查订单、查物流、查商品或处理售后,请问需要什么?", trace);
    }

    // ====== BPMN 驱动的售后流程 ======
    private Map<String, Object> expertAftersaleBpmn(String oid, String text, StringBuilder trace) {
        trace.append("[BPMN] 启动 aftersale_refund 流程, 订单=").append(oid).append("\n");
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("order_id", oid);
            ctx.put("reason", text);
            ctx.put("user_id", "");
            Map<String, Function<Map<String, Object>, String>> handlers = bpmnHandlers.buildHandlers();
            BpmnEngine.run("flows/aftersale_refund.bpmn", handlers, ctx, trace);
            String reply = (String) ctx.getOrDefault("reply", "售后处理完成");
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) ctx.get("map_data");
            return responseWithMap("售后", reply, trace, mapData);
        } catch (Exception e) {
            trace.append("[BPMN] 异常: ").append(e.getMessage()).append(", 回退硬编码流程\n");
            return expertAftersaleFallback(oid, text, trace);
        }
    }

    // ====== BPMN 驱动的物流流程 ======
    private Map<String, Object> expertLogisticsBpmn(String oid, StringBuilder trace) {
        trace.append("[BPMN] 启动 logistics_map 流程, 订单=").append(oid).append("\n");
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("order_id", oid);
            Map<String, Function<Map<String, Object>, String>> handlers = bpmnHandlers.buildHandlers();
            BpmnEngine.run("flows/logistics_map.bpmn", handlers, ctx, trace);
            String reply = (String) ctx.getOrDefault("reply", "物流查询完成");
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) ctx.get("map_data");
            return responseWithMap("物流", reply, trace, mapData);
        } catch (Exception e) {
            trace.append("[BPMN] 异常: ").append(e.getMessage()).append(", 回退硬编码流程\n");
            return expertLogisticsFallback(oid, trace);
        }
    }

    // ====== 兜底: 物流 ======
    private Map<String, Object> expertLogisticsFallback(String oid, StringBuilder trace) {
        orderClient.getOrder(oid);  // 验证订单存在
        Map<String, Object> route = logisticsClient.route(oid);
        trace.append("[Fallback] 查询物流: 订单").append(oid).append("\n");
        boolean hasMap = route != null && Boolean.TRUE.equals(route.get("has_map_data"));
        if (hasMap) {
            String rider = route.get("rider") instanceof Map ? (String) ((Map<?,?>) route.get("rider")).get("name") : "未知";
            Object progress = route.get("progress");
            double prog = progress instanceof Number ? ((Number) progress).doubleValue() * 100 : 0;
            return Map.of("intent", "物流", "answer", "【物流】订单" + oid + ": 骑手" + rider + "正在配送中, 已完成" + (int)prog + "%", "trace", trace.toString(), "map_data", route);
        }
        return response("物流", "订单" + oid + "暂无地图数据", trace);
    }

    // ====== 兜底: 售后 ======
    private Map<String, Object> expertAftersaleFallback(String oid, String text, StringBuilder trace) {
        Map<String, Object> o = orderClient.getOrder(oid);
        if (o == null || o.containsKey("error")) {
            trace.append("[Fallback] 订单不存在,回退ReAct\n");
            return reactAgent(text, "售后", trace);
        }
        Map<String, Object> tl = logisticsClient.track(oid);
        double amount = o.get("amount") instanceof Number ? ((Number) o.get("amount")).doubleValue() : 0;
        boolean timedOut = tl != null && Boolean.TRUE.equals(tl.get("timed_out"));
        trace.append("[Fallback] 查订单: 状态=").append(o.get("status")).append(", 金额=").append(amount).append("\n");

        boolean isDelivery = classifyReason(text, timedOut);
        // RAG 政策检索
        List<String> policyResults = rag.retrieve(isDelivery ? "超时补偿 配送" : "退款政策 退货", 2);
        String policyText = policyResults.isEmpty() ? "未找到相关政策" : policyResults.get(0);
        boolean needManual = amount >= 100;
        String result = needManual ? "金额" + amount + "元,已达人工审核标准,已转人工坐席待确认"
            : "已自动发起退款(退款申请已提交,1-3个工作日原路退回)";
        if (!needManual) orderClient.refund(oid);

        Map<String, Object> response = response("售后", "【售后】订单" + oid + "售后处理结果: " + result
            + "。原因分类: " + (isDelivery ? "配送类" : "商品类") + "。政策: " + policyText, trace);
        return response;
    }

    // ====== 原因分类 ======
    private boolean classifyReason(String text, boolean timedOut) {
        String[] productKw = {"质量", "损坏", "不满意", "不好吃", "变质", "异味", "退货", "换货"};
        String[] deliveryKw = {"超时", "迟到", "送错", "错送", "漏送", "少送", "没送到", "配送", "物流", "骑手", "太久", "延误"};
        boolean isProduct = Arrays.stream(productKw).anyMatch(text::contains);
        boolean isDelivery = Arrays.stream(deliveryKw).anyMatch(text::contains);
        if (isDelivery && !isProduct) return true;
        if (isProduct && !isDelivery) return false;
        return timedOut;
    }

    // ====== ReAct ======
    private Map<String, Object> reactAgent(String text, String intent, StringBuilder trace) {
        trace.append("[ReAct] ").append(intent).append(" 专家处理\n");
        if ("导购".equals(intent)) {
            for (String kw : List.of("蓝牙耳机", "机械键盘", "黄焖鸡米饭", "麻辣烫", "珍珠奶茶", "炸鸡排", "可乐")) {
                if (text.contains(kw)) {
                    Map<String, Object> p = productClient.getProduct(kw);
                    if (p != null && !p.containsKey("error")) {
                        String answer = "【导购】" + p.get("name") + "售价" + p.get("price") + "元,库存" + p.get("stock") + "件,评分" + p.get("rating") + "。";
                        return response(intent, answer, trace);
                    }
                }
            }
        }
        // RAG 政策检索
        if (Pattern.compile("退|赔|补偿|超时|退货|换货|退款").matcher(text).find()) {
            String ragQuery = detectPolicyQuery(text);
            trace.append("[ReAct] RAG查询: \"").append(ragQuery).append("\"\n");
            List<String> results = rag != null ? rag.retrieve(ragQuery, 2) : List.of();
            trace.append("[ReAct] RAG结果数: ").append(results.size()).append("\n");
            if (!results.isEmpty()) {
                Map<String, Object> ragResp = new LinkedHashMap<>();
                ragResp.put("intent", intent);
                ragResp.put("answer", "【" + intent + "】相关政策:" + results.get(0));
                ragResp.put("trace", trace.toString());
                return ragResp;
            }
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("intent", intent);
        fallback.put("answer", "您好,我可以帮您查订单、查物流、查商品或处理售后,请问需要什么?");
        fallback.put("trace", trace.toString());
        return fallback;
    }

    // ====== 开放式售后 (smart_resolve) ======
    private Map<String, Object> smartResolve(String userId, String text, StringBuilder trace) {
        trace.append("[smart_resolve] 开放式售后 → 检索用户订单\n");
        List<Map<String, Object>> orders = db.queryForList("SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC", userId);
        trace.append("[smart_resolve] 查到 ").append(orders.size()).append(" 个订单\n");
        List<String> plan = new ArrayList<>(), clarify = new ArrayList<>();
        boolean wantsRefund = text.contains("退");
        for (Map<String, Object> o : orders) {
            String oid = (String) o.get("id");
            double amt = o.get("amount") instanceof Number ? ((Number) o.get("amount")).doubleValue() : 0;
            if ("外卖".equals(o.get("type"))) {
                Map<String, Object> tl = logisticsClient.track(oid);
                if (tl != null && Boolean.TRUE.equals(tl.get("timed_out"))) plan.add("订单" + oid + "(外卖·超时): 已自动补偿");
            } else if (wantsRefund || Pattern.compile("不满意|问题|不好").matcher(text).find()) {
                if (amt >= 100) { clarify.add("商品订单" + oid + "(¥" + amt + "):支持7天无理由退货"); plan.add("订单" + oid + ": 待确认"); }
                else plan.add("订单" + oid + ": 可直接退货退款");
            }
        }
        if (plan.isEmpty() && clarify.isEmpty()) {
            List<String> policies = rag.retrieve(text, 2);
            String answer = policies.isEmpty() ? "未发现需要处理的异常订单。如有具体问题请告诉我订单号。"
                : "未发现需要处理的异常订单。相关政策参考: " + policies.get(0);
            return response("售后(开放式)", answer, trace);
        }
        StringBuilder reply = new StringBuilder("我看了你名下的订单,帮你这样处理:\n");
        for (String p : plan) reply.append("· ").append(p).append("\n");
        if (!clarify.isEmpty()) { reply.append("还需你确认一下:\n"); for (String c : clarify) reply.append("? ").append(c).append("\n"); }
        return response("售后(开放式)", reply.toString(), trace);
    }

    // ====== 自然语言下单 ======
    private Map<String, Object> smartOrder(String userId, String text, StringBuilder trace) {
        trace.append("[smart_order] 自然语言下单\n");
        List<Map<String, Object>> allProducts = db.queryForList("SELECT * FROM products WHERE stock>0");
        Map<String, Object> matched = null;
        for (Map<String, Object> p : allProducts) {
            String name = (String) p.get("name");
            if (name != null && text.contains(name) && (matched == null || name.length() > ((String) matched.get("name")).length()))
                matched = p;
        }
        if (matched == null) {
            for (String k : new String[]{"黄焖鸡", "麻辣烫", "奶茶", "鸡排", "可乐", "炸鸡", "蓝牙耳机", "机械键盘", "键盘", "耳机"}) {
                if (text.contains(k)) { for (Map<String, Object> p : allProducts) { if (((String) p.get("name")).contains(k)) { matched = p; break; } } }
                if (matched != null) break;
            }
        }
        if (matched == null) return Map.of("intent", "下单", "answer", "抱歉,没找到你想买的商品。试试说「帮我点一份黄焖鸡米饭送到三号宿舍楼」。");
        int qty = 1;
        Matcher qm = Pattern.compile("(\\d+|[一两二三四五])\\s*(份|杯|个|件|单)").matcher(text);
        if (qm.find()) { String ns = qm.group(1); qty = Map.of("一",1,"两",2,"二",2,"三",3,"四",4,"五",5).getOrDefault(ns, ns.matches("\\d+") ? Integer.parseInt(ns) : 1); }
        String address = "三号宿舍楼";
        Matcher am = Pattern.compile("送到?(\\S{2,6})").matcher(text);
        if (am.find()) address = am.group(1);
        String name = (String) matched.get("name");
        double price = matched.get("price") instanceof Number ? ((Number) matched.get("price")).doubleValue() : 0;
        double total = price * qty;
        String oid = "ORD" + System.currentTimeMillis();
        trace.append("[smart_order] 匹配: ").append(name).append(" x").append(qty).append(" ¥").append(total).append("\n");
        orderClient.createOrder(Map.of("order_id", oid, "user_id", userId, "store_id", matched.get("store_id"), "items", List.of(name), "amount", total, "type", "m001".equals(matched.get("store_id")) ? "外卖" : "电商", "address", address));
        return Map.of("intent", "下单", "answer", "已帮你下单!\n  订单号: " + oid + "\n  商品: " + name + " x" + qty + "\n  金额: ¥" + String.format("%.2f", total) + "\n  送达: " + address + "\n  预计30分钟内送达,请留意骑手电话。");
    }

    private boolean isOrdering(String text) {
        return Arrays.asList("点一份","下一单","来一杯","来一份","帮我点","帮我下","下单","点个","来杯","来碗","帮我叫","叫一份","要一杯","要一份","帮我买","我点","帮我订","订一份").stream().anyMatch(text::contains);
    }
    /** 检测政策查询意图 → RAG 检索词 */
    private String detectPolicyQuery(String text) {
        if (text.contains("超时") || text.contains("配送")) return "超时补偿 配送时效";
        if (text.contains("退货") || text.contains("换货")) return "退货政策 无理由退货";
        if (text.contains("质量") || text.contains("变质") || text.contains("异味")) return "食品安全 商品质量";
        return "退款政策 售后";
    }

    /** 构建响应 Map（允许 null 值） */
    private static Map<String, Object> response(String intent, String answer, StringBuilder trace) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intent", intent);
        map.put("answer", answer);
        map.put("trace", trace.toString());
        return map;
    }

    /** 构建含 map_data 的响应 */
    private static Map<String, Object> responseWithMap(String intent, String answer, StringBuilder trace, Object mapData) {
        Map<String, Object> map = response(intent, answer, trace);
        map.put("map_data", mapData);
        return map;
    }

    private boolean isOpenEnded(String text) {
        if (extractOid(text) != null) return false;
        return Arrays.asList("怎么处理","能退","能换","有问题","不太满意","能不能","处理一下","怎么办","洒","漏","坏","破","少","缺","退掉","想退","不想要","退款","退货","不满意","不好").stream().anyMatch(text::contains);
    }
}
