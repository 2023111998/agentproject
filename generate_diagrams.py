#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
课程报告图表自动生成脚本
生成: 用例图、系统架构图(四层)、部署架构图、BPMN流程图、Jenkins CI/CD流水线
输出目录: screenshots/
"""
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Arc, Polygon
import numpy as np
import os

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'screenshots')
os.makedirs(OUT_DIR, exist_ok=True)

# 全局配色
C_BG = '#FAFBFC'
C_BORDER = '#2C3E50'
C_USER = '#3498DB'      # 用户蓝
C_MERCHANT = '#E67E22'   # 商家橙
C_RIDER = '#27AE60'      # 骑手绿
C_SYSTEM = '#9B59B6'     # 核心紫
C_FRONTEND = '#1ABC9C'   # 前端青
C_APP = '#2980B9'        # 应用蓝
C_DOMAIN = '#8E44AD'     # 领域紫
C_INFRA = '#34495E'      # 基础设施灰
C_AGENT = '#E74C3C'      # 智能助理红
C_TEXT = '#2C3E50'
C_WHITE = '#FFFFFF'

plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# ================================================================
# 图1: 用例图
# ================================================================
def draw_usecase():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # 系统边界
    system_box = FancyBboxPatch((1.5, 0.5), 13, 9, boxstyle="round,pad=0.5",
                                 facecolor=C_WHITE, edgecolor=C_SYSTEM, linewidth=2, zorder=1)
    ax.add_patch(system_box)
    ax.text(8, 9.2, '校园电商/外卖智能服务平台', ha='center', fontsize=16, fontweight='bold', color=C_SYSTEM)

    # 用户 Actor
    draw_actor(ax, 2.5, 7, '用户\n(学生)')
    ax.plot([3.2, 5.5], [7, 7], 'k-', linewidth=1)
    ax.text(5.6, 7.5, '选商家/选商品/下单', fontsize=9, color=C_TEXT)
    ax.plot([3.2, 9], [7, 6], 'k-', linewidth=1)
    ax.text(9.1, 6.2, '查看我的订单', fontsize=9, color=C_TEXT)
    ax.plot([3.2, 9], [7, 5], 'k-', linewidth=1)
    ax.text(9.1, 5.2, '智能助理咨询 →', fontsize=9, color=C_AGENT)

    # 智能助理
    ai_box = FancyBboxPatch((10, 4.5), 4, 2.5, boxstyle="round,pad=0.2",
                             facecolor='#FDEDEC', edgecolor=C_AGENT, linewidth=2)
    ax.add_patch(ai_box)
    ax.text(12, 6.7, '智能助理', ha='center', fontweight='bold', fontsize=11, color=C_AGENT)
    ax.text(12, 6.1, '• 物流地图', ha='center', fontsize=9, color=C_TEXT)
    ax.text(12, 5.6, '• 售后退款', ha='center', fontsize=9, color=C_TEXT)
    ax.text(12, 5.1, '• 商品导购', ha='center', fontsize=9, color=C_TEXT)

    # 商家 Actor
    draw_actor(ax, 2.5, 4, '商家')
    ax.plot([3.2, 11.3], [4, 3.5], 'k-', linewidth=1)
    ax.text(11.4, 3.3, '上架/编辑/下架商品', fontsize=9, color=C_TEXT)
    ax.plot([3.2, 11.3], [4, 3.0], 'k-', linewidth=1)
    ax.text(11.4, 2.8, '查看店铺订单', fontsize=9, color=C_TEXT)
    ax.plot([3.2, 11.3], [4, 2.5], 'k-', linewidth=1)
    ax.text(11.4, 2.3, '人工审核退款(≥100元)', fontsize=9, color=C_TEXT)
    ax.plot([3.2, 11.3], [4, 2.0], 'k-', linewidth=1)
    ax.text(11.4, 1.8, '查看营业统计', fontsize=9, color=C_TEXT)

    # 骑手 Actor
    draw_actor(ax, 2.5, 1, '骑手')
    ax.plot([3.2, 11.3], [1, 1], 'k-', linewidth=1)
    ax.text(11.4, 1.0, '浏览可接订单 / 接单 / 配送 / 历史', fontsize=9, color=C_TEXT)

    ax.set_title('图1-1 系统用例图', fontsize=14, fontweight='bold', color=C_BORDER, pad=15)
    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'usecase-diagram.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')

def draw_actor(ax, x, y, name):
    """绘制火柴人 Actor"""
    head = plt.Circle((x+0.5, y+1.2), 0.25, fc=C_USER, ec=C_BORDER, linewidth=1.5, zorder=10)
    ax.add_patch(head)
    ax.plot([x+0.5, x+0.5], [y+0.95, y+0.5], 'k-', linewidth=2, zorder=10)
    ax.plot([x+0.5, x+0.1], [y+0.7, y+0.2], 'k-', linewidth=1.5, zorder=10)
    ax.plot([x+0.5, x+0.9], [y+0.7, y+0.2], 'k-', linewidth=1.5, zorder=10)
    ax.plot([x+0.5, x+0.2], [y+0.5, y-0.1], 'k-', linewidth=1.5, zorder=10)
    ax.plot([x+0.5, x+0.8], [y+0.5, y-0.1], 'k-', linewidth=1.5, zorder=10)
    ax.text(x+0.5, y-0.4, name, ha='center', fontsize=10, fontweight='bold', color=C_TEXT)


# ================================================================
# 图2: 系统架构图 (四层)
# ================================================================
def draw_architecture():
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.set_xlim(0, 16); ax.set_ylim(0, 12)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    layers = [
        (0.5, 9.5, 15, 2, '前端层 (Vue 3 CDN + Leaflet)', C_FRONTEND,
         ['customer.html\n用户端', 'merchant.html\n商家端', 'rider.html\n骑手端', 'index.html\n智能助理']),
        (0.5, 7, 15, 2, '应用层 (Spring Boot + Nginx)', C_APP,
         ['campus-server-1\n:8000 (weight=3)', 'campus-server-2\n:8000 (weight=3)', 'Nginx\n:80 反向代理/负载均衡']),
        (0.5, 4, 15, 2.5, '领域层 — (核心业务逻辑)', C_DOMAIN,
         ['AgentOrchestrator\nrouter()→物流/售后/导购', 'BpmnEngine (StAX)\n+ BpmnHandlers x8', 'RagService\nn-gram TF RAG',
          'Guardrails\n输入/授权/PII']),
        (0.5, 0.5, 15, 3, '基础设施层', C_INFRA,
         ['order-service\n:8001', 'product-service\n:8002', 'logistics-service\n:8003', 'MySQL 8.0\n:3306 (7表)',
          'Redis 7\n:6379', 'Spring Actuator\nPrometheus', 'Swagger\nAPI文档']),
    ]

    for x, y, w, h, title, color, items in layers:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.3",
                              facecolor='white', edgecolor=color, linewidth=2.5, zorder=2)
        ax.add_patch(box)
        ax.text(x+0.3, y+h-0.4, title, fontsize=13, fontweight='bold', color=color, va='top', zorder=10)
        # 层级内的组件
        n = len(items)
        item_w = (w - 1) / n
        for i, item in enumerate(items):
            ix = x + 0.3 + i * item_w
            iw = item_w - 0.3
            ibox = FancyBboxPatch((ix, y+0.3), iw, h-1.2, boxstyle="round,pad=0.15",
                                   facecolor=desaturate(color, 0.85), edgecolor=color, linewidth=1, zorder=5)
            ax.add_patch(ibox)
            ax.text(ix+iw/2, y+h/2-0.1, item, ha='center', va='center', fontsize=8, color=C_TEXT, zorder=10)

    # 层间连接线
    for y_pos in [9.5, 7, 4]:
        ax.annotate('', xy=(8, y_pos+0.1), xytext=(8, y_pos-0.1),
                    arrowprops=dict(arrowstyle='->', lw=2, color='#95A5A6'))

    ax.set_title('图1-2 系统四层架构图', fontsize=14, fontweight='bold', color=C_BORDER, pad=15)
    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'architecture-diagram.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')

def desaturate(hex_color, amount):
    """Make a color lighter"""
    import matplotlib.colors as mc
    rgb = mc.to_rgb(hex_color)
    return tuple(c + (1-c)*amount for c in rgb)


# ================================================================
# 图3: 部署架构图
# ================================================================
def draw_deployment():
    fig, ax = plt.subplots(figsize=(16, 8))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # K8s Cluster 边框
    k8s = FancyBboxPatch((0.5, 0.5), 11, 9, boxstyle="round,pad=0.5",
                          facecolor='#F4F6F7', edgecolor=C_INFRA, linewidth=3, linestyle='--')
    ax.add_patch(k8s)
    ax.text(6, 9.2, 'Kubernetes Cluster', ha='center', fontsize=14, fontweight='bold', color=C_INFRA)

    # Namespace box
    ns = FancyBboxPatch((1, 1), 10, 7.5, boxstyle="round,pad=0.3",
                          facecolor='white', edgecolor='#5D6D7E', linewidth=1.5)
    ax.add_patch(ns)
    ax.text(1.3, 8.2, 'Namespace: campus-prod', fontsize=11, fontweight='bold', color='#5D6D7E')

    # 服务 boxes
    services = [
        (1.3, 5.5, 2.5, 2, 'campus-server\nx2+', C_APP, 'HPA: min=2, max=10\nCPU > 70% 自动扩缩'),
        (4.2, 5.5, 2, 2, 'order-service\nx2', '#E67E22', ':8001'),
        (6.6, 5.5, 2, 2, 'product-service\nx2', '#2ECC71', ':8002'),
        (9, 5.5, 2, 2, 'logistics-service\nx2', '#F39C12', ':8003'),
        (1.3, 2, 2.5, 2.5, 'MySQL 8.0\nPVC 5Gi', '#8E44AD', ':3306\nInnoDB, UTF8mb4'),
        (4.2, 2, 2, 2.5, 'Redis 7', '#D35400', ':6379\n会话共享'),
        (6.6, 2, 2, 2.5, 'Prometheus\n+ Grafana', '#C0392B', ':9091 / :3000'),
    ]
    for x, y, w, h, name, color, detail in services:
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15",
                              facecolor='white', edgecolor=color, linewidth=2)
        ax.add_patch(box)
        ax.text(x+w/2, y+h-0.4, name, ha='center', va='top', fontsize=10, fontweight='bold', color=color)
        ax.text(x+w/2, y+0.4, detail, ha='center', va='bottom', fontsize=8, color=C_TEXT)

    # Nginx LoadBalancer
    lb = FancyBboxPatch((3, 7.7), 5.5, 0.8, boxstyle="round,pad=0.15",
                         facecolor='#2ECC71', edgecolor='#27AE60', linewidth=2)
    ax.add_patch(lb)
    ax.text(5.75, 8.1, 'LoadBalancer :80 → campus-server × 2', ha='center', fontsize=11, fontweight='bold', color='white')

    # Jenkins
    jenkins_box = FancyBboxPatch((12, 5), 3.5, 4, boxstyle="round,pad=0.2",
                                  facecolor='white', edgecolor=C_BORDER, linewidth=2)
    ax.add_patch(jenkins_box)
    ax.text(13.75, 8.7, 'Jenkins CI/CD', ha='center', fontsize=12, fontweight='bold', color=C_BORDER)
    ax.text(13.75, 7.8, 'Git → Build → Test\n→ Eval → Package\n→ Docker Build → Push\n→ Deploy to K8s',
            ha='center', fontsize=9, color=C_TEXT, linespacing=1.5)

    ax.plot([11, 12], [6, 8], 'k-', linewidth=1, linestyle=':')
    ax.annotate('kubectl apply', xy=(12, 8), fontsize=8, color='#7F8C8D')

    ax.set_title('图1-3 Kubernetes 部署架构图', fontsize=14, fontweight='bold', color=C_BORDER, pad=15)
    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'deployment-diagram.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')


# ================================================================
# 图4: BPMN 流程图
# ================================================================
def draw_bpmn():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 9))
    fig.suptitle('图2-1 BPMN 业务流程图', fontsize=14, fontweight='bold', color=C_BORDER, y=0.98)
    for ax in [ax1, ax2]:
        ax.set_facecolor(C_BG)
        ax.axis('off')

    # 售后退款流程
    ax1.set_xlim(0, 16); ax1.set_ylim(0, 14)
    ax1.set_title('(a) 售后退款流程 (aftersale_refund.bpmn)', fontsize=12, fontweight='bold', color='#8E44AD')

    nodes_aftersale = [
        (7.5, 13.5, 'Start\n收到售后请求', 'event', None),
        (7.5, 11.5, 'Task_QueryOrder\n查询订单+物流', 'service', None),
        (7.5, 9.5, 'Task_ClassifyReason\n智能分类售后原因', 'service', '★创新①'),
        (7.5, 8, 'Gw_IsDeliveryIssue\n是否配送类问题?', 'gateway', None),
        (4, 6.5, 'Task_DeliveryPolicy\n查超时补偿政策(RAG)', 'service', None),
        (11, 6.5, 'Task_ProductPolicy\n查商品退款政策(RAG)', 'service', None),
        (4, 4.5, 'Gw_Amount\n金额≥100?', 'gateway', None),
        (1.5, 3, 'Task_ManualReview\n人工审核', 'user', None),
        (6.5, 3, 'Task_AutoRefund\n自动发起退款', 'service', None),
        (4, 1, 'Task_Notify\n汇总通知用户', 'service', None),
        (4, -0.5, 'End\n售后处理完成', 'event', None),
    ]

    colors = {'event': '#3498DB', 'service': '#2ECC71', 'gateway': '#F39C12', 'user': '#E74C3C'}
    shapes = {'event': 'circle', 'service': 'round', 'gateway': 'diamond', 'user': 'round'}

    for x, y, label, ntype, star in nodes_aftersale:
        c = colors[ntype]
        if ntype == 'gateway':
            diamond = Polygon([(x, y+0.8), (x+1.2, y), (x, y-0.8), (x-1.2, y)],
                              facecolor='white', edgecolor=c, linewidth=2)
            ax1.add_patch(diamond)
            ax1.text(x, y, label, ha='center', va='center', fontsize=7, color=C_TEXT)
        else:
            pad = 0.1 if ntype == 'service' else 0.2
            box = FancyBboxPatch((x-1.5, y-0.65), 3, 1.3, boxstyle=f"round,pad={pad}",
                                  facecolor='white', edgecolor=c, linewidth=2)
            ax1.add_patch(box)
            ax1.text(x, y, label, ha='center', va='center', fontsize=7, color=C_TEXT)
        if star:
            ax1.text(x+1.8, y+0.6, star, fontsize=8, color='red', fontweight='bold')

    # 连线
    arrows_aftersale = [
        (7.5, 13.2, 7.5, 11.8), (7.5, 11.2, 7.5, 9.8), (7.5, 9.2, 7.5, 8.8),
        (7.5, 7.3, 5.2, 6.8), (7.5, 7.3, 9.8, 6.8),
        (5.2, 6.2, 4, 5.3), (9.8, 6.2, 11, 5.3),
        (4, 3.8, 3, 3.3), (4, 3.8, 5.3, 3.3), (6.5, 3.8, 5.3, 3.3),
        (3, 2.7, 4, 1.3), (5.3, 2.7, 4, 1.3),
        (4, 0.7, 4, -0.2),
    ]
    for x1, y1, x2, y2 in arrows_aftersale:
        ax1.annotate('', xy=(x2, y2), xytext=(x1, y1),
                     arrowprops=dict(arrowstyle='->', lw=1.5, color='#7F8C8D'))

    # 标签
    ax1.text(6.2, 7.3, '是', fontsize=8, color='#27AE60')
    ax1.text(8.8, 7.3, '否', fontsize=8, color='#E74C3C')
    ax1.text(3.2, 3.8, '≥100', fontsize=8, color='#E74C3C')
    ax1.text(5.8, 3.8, '<100', fontsize=8, color='#27AE60')

    # 物流地图流程
    ax2.set_xlim(0, 16); ax2.set_ylim(0, 14)
    ax2.set_title('(b) 物流地图流程 (logistics_map.bpmn)', fontsize=12, fontweight='bold', color='#2980B9')

    nodes_logistics = [
        (7.5, 13.5, 'Start\n收到物流查询', 'event'),
        (7.5, 11.5, 'Task_QueryLogistics\n查询物流+路线数据', 'service'),
        (7.5, 9.5, 'Gw_HasMapData\n是否有地图数据?', 'gateway'),
        (11, 7, 'Task_BuildRouteMap\n构建配送路线地图', 'service'),
        (11, 5, 'End\n返回地图信息', 'event'),
        (4, 7, 'End\n返回基本信息', 'event'),
    ]

    for item in nodes_logistics:
        x, y, label, ntype = item
        c = colors[ntype]
        if ntype == 'gateway':
            diamond = Polygon([(x, y+0.8), (x+1.2, y), (x, y-0.8), (x-1.2, y)],
                              facecolor='white', edgecolor=c, linewidth=2)
            ax2.add_patch(diamond)
            ax2.text(x, y, label, ha='center', va='center', fontsize=7, color=C_TEXT)
        else:
            box = FancyBboxPatch((x-1.5, y-0.65), 3, 1.3, boxstyle="round,pad=0.2",
                                  facecolor='white', edgecolor=c, linewidth=2)
            ax2.add_patch(box)
            ax2.text(x, y, label, ha='center', va='center', fontsize=7, color=C_TEXT)

    # 连线
    arrows_logistics = [
        (7.5, 13.2, 7.5, 11.8), (7.5, 11.2, 7.5, 10.3),
        (7.5, 8.7, 9.8, 7.3), (7.5, 8.7, 5.5, 7.3),
        (9.8, 6.7, 11, 5.3), (5.3, 6.7, 4, 5),
    ]
    for x1, y1, x2, y2 in arrows_logistics:
        ax2.annotate('', xy=(x2, y2), xytext=(x1, y1),
                     arrowprops=dict(arrowstyle='->', lw=1.5, color='#7F8C8D'))

    ax2.text(9, 8.7, '有地图', fontsize=8, color='#27AE60')
    ax2.text(6, 8.7, '无地图', fontsize=8, color='#E74C3C')

    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'bpmn-diagram.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')


# ================================================================
# 图5: Jenkins CI/CD 流水线
# ================================================================
def draw_jenkins():
    fig, ax = plt.subplots(figsize=(20, 5))
    ax.set_xlim(0, 20); ax.set_ylim(0, 6)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    stages = [
        ('Checkout\n代码检出', '#3498DB', 'Git\n4重回退策略'),
        ('Build & Test\n编译+测试', '#2ECC71', 'Maven+JUnit5\n18 tests, 0 failures'),
        ('Static Analysis\n代码统计', '#1ABC9C', 'Shell\n2,882行/43文件'),
        ('Package\nMaven打包', '#9B59B6', 'Maven\n5 JAR包 (119MB)'),
        ('Evaluation\n离线评测', '#F39C12', 'API调用\n8用例→100%'),
        ('Docker Build\n镜像构建', '#E67E22', 'Docker\n4微服务镜像'),
        ('Docker Push\n推送仓库', '#D35400', 'Docker\nHarbor Registry'),
        ('Deploy to K8s\n部署上线', '#C0392B', 'kubectl\n滚动更新+自动回滚'),
    ]

    for i, (name, color, detail) in enumerate(stages):
        x = 0.5 + i * 2.4
        # Stage box
        box = FancyBboxPatch((x, 1.5), 2, 3, boxstyle="round,pad=0.2",
                              facecolor='white', edgecolor=color, linewidth=2.5)
        ax.add_patch(box)
        ax.text(x+1, 3.8, name, ha='center', va='top', fontsize=9, fontweight='bold', color=color)
        ax.text(x+1, 2.0, detail, ha='center', va='bottom', fontsize=7, color=C_TEXT)

        # Arrow
        if i < len(stages) - 1:
            ax.annotate('', xy=(x+2.3, 3), xytext=(x+2.2, 3),
                        arrowprops=dict(arrowstyle='->', lw=2, color='#95A5A6'))

    ax.text(10, 5.2, 'Jenkins CI/CD Pipeline (8阶段)', ha='center', fontsize=14, fontweight='bold', color=C_BORDER)
    ax.text(10, 4.7, '参数化构建: DEPLOY_ENV(dev/staging/prod) | RUN_EVAL | SKIP_DEPLOY | SKIP_TESTS | DOCKER_TAG_OVERRIDE',
            ha='center', fontsize=9, color='#7F8C8D')

    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'jenkins-pipeline-diagram.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')


# ================================================================
# 图6: SOA 服务架构图
# ================================================================
def draw_soa():
    fig, ax = plt.subplots(figsize=(16, 8))
    ax.set_xlim(0, 16); ax.set_ylim(0, 10)
    ax.axis('off')
    ax.set_facecolor(C_BG)

    # Nginx LB
    ng = FancyBboxPatch((4, 8.5), 8, 1, boxstyle="round,pad=0.15",
                         facecolor='#2ECC71', edgecolor='#27AE60', linewidth=2)
    ax.add_patch(ng)
    ax.text(8, 9, 'Nginx :80 (反向代理 + 负载均衡, upstream weight=3)', ha='center', fontsize=11, fontweight='bold', color='white')

    # campus-server x2
    for i, offset in enumerate([-3, 1]):
        cs = FancyBboxPatch((5+offset, 6), 3, 1.5, boxstyle="round,pad=0.15",
                              facecolor='white', edgecolor=C_APP, linewidth=2)
        ax.add_patch(cs)
        ax.text(6.5+offset, 7.2, f'campus-server-{i+1}\n:8000', ha='center', fontsize=10, fontweight='bold', color=C_APP)
        ax.text(6.5+offset, 6.4, 'AgentOrchestrator\nBpmnEngine + RagService', ha='center', fontsize=7, color=C_TEXT)

    # 微服务
    ms_colors = [('#E67E22', '#D35400'), ('#2ECC71', '#27AE60'), ('#F39C12', '#E67E22')]
    ms_names = [('order-service\n:8001', '订单CRUD\n退款处理'), ('product-service\n:8002', '商品搜索\n上下架'), ('logistics-service\n:8003', '物流查询\n路线地图')]
    for i, ((fc, ec), (name, desc)) in enumerate(zip(ms_colors, ms_names)):
        ms = FancyBboxPatch((1+i*5, 2.5), 4, 2, boxstyle="round,pad=0.15",
                              facecolor='white', edgecolor=ec, linewidth=2)
        ax.add_patch(ms)
        ax.text(3+i*5, 3.8, name, ha='center', fontsize=10, fontweight='bold', color=ec)
        ax.text(3+i*5, 3.0, desc, ha='center', fontsize=8, color=C_TEXT)

    # 连接线
    for i in range(3):
        ax.annotate('', xy=(3+i*5, 4.5), xytext=(7, 6),
                     arrowprops=dict(arrowstyle='->', lw=1, color='#BDC3C7', linestyle=':'))

    # MySQL + Redis
    infra = FancyBboxPatch((5, 0.5), 6, 1.3, boxstyle="round,pad=0.1",
                            facecolor=C_INFRA, edgecolor='#2C3E50', linewidth=2)
    ax.add_patch(infra)
    ax.text(8, 1.4, 'MySQL 8.0 :3306  |  Redis 7 :6379  |  Resilience4j 断路器+重试', ha='center', fontsize=10, color='white')

    ax.set_title('图1-4 SOA 面向服务架构图', fontsize=14, fontweight='bold', color=C_BORDER, pad=15)
    plt.tight_layout()
    path = os.path.join(OUT_DIR, 'soa-architecture.png')
    fig.savefig(path, dpi=150, bbox_inches='tight', facecolor=C_BG)
    plt.close()
    print(f'[OK] {path}')


# ================================================================
# 主入口
# ================================================================
if __name__ == '__main__':
    print('开始生成课程报告图表...\n')
    draw_usecase()
    draw_architecture()
    draw_deployment()
    draw_soa()
    draw_bpmn()
    draw_jenkins()
    print(f'\n全部图表已保存到: {OUT_DIR}')
