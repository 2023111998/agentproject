#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
课程报告图表自动生成脚本 (v2 — 修复显示问题)
生成: 用例图、系统架构图(四层)、部署架构图、BPMN流程图、Jenkins CI/CD流水线、SOA架构图
输出目录: screenshots/

修复要点:
- 增大字号(最小10pt)，提高可读性
- 使用 Source Han Sans CN / Microsoft YaHei 保证中文渲染
- savefig pad_inches=0.3 防止内容裁切
- DPI 200 确保清晰
- 简化复杂形状(菱形网关→圆角矩形+菱形标记)
"""
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Arc, Polygon, Circle
import matplotlib.font_manager as fm
import numpy as np
import os
import sys

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'screenshots')
os.makedirs(OUT_DIR, exist_ok=True)

# ── 字体选择 ──────────────────────────────────────────
# 清除 matplotlib 字体缓存，确保使用正确字体
_CACHE_DIR = matplotlib.get_cachedir()
for _cf in os.listdir(_CACHE_DIR):
    if _cf.endswith('.json'):
        _cp = os.path.join(_CACHE_DIR, _cf)
        try: os.remove(_cp)
        except: pass

# 强制重建字体列表
fm._load_fontmanager(try_read_cache=False)

# 优先使用 Microsoft YaHei (系统自带，稳定)，回退到 SimHei / Source Han Sans CN
_FONT_CANDIDATES = [
    'Microsoft YaHei',
    'SimHei',
    'Source Han Sans CN',
    'Noto Sans SC',
]
_available = {f.name for f in fm.fontManager.ttflist}
FONT_FAMILY = None
for fc in _FONT_CANDIDATES:
    if fc in _available:
        FONT_FAMILY = fc
        break
if FONT_FAMILY is None:
    FONT_FAMILY = 'sans-serif'

plt.rcParams['font.sans-serif'] = [FONT_FAMILY, 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

print(f'[INFO] Using font: {FONT_FAMILY}')

# ── 全局配色 ──────────────────────────────────────────
C_BG       = '#FAFBFC'
C_BORDER   = '#2C3E50'
C_USER     = '#3498DB'
C_MERCHANT = '#E67E22'
C_RIDER    = '#27AE60'
C_SYSTEM   = '#9B59B6'
C_FRONTEND = '#1ABC9C'
C_APP      = '#2980B9'
C_DOMAIN   = '#8E44AD'
C_INFRA    = '#34495E'
C_AGENT    = '#E74C3C'
C_TEXT     = '#2C3E50'
C_WHITE    = '#FFFFFF'
C_LIGHT_GRAY = '#F4F6F7'

# ── 颜色工具 ──────────────────────────────────────────
def desaturate(hex_color, amount):
    import matplotlib.colors as mc
    rgb = mc.to_rgb(hex_color)
    return tuple(c + (1-c)*amount for c in rgb)

# ================================================================
# 图1: 用例图
# ================================================================
def draw_usecase():
    fig, ax = plt.subplots(figsize=(18, 11))
    ax.set_xlim(0, 18); ax.set_ylim(0, 11)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # 系统边界
    system_box = FancyBboxPatch((2, 0.8), 14, 9.5, boxstyle="round,pad=0.5",
                                 facecolor=C_WHITE, edgecolor=C_SYSTEM, linewidth=2.5, zorder=1)
    ax.add_patch(system_box)
    ax.text(9, 10.3, '校园电商/外卖智能服务平台 系统边界', ha='center', fontsize=16, fontweight='bold', color=C_SYSTEM)

    # ── 用例（椭圆） ──
    def draw_usecase_ellipse(cx, cy, text, color=C_TEXT):
        ell = FancyBboxPatch((cx-2.8, cy-0.55), 5.6, 1.1, boxstyle="round,pad=0.3",
                              facecolor=C_WHITE, edgecolor=color, linewidth=1.5, zorder=5)
        ax.add_patch(ell)
        ax.text(cx, cy, text, ha='center', va='center', fontsize=10, color=C_TEXT, zorder=10)

    draw_usecase_ellipse(11, 8.5, '选商家/选商品/下单')
    draw_usecase_ellipse(11, 7.3, '查看我的订单')
    draw_usecase_ellipse(11, 6.1, '智能助理咨询')
    draw_usecase_ellipse(11, 4.6, '上架/管理商品')
    draw_usecase_ellipse(11, 3.5, '查看店铺订单')
    draw_usecase_ellipse(11, 2.4, '人工审核退款(≥100元)')
    draw_usecase_ellipse(11, 1.3, '浏览可接订单/接单/配送')

    # ── 智能助理子框 ──
    ai_box = FancyBboxPatch((13.8, 5.0), 3.8, 2.6, boxstyle="round,pad=0.2",
                             facecolor='#FDEDEC', edgecolor=C_AGENT, linewidth=2, zorder=6)
    ax.add_patch(ai_box)
    ax.text(15.7, 7.3, '智能助理', ha='center', fontweight='bold', fontsize=11, color=C_AGENT)
    ax.text(15.7, 6.6, '· 物流地图\n· 售后退款\n· 商品导购', ha='center', fontsize=9, color=C_TEXT, linespacing=1.6)

    # ── 角色 (Actor 图标 + 标签) ──
    def draw_role(ax, x, y, label, color):
        # 圆形头像
        head = Circle((x, y+1.4), 0.28, fc=color, ec=C_BORDER, linewidth=2, zorder=10)
        ax.add_patch(head)
        # 身体线
        ax.plot([x, x], [y+1.1, y+0.45], 'k-', linewidth=2.5, zorder=10)
        # 手臂
        ax.plot([x, x-0.55], [y+0.8, y+0.15], 'k-', linewidth=2, zorder=10)
        ax.plot([x, x+0.55], [y+0.8, y+0.15], 'k-', linewidth=2, zorder=10)
        # 腿
        ax.plot([x, x-0.4], [y+0.45, y-0.2], 'k-', linewidth=2, zorder=10)
        ax.plot([x, x+0.4], [y+0.45, y-0.2], 'k-', linewidth=2, zorder=10)
        # 标签
        ax.text(x, y-0.5, label, ha='center', fontsize=12, fontweight='bold', color=color)

    draw_role(ax, 3.7, 8.2, '用户\n(学生)', C_USER)
    draw_role(ax, 3.7, 4.5, '商家', C_MERCHANT)
    draw_role(ax, 3.7, 1.2, '骑手', C_RIDER)

    # ── 连线 ──
    def connect(ax, x1, y1, x2, y2):
        ax.plot([x1, x2], [y1, y2], 'k-', linewidth=1.2, zorder=3)

    # 用户 -> 用例
    connect(ax, 4.5, 8.7, 8.2, 8.5)
    connect(ax, 4.5, 8.5, 8.2, 7.3)
    connect(ax, 4.5, 8.3, 8.2, 6.1)
    # 商家 -> 用例
    connect(ax, 4.5, 4.6, 8.2, 4.6)
    connect(ax, 4.5, 4.4, 8.2, 3.5)
    connect(ax, 4.5, 4.2, 8.2, 2.4)
    # 骑手 -> 用例
    connect(ax, 4.5, 1.2, 8.2, 1.3)
    # 智能助理关联
    ax.plot([13.8, 13.8], [6.1, 6.3], '--', linewidth=1, color=C_AGENT, zorder=3)

    ax.set_title('图1-1 系统用例图', fontsize=16, fontweight='bold', color=C_BORDER, pad=20)
    path = os.path.join(OUT_DIR, 'usecase-diagram.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] usecase-diagram.png')

# ================================================================
# 图2: 系统架构图 (四层) — 已验证正常，小幅优化
# ================================================================
def draw_architecture():
    fig, ax = plt.subplots(figsize=(18, 12))
    ax.set_xlim(0, 18); ax.set_ylim(0, 13)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    layers = [
        (0.8, 10.0, 16.4, 2.2, '前端层 — Vue 3 CDN + Leaflet', C_FRONTEND,
         ['customer.html\n用户端', 'merchant.html\n商家端', 'rider.html\n骑手端', 'index.html\n智能助理独立页']),
        (0.8, 7.2, 16.4, 2.2, '应用层 — Spring Boot 3.2 + Nginx', C_APP,
         ['campus-server-1\n:8000 (weight=3)', 'campus-server-2\n:8000 (weight=3)', 'Nginx :80\n反向代理/负载均衡', 'SessionStore\nRedis 会话共享']),
        (0.8, 3.8, 16.4, 2.8, '领域层 — 核心业务逻辑', C_DOMAIN,
         ['AgentOrchestrator\nrouter()→物流/售后/导购\nBPMN/ReAct/smart', 'BpmnEngine (StAX)\nBpmnHandlers×9\nsafeEval安全求值',
          'RagService\nn-gram TF RAG\n余弦相似度检索', 'Guardrails\n输入注入/授权越权\nPII脱敏']),
        (0.8, 0.5, 16.4, 2.7, '基础设施层 — 微服务 + 数据库 + 监控', C_INFRA,
         ['order-service\n:8001 (Feign)', 'product-service\n:8002 (Feign)', 'logistics-service\n:8003 (Feign)',
          'MySQL 8.0\n:3306 7表', 'Redis 7\n:6379', 'Prometheus\n+Grafana', 'Swagger UI\nAPI 文档']),
    ]

    for x, y, w, h, title, color, items in layers:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.3",
                              facecolor=C_WHITE, edgecolor=color, linewidth=2.5, zorder=2)
        ax.add_patch(box)
        ax.text(x+0.4, y+h-0.35, title, fontsize=13, fontweight='bold', color=color, va='top', zorder=10)

        n = len(items)
        item_w = (w - 1.2) / n
        for i, item in enumerate(items):
            ix = x + 0.4 + i * item_w
            iw = item_w - 0.3
            ibox = FancyBboxPatch((ix, y+0.3), iw, h-1.2, boxstyle="round,pad=0.15",
                                   facecolor=desaturate(color, 0.85), edgecolor=color, linewidth=1.2, zorder=5)
            ax.add_patch(ibox)
            ax.text(ix+iw/2, y+h/2-0.05, item, ha='center', va='center', fontsize=9.5, color=C_TEXT, zorder=10)

    # 层间箭头
    for y_pos in [10.0, 7.2, 3.8]:
        ax.annotate('', xy=(9, y_pos+0.08), xytext=(9, y_pos-0.45),
                    arrowprops=dict(arrowstyle='->', lw=2.5, color='#95A5A6'))

    ax.set_title('图1-2 系统四层架构图', fontsize=16, fontweight='bold', color=C_BORDER, pad=20)
    path = os.path.join(OUT_DIR, 'architecture-diagram.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] architecture-diagram.png')

# ================================================================
# 图3: 部署架构图
# ================================================================
def draw_deployment():
    fig, ax = plt.subplots(figsize=(18, 10))
    ax.set_xlim(0, 18); ax.set_ylim(0, 11)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # K8s Cluster 边框
    k8s = FancyBboxPatch((0.5, 0.5), 13.5, 10, boxstyle="round,pad=0.5",
                          facecolor=C_LIGHT_GRAY, edgecolor=C_INFRA, linewidth=3, linestyle='--', zorder=1)
    ax.add_patch(k8s)
    ax.text(7.25, 10.2, 'Kubernetes Cluster', ha='center', fontsize=15, fontweight='bold', color=C_INFRA)

    # Namespace
    ns = FancyBboxPatch((1, 1), 12.5, 8.5, boxstyle="round,pad=0.3",
                          facecolor=C_WHITE, edgecolor='#5D6D7E', linewidth=1.5, zorder=2)
    ax.add_patch(ns)
    ax.text(1.3, 9.2, 'Namespace: campus-prod', fontsize=12, fontweight='bold', color='#5D6D7E')

    # Nginx LB
    lb = FancyBboxPatch((2, 8.5), 10.5, 0.9, boxstyle="round,pad=0.12",
                         facecolor='#27AE60', edgecolor='#1E8449', linewidth=2, zorder=5)
    ax.add_patch(lb)
    ax.text(7.25, 8.95, 'LoadBalancer :80 → Nginx upstream (轮询, weight=3)', ha='center', fontsize=12, fontweight='bold', color=C_WHITE)

    # 四个微服务
    services = [
        (2, 5.5, 2.8, 2.2, 'campus-server\n(x2+)', C_APP, 'HPA: min=2, max=10\nCPU>70% 自动扩缩'),
        (5.2, 5.5, 2.5, 2.2, 'order-service\n(x2)', '#E67E22', ':8001'),
        (8.1, 5.5, 2.5, 2.2, 'product-service\n(x2)', '#2ECC71', ':8002'),
        (11, 5.5, 2.5, 2.2, 'logistics-service\n(x2)', '#F39C12', ':8003'),
    ]
    for x, y, w, h, name, color, detail in services:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.12",
                              facecolor=C_WHITE, edgecolor=color, linewidth=2.5, zorder=5)
        ax.add_patch(box)
        ax.text(x+w/2, y+h-0.4, name, ha='center', va='top', fontsize=11, fontweight='bold', color=color)
        ax.text(x+w/2, y+0.35, detail, ha='center', va='bottom', fontsize=9, color=C_TEXT)

    # 基础设施
    infra_items = [
        (2, 1.5, 2.8, 2.5, 'MySQL 8.0', '#8E44AD', ':3306\nInnoDB, UTF8mb4\nPVC 5Gi'),
        (5.2, 1.5, 2.5, 2.5, 'Redis 7', '#D35400', ':6379\n会话共享'),
        (8.1, 1.5, 2.5, 2.5, 'Prometheus\n+ Grafana', '#C0392B', ':9091 / :3000\n11面板仪表盘'),
        (11, 1.5, 2.5, 2.5, 'ConfigMap\n+ Secret', '#7F8C8D', '环境配置\n密钥管理'),
    ]
    for x, y, w, h, name, color, detail in infra_items:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.12",
                              facecolor=C_WHITE, edgecolor=color, linewidth=2.5, zorder=5)
        ax.add_patch(box)
        ax.text(x+w/2, y+h-0.35, name, ha='center', va='top', fontsize=11, fontweight='bold', color=color)
        ax.text(x+w/2, y+0.35, detail, ha='center', va='bottom', fontsize=9, color=C_TEXT)

    # 连接箭头 (LB -> services)
    for x, y, w, h, name, color, detail in services:
        ax.annotate('', xy=(x+w/2, 7.7), xytext=(7.25, 8.5),
                    arrowprops=dict(arrowstyle='->', lw=1.5, color='#95A5A6', linestyle=':'))

    # Jenkins (右侧)
    jenkins_box = FancyBboxPatch((14.5, 5), 3, 5.5, boxstyle="round,pad=0.2",
                                  facecolor=C_WHITE, edgecolor=C_BORDER, linewidth=2.5, zorder=5)
    ax.add_patch(jenkins_box)
    ax.text(16, 10.2, 'Jenkins CI/CD', ha='center', fontsize=12, fontweight='bold', color=C_BORDER)
    ax.text(16, 9.0, 'Git → Build → Test\n→ Package → Eval\n→ Docker Build\n→ Push → Deploy',
            ha='center', fontsize=9.5, color=C_TEXT, linespacing=1.6)

    ax.plot([14.5, 13.5], [9, 9], 'k-', linewidth=1.5, linestyle=':')
    ax.text(14, 9.3, 'kubectl apply\n滚动更新', ha='center', fontsize=8, color='#7F8C8D')

    ax.set_title('图1-3 Kubernetes 部署架构图', fontsize=16, fontweight='bold', color=C_BORDER, pad=20)
    path = os.path.join(OUT_DIR, 'deployment-diagram.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] deployment-diagram.png')

# ================================================================
# 图4: SOA 服务架构图
# ================================================================
def draw_soa():
    fig, ax = plt.subplots(figsize=(18, 9))
    ax.set_xlim(0, 18); ax.set_ylim(0, 10)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # Nginx LB
    ng = FancyBboxPatch((5, 8.5), 8, 1, boxstyle="round,pad=0.12",
                         facecolor='#27AE60', edgecolor='#1E8449', linewidth=2.5)
    ax.add_patch(ng)
    ax.text(9, 9.0, 'Nginx :80 — 反向代理 + 负载均衡 (upstream轮询, weight=3)', ha='center', fontsize=12, fontweight='bold', color=C_WHITE)

    # campus-server x2
    for i, off_x in enumerate([-3, 1]):
        cs = FancyBboxPatch((5.5+off_x, 6.0), 3.2, 1.8, boxstyle="round,pad=0.12",
                              facecolor=C_WHITE, edgecolor=C_APP, linewidth=2.5)
        ax.add_patch(cs)
        ax.text(7.1+off_x, 7.3, f'campus-server-{i+1}\n:8000', ha='center', fontsize=11, fontweight='bold', color=C_APP)
        ax.text(7.1+off_x, 6.5, 'AgentOrchestrator\nBpmnEngine + RagService\nGuardrails + SLA', ha='center', fontsize=8, color=C_TEXT)

    # LB -> servers 箭头
    for off_x in [-3, 1]:
        ax.annotate('', xy=(7.1+off_x, 7.8), xytext=(9, 8.5),
                    arrowprops=dict(arrowstyle='->', lw=1.5, color='#95A5A6'))

    # 微服务
    ms_data = [
        (1.2, 2.5, 'order-service\n:8001', '订单 CRUD\n退款处理\n骑手接单', '#E67E22', '#D35400'),
        (6.5, 2.5, 'product-service\n:8002', '商品搜索\n上下架\n库存管理', '#2ECC71', '#27AE60'),
        (11.8, 2.5, 'logistics-service\n:8003', '物流查询\nGPS 路线\n超时判断', '#F39C12', '#E67E22'),
    ]
    for x, y, name, desc, fc, ec in ms_data:
        ms = FancyBboxPatch((x, y), 4.6, 2.2, boxstyle="round,pad=0.12",
                              facecolor=C_WHITE, edgecolor=ec, linewidth=2.5, zorder=5)
        ax.add_patch(ms)
        ax.text(x+2.3, y+1.5, name, ha='center', fontsize=11, fontweight='bold', color=ec)
        ax.text(x+2.3, y+0.55, desc, ha='center', fontsize=9.5, color=C_TEXT)
        # 连接
        ax.annotate('', xy=(x+2.3, y+2.2), xytext=(9, 6.0),
                    arrowprops=dict(arrowstyle='->', lw=1, color='#BDC3C7', linestyle=':'))

    # Feign 标注
    ax.text(9, 4.5, '▲ Spring Cloud OpenFeign 声明式服务调用', ha='center', fontsize=10,
            color='#7F8C8D', style='italic',
            bbox=dict(boxstyle='round,pad=0.3', facecolor=C_LIGHT_GRAY, edgecolor='#BDC3C7', alpha=0.8))

    # 基础设施层
    infra = FancyBboxPatch((5, 0.5), 8, 1.3, boxstyle="round,pad=0.1",
                            facecolor=C_INFRA, edgecolor='#2C3E50', linewidth=2.5)
    ax.add_patch(infra)
    ax.text(9, 1.4, 'MySQL 8.0 :3306  |  Redis 7 :6379  |  Resilience4j 断路器+重试+超时  |  Spring Actuator + Prometheus',
            ha='center', fontsize=10, color=C_WHITE, fontweight='bold')

    ax.set_title('图1-4 SOA 面向服务架构图', fontsize=16, fontweight='bold', color=C_BORDER, pad=20)
    path = os.path.join(OUT_DIR, 'soa-architecture.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] soa-architecture.png')

# ================================================================
# 图5: BPMN 流程图 — 重写，避免菱形裁切问题
# ================================================================
def draw_bpmn():
    """BPMN 流程图 — 两个子图并排，使用圆角矩形+菱形标记替代真菱形"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(22, 11))
    fig.suptitle('图2-1 BPMN 业务流程图', fontsize=16, fontweight='bold', color=C_BORDER, y=0.98)
    for ax in [ax1, ax2]:
        ax.set_facecolor(C_BG)
        ax.axis('off')

    COLOR_EVENT = '#3498DB'
    COLOR_TASK = '#2ECC71'
    COLOR_GATE = '#F39C12'
    COLOR_USER = '#E74C3C'

    # ── 售后流程 ──
    ax1.set_xlim(0, 18); ax1.set_ylim(0, 16)
    ax1.set_title('(a) 售后退款流程 (aftersale_refund.bpmn)', fontsize=13, fontweight='bold', color='#8E44AD', pad=15)

    def draw_node(ax, x, y, w, h, label, color, extra=None, fontsize=9):
        """绘制 BPMN 节点（圆角矩形）"""
        box = FancyBboxPatch((x-w/2, y-h/2), w, h, boxstyle="round,pad=0.15",
                              facecolor=C_WHITE, edgecolor=color, linewidth=2.5, zorder=5)
        ax.add_patch(box)
        ax.text(x, y, label, ha='center', va='center', fontsize=fontsize, color=C_TEXT, zorder=10)
        if extra:
            ax.text(x+w/2+0.3, y+h/2+0.1, extra, fontsize=8, color='red', fontweight='bold', zorder=10)

    def draw_gate(ax, x, y, label):
        """绘制网关 — 圆角矩形 + 内部菱形标记"""
        # 外层圆角矩形
        box = FancyBboxPatch((x-2.0, y-0.85), 4.0, 1.7, boxstyle="round,pad=0.2",
                              facecolor=C_WHITE, edgecolor=COLOR_GATE, linewidth=2.5, zorder=5)
        ax.add_patch(box)
        # 内部小菱形标记
        diamond = Polygon([(x, y+0.35), (x+0.4, y), (x, y-0.35), (x-0.4, y)],
                          facecolor=COLOR_GATE, edgecolor='#D68910', linewidth=1, zorder=6, alpha=0.6)
        ax.add_patch(diamond)
        ax.text(x, y-0.3, label, ha='center', va='top', fontsize=9, color=C_TEXT, zorder=10)

    # 售后节点布局 (x, y, w, h)
    NODE_W, NODE_H = 4.8, 1.3
    # Start
    draw_node(ax1, 9, 15.5, NODE_W*0.7, NODE_H, 'Start\n收到售后请求', COLOR_EVENT)
    # 查询订单
    draw_node(ax1, 9, 13.5, NODE_W, NODE_H, 'Task_QueryOrder\nFeign 查询订单+物流', COLOR_TASK)
    # 分类原因
    draw_node(ax1, 9, 11.5, NODE_W, NODE_H, 'Task_ClassifyReason\n智能分类售后原因', COLOR_TASK, extra='★创新①')
    # 网关: 配送类?
    draw_gate(ax1, 9, 9.8, 'Gw_IsDeliveryIssue\n是否配送类问题?')
    # 分支
    draw_node(ax1, 4, 8.0, NODE_W, NODE_H, 'Task_DeliveryPolicy\n查超时补偿政策(RAG)', COLOR_TASK)
    draw_node(ax1, 14, 8.0, NODE_W, NODE_H, 'Task_ProductPolicy\n查商品退款政策(RAG)', COLOR_TASK)
    # 网关: 金额
    draw_gate(ax1, 4, 5.8, 'Gw_Amount\n金额 ≥ 100?')
    draw_gate(ax1, 14, 5.8, 'Gw_Amount\n金额 ≥ 100?')
    # 退款
    draw_node(ax1, 1.8, 3.5, NODE_W*0.9, NODE_H, 'Task_ManualReview\n人工审核', COLOR_USER)
    draw_node(ax1, 6.5, 3.5, NODE_W*0.9, NODE_H, 'Task_AutoRefund\n自动发起退款', COLOR_TASK)
    draw_node(ax1, 11.5, 3.5, NODE_W*0.9, NODE_H, 'Task_AutoRefund\n自动发起退款', COLOR_TASK)
    draw_node(ax1, 16.5, 3.5, NODE_W*0.9, NODE_H, 'Task_ManualReview\n人工审核', COLOR_USER)
    # 通知
    draw_node(ax1, 4, 1.5, NODE_W*0.9, NODE_H, 'Task_Notify\n汇总通知用户', COLOR_TASK)
    draw_node(ax1, 14, 1.5, NODE_W*0.9, NODE_H, 'Task_Notify\n汇总通知用户', COLOR_TASK)
    # End
    draw_node(ax1, 4, -0.3, NODE_W*0.7, NODE_H, 'End\n售后处理完成', COLOR_EVENT)
    draw_node(ax1, 14, -0.3, NODE_W*0.7, NODE_H, 'End\n售后处理完成', COLOR_EVENT)

    # ── 连线 ──
    def arrow(ax, x1, y1, x2, y2, color='#7F8C8D'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                     arrowprops=dict(arrowstyle='->', lw=1.5, color=color))

    # 主流程
    arrow(ax1, 9, 14.85, 9, 14.15)
    arrow(ax1, 9, 12.85, 9, 12.15)
    arrow(ax1, 9, 10.85, 9, 10.65)
    # 网关分支
    arrow(ax1, 9, 9.0, 4, 8.65, '#27AE60')
    arrow(ax1, 9, 9.0, 14, 8.65, '#E74C3C')
    ax1.text(6.3, 9.1, '是(配送类)', fontsize=9, color='#27AE60', fontweight='bold')
    ax1.text(11.5, 9.1, '否(商品类)', fontsize=9, color='#E74C3C', fontweight='bold')
    # 策略 -> 金额网关
    arrow(ax1, 4, 7.35, 4, 6.65)
    arrow(ax1, 14, 7.35, 14, 6.65)
    # 金额网关分支
    arrow(ax1, 3.0, 5.0, 1.8, 4.15, '#E74C3C')
    arrow(ax1, 5.0, 5.0, 6.5, 4.15, '#27AE60')
    arrow(ax1, 13.0, 5.0, 11.5, 4.15, '#27AE60')
    arrow(ax1, 15.0, 5.0, 16.5, 4.15, '#E74C3C')
    ax1.text(2.2, 5.1, '≥100', fontsize=9, color='#E74C3C', fontweight='bold')
    ax1.text(5.2, 5.1, '<100', fontsize=9, color='#27AE60', fontweight='bold')
    ax1.text(12.2, 5.1, '<100', fontsize=9, color='#27AE60', fontweight='bold')
    ax1.text(15.2, 5.1, '≥100', fontsize=9, color='#E74C3C', fontweight='bold')
    # -> 通知
    arrow(ax1, 3.2, 2.85, 4, 2.15)
    arrow(ax1, 5.2, 2.85, 4, 2.15)
    arrow(ax1, 13.2, 2.85, 14, 2.15)
    arrow(ax1, 15.2, 2.85, 14, 2.15)
    # -> End
    arrow(ax1, 4, 0.85, 4, 0.35)
    arrow(ax1, 14, 0.85, 14, 0.35)

    # ── 物流流程 ──
    ax2.set_xlim(0, 18); ax2.set_ylim(0, 16)
    ax2.set_title('(b) 物流地图流程 (logistics_map.bpmn)', fontsize=13, fontweight='bold', color='#2980B9', pad=15)

    draw_node(ax2, 9, 15.5, NODE_W*0.7, NODE_H, 'Start\n收到物流查询', COLOR_EVENT)
    draw_node(ax2, 9, 13.5, NODE_W, NODE_H, 'Task_QueryLogistics\n查询物流+路线数据', COLOR_TASK)
    draw_gate(ax2, 9, 11.5, 'Gw_HasMapData\n是否有地图数据?')
    draw_node(ax2, 14, 9, NODE_W, NODE_H, 'Task_BuildRouteMap\n构建配送路线地图\n(★创新②)', COLOR_TASK)
    draw_node(ax2, 4, 9, NODE_W, NODE_H, '提供文字信息\n返回订单状态', COLOR_TASK)
    draw_node(ax2, 14, 6.5, NODE_W*0.7, NODE_H, 'End\n返回地图信息', COLOR_EVENT)
    draw_node(ax2, 4, 6.5, NODE_W*0.7, NODE_H, 'End\n返回基本信息', COLOR_EVENT)

    # 连线
    arrow(ax2, 9, 14.85, 9, 14.15)
    arrow(ax2, 9, 12.85, 9, 12.4)
    arrow(ax2, 10.5, 11.0, 14, 9.65, '#27AE60')
    arrow(ax2, 7.5, 11.0, 4, 9.65, '#E74C3C')
    ax2.text(12.5, 10.6, '有地图', fontsize=10, color='#27AE60', fontweight='bold')
    ax2.text(5.8, 10.6, '无地图', fontsize=10, color='#E74C3C', fontweight='bold')
    arrow(ax2, 14, 8.35, 14, 7.15)
    arrow(ax2, 4, 8.35, 4, 7.15)

    # 图例
    legend_items = [
        (ax1, 15, 14.5, COLOR_EVENT, '开始/结束事件'),
        (ax1, 15, 13.2, COLOR_TASK, '服务任务'),
        (ax1, 15, 11.9, COLOR_GATE, '排他网关'),
        (ax1, 15, 10.6, COLOR_USER, '用户任务'),
    ]
    for a, x, y, c, label in legend_items:
        a.add_patch(FancyBboxPatch((x-0.6, y-0.3), 1.2, 0.6, boxstyle="round,pad=0.1",
                                     facecolor=C_WHITE, edgecolor=c, linewidth=2, zorder=10))
        a.text(x+1.0, y, label, fontsize=9, color=C_TEXT, va='center')

    path = os.path.join(OUT_DIR, 'bpmn-diagram.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] bpmn-diagram.png')

# ================================================================
# 图6: Jenkins CI/CD 流水线
# ================================================================
def draw_jenkins():
    """Jenkins Pipeline — 横向流水线，增大卡片和字号"""
    fig, ax = plt.subplots(figsize=(24, 6))
    ax.set_xlim(0, 24); ax.set_ylim(0, 7)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    stages = [
        ('Checkout\n代码检出', '#3498DB', 'Git\n4重回退策略'),
        ('Build & Test\n编译+测试', '#2ECC71', 'Maven+JUnit5\n18 tests'),
        ('Static Analysis\n代码统计', '#1ABC9C', 'Shell\n2,882行/43文件'),
        ('Package\nMaven打包', '#9B59B6', 'Maven\n5 JAR (119MB)'),
        ('Evaluation\n离线评测', '#F39C12', 'API调用\n8用例 100%'),
        ('Docker Build\n镜像构建', '#E67E22', 'Docker\n4微服务镜像'),
        ('Docker Push\n推送仓库', '#D35400', 'Docker\nHarbor Registry'),
        ('Deploy to K8s\n部署上线', '#C0392B', 'kubectl\n滚动更新+回滚'),
    ]

    CARD_W = 2.5
    CARD_H = 3.5
    GAP = 0.35

    for i, (name, color, detail) in enumerate(stages):
        x = 0.5 + i * (CARD_W + GAP)
        # Stage card
        box = FancyBboxPatch((x, 1.8), CARD_W, CARD_H, boxstyle="round,pad=0.15",
                              facecolor=C_WHITE, edgecolor=color, linewidth=2.5, zorder=5)
        ax.add_patch(box)
        # Header bar
        header = FancyBboxPatch((x, 1.8+CARD_H-1.1), CARD_W, 1.1, boxstyle="round,pad=0.1",
                                 facecolor=color, edgecolor=color, linewidth=0, zorder=6)
        ax.add_patch(header)
        ax.text(x+CARD_W/2, 1.8+CARD_H-0.55, name, ha='center', va='center', fontsize=10, fontweight='bold', color=C_WHITE)
        # Detail
        ax.text(x+CARD_W/2, 2.3, detail, ha='center', va='center', fontsize=9, color=C_TEXT)
        # Arrow between cards
        if i < len(stages) - 1:
            arrow_x = x + CARD_W + 0.05
            ax.annotate('', xy=(arrow_x + GAP - 0.05, 1.8+CARD_H/2), xytext=(arrow_x, 1.8+CARD_H/2),
                        arrowprops=dict(arrowstyle='->', lw=2.5, color='#95A5A6'))

    # 标题
    ax.text(12, 6.2, 'Jenkins CI/CD Pipeline — 8 阶段全自动化流水线', ha='center', fontsize=15, fontweight='bold', color=C_BORDER)
    ax.text(12, 5.7, '参数化构建: DEPLOY_ENV(dev/staging/prod) | RUN_EVAL | SKIP_DEPLOY | SKIP_TESTS | DOCKER_TAG_OVERRIDE',
            ha='center', fontsize=9.5, color='#7F8C8D')

    path = os.path.join(OUT_DIR, 'jenkins-pipeline-diagram.png')
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] jenkins-pipeline-diagram.png')

# ================================================================
# 主入口
# ================================================================
if __name__ == '__main__':
    print(f'开始生成课程报告图表 (字体: {FONT_FAMILY})...\n')
    draw_usecase()
    draw_architecture()
    draw_deployment()
    draw_soa()
    draw_bpmn()
    draw_jenkins()
    print(f'\n全部 6 张图表已保存到: {OUT_DIR}')
