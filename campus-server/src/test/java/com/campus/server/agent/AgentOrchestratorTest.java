package com.campus.server.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Agent 编排器单元测试 — SOA: 构造函数接受 Feign 客户端 */
class AgentOrchestratorTest {

    // 仅测试路由逻辑，不需要 Feign 客户端
    private final AgentOrchestrator agent = new AgentOrchestrator(null, null, null, null, null);

    @Test
    void testRouterAfterSale() {
        assertEquals("售后", agent.router("我要退款"));
        assertEquals("售后", agent.router("订单损坏了要退货"));
        assertEquals("售后", agent.router("有补偿吗"));
        assertEquals("售后", agent.router("菜洒了怎么办"));
    }

    @Test
    void testRouterLogistics() {
        assertEquals("物流", agent.router("订单到哪了"));
        assertEquals("物流", agent.router("查物流配送进度"));
        assertEquals("物流", agent.router("什么时候到"));
        assertEquals("物流", agent.router("快递路线地图"));
    }

    @Test
    void testRouterShopping() {
        assertEquals("导购", agent.router("蓝牙耳机多少钱"));
        assertEquals("导购", agent.router("推荐什么好"));
        assertEquals("导购", agent.router("有没有机械键盘"));
    }

    @Test
    void testRouterOther() {
        assertEquals("其他", agent.router("你好"));
        assertEquals("其他", agent.router("谢谢"));
    }
}
