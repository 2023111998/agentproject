package com.campus.server.session;

import java.util.List;
import java.util.Map;

/**
 * 会话存储接口 — SOA 无状态服务支持。
 *
 * 提供两个实现:
 * - InMemorySessionStore: HashMap (默认, 单实例开发环境)
 * - RedisSessionStore: 共享存储 (多实例水平扩展)
 */
public interface SessionStore {

    /** 添加一轮对话 */
    void add(String userId, String role, String content);

    /** 获取最近历史 */
    List<Map<String, String>> getHistory(String userId);

    /** 获取会话摘要 */
    String getSummary(String userId);

    /** 保存长期记忆 */
    void remember(String userId, String key, String value);

    /** 获取长期记忆 */
    Map<String, String> getProfile(String userId);
}
