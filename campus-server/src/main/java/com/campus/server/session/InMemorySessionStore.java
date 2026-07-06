package com.campus.server.session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存会话存储 — HashMap 实现（单实例环境）。
 * 当 Redis 不可用时作为 fallback。
 */
public class InMemorySessionStore implements SessionStore {

    private static final int WINDOW = 6;
    private final Map<String, List<Map<String, String>>> histories = new ConcurrentHashMap<>();
    private final Map<String, String> summaries = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> profiles = new ConcurrentHashMap<>();

    @Override
    public void add(String userId, String role, String content) {
        List<Map<String, String>> history = histories.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add(Map.of("role", role, "content", content));
        if (history.size() > WINDOW) {
            List<Map<String, String>> old = new ArrayList<>(history.subList(0, history.size() - WINDOW));
            history.subList(0, history.size() - WINDOW).clear();
            summaries.merge(userId, summarize(old), (a, b) -> b);
        }
    }

    private String summarize(List<Map<String, String>> msgs) {
        StringBuilder sb = new StringBuilder();
        for (var m : msgs) sb.append(m.get("role")).append(": ").append(m.get("content")).append("\n");
        return "历史摘要:" + sb;
    }

    @Override
    public List<Map<String, String>> getHistory(String userId) {
        return histories.getOrDefault(userId, List.of());
    }

    @Override
    public String getSummary(String userId) {
        return summaries.getOrDefault(userId, "");
    }

    @Override
    public void remember(String userId, String key, String value) {
        profiles.computeIfAbsent(userId, k -> new HashMap<>()).put(key, value);
    }

    @Override
    public Map<String, String> getProfile(String userId) {
        return profiles.getOrDefault(userId, Map.of());
    }
}
