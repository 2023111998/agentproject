package com.campus.server.guardrails;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 安全护栏三层：输入注入检测 / 授权越权校验 / 输出 PII 脱敏。
 * 对应 Python 版 guardrails.py
 */
public class Guardrails {

    private final JdbcTemplate db;
    private static final Set<String> INJECTION = Set.of(
        "忽略以上", "忽略之前", "ignore previous", "ignore above",
        "你现在是", "把所有", "管理员", "系统指令", "system prompt"
    );
    private static final Set<String> INJECTION_STRICT = Set.of(
        "忽略以上所有指令", "ignore all previous instructions", "你现在是一个", "系统指令是"
    );

    public Guardrails(JdbcTemplate db) { this.db = db; }

    /** 输入护栏：放行返回 true，拦截返回 false */
    public boolean inputGuard(String text) {
        String low = text.toLowerCase();
        for (String kw : INJECTION_STRICT) {
            if (low.contains(kw.toLowerCase())) return false;
        }
        long hits = INJECTION.stream().filter(kw -> low.contains(kw.toLowerCase())).count();
        return hits < 2;
    }

    /** 授权护栏：校验订单归属 */
    public Map<String, Object> authzGuard(String userId, String orderId) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM orders WHERE id=?", orderId);
        if (rows.isEmpty()) return Map.of("ok", false, "msg", "未找到该订单");
        if (!userId.equals(rows.get(0).get("user_id")))
            return Map.of("ok", false, "msg", "无权操作该订单");
        return Map.of("ok", true, "msg", "");
    }

    /** 输出护栏：PII 脱敏（手机号 + 身份证） */
    public String piiMask(String text) {
        if (text == null) return "";
        // 手机号: 138****5678
        text = text.replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
        // 身份证: 320***********1234
        text = text.replaceAll("(\\d{3})\\d{11}(\\d{4})", "$1***********$2");
        return text;
    }
}
