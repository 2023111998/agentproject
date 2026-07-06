package com.campus.server.config;

import com.campus.server.rag.RagService;
import com.campus.server.session.InMemorySessionStore;
import com.campus.server.session.SessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 应用配置。
 *
 * SOA 改进: 移除 RestTemplate Bean — 所有服务间通信通过 Feign 客户端完成。
 */
@Configuration
public class AppConfig {

    private final JdbcTemplate db;

    public AppConfig(JdbcTemplate db) {
        this.db = db;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RagService ragService() {
        RagService rag = new RagService(db);
        rag.init();
        return rag;
    }

    /**
     * SOA: 会话存储 — 默认使用内存实现。
     * 当 classpath 中有 Redis 时自动切换到 RedisSessionStore。
     */
    @Bean
    public SessionStore sessionStore() {
        return new InMemorySessionStore();
    }
}
