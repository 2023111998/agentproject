package com.campus.server.memory;

import java.util.*;
import java.util.regex.*;

/**
 * 会话记忆：滑动窗口 + 摘要压缩 + 长期画像。
 * 对应 Python 版 memory.py
 */
public class MemoryStore {
    private final int window = 6;
    private final List<Map<String, String>> history = new ArrayList<>();
    private String summary = "";
    private final Map<String, String> profile = new HashMap<>();

    /** 添加一轮对话 */
    public void add(String role, String content) {
        history.add(Map.of("role", role, "content", content));
        if (history.size() > window) {
            List<Map<String, String>> old = new ArrayList<>(history.subList(0, history.size() - window));
            history.subList(0, history.size() - window).clear();
            summary = summarize(old);
        }
    }

    /** 压缩旧消息为摘要 */
    private String summarize(List<Map<String, String>> msgs) {
        StringBuilder sb = new StringBuilder();
        for (var m : msgs) sb.append(m.get("role")).append(": ").append(m.get("content")).append("\n");
        String base = summary.length() > 200 ? summary.substring(summary.length() - 200) : summary;
        return "历史摘要:" + base + "\n" + sb;
    }

    /** 保存长期记忆 */
    public void remember(String key, String value) { profile.put(key, value); }

    /** 在历史和摘要中回溯最近一个订单号 */
    public String recallOrder() {
        for (int i = history.size() - 1; i >= 0; i--) {
            Matcher m = Pattern.compile("\\d{8,}").matcher(history.get(i).getOrDefault("content", ""));
            if (m.find()) return m.group();
        }
        Matcher m = Pattern.compile("\\d{8,}").matcher(summary);
        return m.find() ? m.group() : null;
    }

    // -- getters --
    public Map<String, String> getProfile() { return profile; }
    public String getSummary() { return summary; }
    public List<Map<String, String>> getHistory() { return history; }
}
