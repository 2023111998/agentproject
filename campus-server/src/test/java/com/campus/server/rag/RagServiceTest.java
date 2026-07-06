package com.campus.server.rag;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** RAG 服务单元测试 */
class RagServiceTest {

    @Test
    void testNgrams() {
        List<String> grams = RagService.ngrams("你好");
        assertFalse(grams.isEmpty());
        assertTrue(grams.contains("你"));
        assertTrue(grams.contains("好"));
        assertTrue(grams.contains("你好"));
    }

    @Test
    void testNgramsChinese() {
        List<String> grams = RagService.ngrams("超时补偿");
        assertTrue(grams.contains("超"));
        assertTrue(grams.contains("时"));
        assertTrue(grams.contains("补"));
        assertTrue(grams.contains("偿"));
        // 2-grams
        assertTrue(grams.contains("超时"));
        assertTrue(grams.contains("时补"));
        assertTrue(grams.contains("补偿"));
    }

    @Test
    void testVectorStore() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("配送时效", "外卖订单承诺30分钟内送达，超时可申请红包补偿。");
        docs.put("食品安全", "外卖食品如出现异味变质等安全问题，核实后将全额退款。");
        docs.put("退款政策", "退款申请审核通过后1-3个工作日原路退回至支付账户。");

        RagService.VectorStore store = new RagService.VectorStore(docs);

        // 搜索与"超时"相关的政策
        List<RagService.VectorStore.ScoredDoc> results = store.search("超时了有没有补偿", 2);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).text().contains("超时"));

        // 搜索与"退款"相关的政策
        results = store.search("怎么退款", 2);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).text().contains("退款"));

        // 搜索与"食品质量"相关的政策
        results = store.search("食品变质了怎么办", 1);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).text().contains("变质") || results.get(0).text().contains("安全"));
    }

    @Test
    void testVectorStoreEmptyCorpus() {
        Map<String, String> docs = new LinkedHashMap<>();
        RagService.VectorStore store = new RagService.VectorStore(docs);
        List<RagService.VectorStore.ScoredDoc> results = store.search("测试", 2);
        assertTrue(results.isEmpty());
    }
}
