package com.campus.server.rag;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * RAG 检索增强生成 — 字符级 n-gram TF 向量 + 余弦相似度。
 * 对应 Python 版 rag.py (numpy n-gram 向量检索)。
 *
 * 工作流程：
 * 1. 从数据库加载政策文档
 * 2. 对每个文档做 n-gram (1,2) 分词
 * 3. 构建词表 → TF 向量矩阵
 * 4. 查询时向量化 → 余弦相似度排序 → 返回 top-k
 */
public class RagService {

    private VectorStore store;
    private final JdbcTemplate db;

    public RagService(JdbcTemplate db) {
        this.db = db;
    }

    /** 从数据库加载政策并初始化向量存储 */
    public void init() {
        List<Map<String, Object>> rows = db.queryForList("SELECT id, title, content FROM policies");
        Map<String, String> docs = new LinkedHashMap<>();
        for (var row : rows) {
            String title = (String) row.getOrDefault("title", "");
            String content = (String) row.getOrDefault("content", "");
            docs.put(title, content);
        }
        if (!docs.isEmpty()) {
            this.store = new VectorStore(docs);
        }
    }

    /** 检索最相关的 k 段政策文本 */
    public List<String> retrieve(String query, int k) {
        // 1. 向量存储未初始化 → 尝试初始化
        if (store == null) {
            init();
        }
        // 2. 向量搜索
        if (store != null) {
            List<VectorStore.ScoredDoc> results = store.search(query, k);
            if (!results.isEmpty()) {
                return results.stream().map(r -> r.text).toList();
            }
        }
        // 3. 回退: SQL LIKE 模糊匹配
        List<Map<String, Object>> rows = db.queryForList(
            "SELECT content FROM policies LIMIT ?", k);
        if (!rows.isEmpty()) {
            return rows.stream()
                .map(r -> (String) r.getOrDefault("content", ""))
                .toList();
        }
        return List.of();
    }

    /** 检索并返回带分数的结果 */
    public List<VectorStore.ScoredDoc> retrieveScored(String query, int k) {
        if (store == null) return List.of();
        return store.search(query, k);
    }

    // ====== 向量存储内部类 ======

    public static class VectorStore {
        private final List<String> ids;
        private final List<String> texts;
        private final Map<String, Integer> vocab;
        private final float[][] matrix;
        private final float[] norms;

        public record ScoredDoc(String id, String text, double score) {}

        /**
         * @param docs id → text 的映射
         */
        public VectorStore(Map<String, String> docs) {
            this.ids = new ArrayList<>(docs.keySet());
            this.texts = new ArrayList<>(docs.values());

            // 构建词表
            Map<String, Integer> vocabBuilder = new LinkedHashMap<>();
            for (String text : texts) {
                for (String gram : ngrams(text)) {
                    vocabBuilder.putIfAbsent(gram, vocabBuilder.size());
                }
            }
            this.vocab = Collections.unmodifiableMap(vocabBuilder);

            // 构建 TF 矩阵 (文档数 × 词表大小)
            int numDocs = texts.size();
            int vocabSize = vocab.size();
            this.matrix = new float[numDocs][vocabSize];
            for (int i = 0; i < numDocs; i++) {
                for (String gram : ngrams(texts.get(i))) {
                    Integer idx = vocab.get(gram);
                    if (idx != null) matrix[i][idx] += 1.0f;
                }
            }

            // 预计算每篇文档的 L2 范数
            this.norms = new float[numDocs];
            for (int i = 0; i < numDocs; i++) {
                float sumSq = 0;
                for (int j = 0; j < vocabSize; j++) {
                    sumSq += matrix[i][j] * matrix[i][j];
                }
                norms[i] = (float) Math.sqrt(sumSq) + 1e-8f;
            }
        }

        /** 检索 top-k 文档（按余弦相似度排序） */
        public List<ScoredDoc> search(String query, int k) {
            float[] queryVec = vectorize(query);
            float queryNorm = 0;
            for (float v : queryVec) queryNorm += v * v;
            queryNorm = (float) Math.sqrt(queryNorm) + 1e-8f;

            // 计算余弦相似度
            List<ScoredDoc> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                float dot = 0;
                for (int j = 0; j < queryVec.length; j++) {
                    dot += matrix[i][j] * queryVec[j];
                }
                double sim = dot / (norms[i] * queryNorm);
                if (sim > 0) {
                    results.add(new ScoredDoc(ids.get(i), texts.get(i), sim));
                }
            }

            // 按相似度降序排列，取 top-k
            results.sort((a, b) -> Double.compare(b.score, a.score));
            if (results.size() > k) results = results.subList(0, k);
            return results;
        }

        /** 将查询文本转为 TF 向量 */
        private float[] vectorize(String query) {
            float[] vec = new float[vocab.size()];
            for (String gram : ngrams(query)) {
                Integer idx = vocab.get(gram);
                if (idx != null) vec[idx] += 1.0f;
            }
            return vec;
        }
    }

    /** 中文 n-gram (1-gram + 2-gram) 分词 */
    public static List<String> ngrams(String text) {
        String cleaned = text.replace(" ", "").replace("\n", "");
        List<String> grams = new ArrayList<>();
        for (int n = 1; n <= 2; n++) {
            for (int i = 0; i <= cleaned.length() - n; i++) {
                grams.add(cleaned.substring(i, i + n));
            }
        }
        return grams;
    }
}
