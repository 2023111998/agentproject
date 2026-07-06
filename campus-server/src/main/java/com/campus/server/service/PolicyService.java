package com.campus.server.service;

import com.campus.server.rag.RagService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 政策服务：政策 CRUD + RAG 检索 */
@Service
public class PolicyService {

    private final JdbcTemplate db;
    private final RagService rag;

    public PolicyService(JdbcTemplate db, RagService rag) {
        this.db = db;
        this.rag = rag;
    }

    /** RAG 检索政策 */
    public List<String> search(String query, int k) {
        return rag.retrieve(query, k);
    }

    /** 获取所有政策 */
    public List<Map<String, Object>> listAll() {
        return db.queryForList("SELECT * FROM policies");
    }

    /** 添加政策 */
    public Map<String, Object> add(Map<String, Object> body) {
        db.update("INSERT INTO policies(title, content) VALUES(?,?)",
            body.get("title"), body.get("content"));
        // 重新加载向量库
        rag.init();
        return Map.of("msg", "政策已添加");
    }
}
