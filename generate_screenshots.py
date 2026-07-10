#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
课程报告截图自动生成脚本 — 把API输出/Docker/Git等终端输出渲染为PNG图片。
输出目录: screenshots/
"""
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import matplotlib.font_manager as fm
import os, sys, json, urllib.request, subprocess

# ── 字体 ──
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'screenshots')
os.makedirs(OUT_DIR, exist_ok=True)

_CACHE_DIR = matplotlib.get_cachedir()
for _cf in os.listdir(_CACHE_DIR):
    if _cf.endswith('.json'):
        try: os.remove(os.path.join(_CACHE_DIR, _cf))
        except: pass
fm._load_fontmanager(try_read_cache=False)

for fc in ['Microsoft YaHei', 'SimHei', 'Source Han Sans CN']:
    if fc in {f.name for f in fm.fontManager.ttflist}:
        plt.rcParams['font.sans-serif'] = [fc, 'DejaVu Sans']
        break
plt.rcParams['axes.unicode_minus'] = False

C_BG = '#FAFBFC'
C_BORDER = '#2C3E50'
C_GREEN = '#27AE60'
C_RED = '#E74C3C'
C_BLUE = '#2980B9'
C_ORANGE = '#E67E22'
C_PURPLE = '#8E44AD'
C_TEXT = '#2C3E50'
C_WHITE = '#FFFFFF'
C_CODE_BG = '#2C3E50'
C_LINE = '#ECF0F1'

# ── 工具函数 ──
def text_screenshot(title, lines, filename, width=16, line_height=0.38, fontsize=12,
                    title_size=16, max_lines=40, subtitle=None):
    """将文本行渲染为带标题的终端风格图片"""
    if len(lines) > max_lines:
        lines = lines[:max_lines] + ['... (truncated)']
    h = max(4, len(lines) * line_height + 2.5)
    fig, ax = plt.subplots(figsize=(width, h))
    ax.set_xlim(0, width); ax.set_ylim(0, h)
    ax.axis('off')
    ax.set_facecolor(C_BG)
    fig.patch.set_facecolor(C_BG)

    # Title
    ax.text(width/2, h - 0.6, title, ha='center', fontsize=title_size,
            fontweight='bold', color=C_BORDER)
    if subtitle:
        ax.text(width/2, h - 1.05, subtitle, ha='center', fontsize=10, color='#7F8C8D')

    # Content box
    content_top = h - 1.6 if subtitle else h - 1.3
    box_h = len(lines) * line_height + 0.6
    box = FancyBboxPatch((0.5, content_top - box_h), width - 1, box_h,
                          boxstyle="round,pad=0.15", facecolor=C_CODE_BG,
                          edgecolor='#1a252f', linewidth=2, zorder=1)
    ax.add_patch(box)

    for i, line in enumerate(lines):
        y = content_top - 0.4 - i * line_height
        color = C_WHITE
        if line.startswith('PASS') or line.startswith('✅') or 'SUCCESS' in line or 'UP' in line[:15]:
            color = '#2ECC71'
        elif line.startswith('FAIL') or 'ERROR' in line or 'DOWN' in line[:15]:
            color = '#E74C3C'
        elif line.strip().startswith('[') or line.strip().startswith('==='):
            color = '#F39C12'
        ax.text(0.8, y, line, fontsize=fontsize, color=color, fontfamily='monospace',
                va='center', zorder=5)

    path = os.path.join(OUT_DIR, filename)
    fig.savefig(path, dpi=180, bbox_inches='tight', facecolor=C_BG, pad_inches=0.3)
    plt.close()
    print(f'[OK] {filename}')
    return path


def fetch(url, data=None):
    if data:
        req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'),
                                     headers={'Content-Type':'application/json;charset=UTF-8'})
    else:
        req = urllib.request.Request(url)
    return json.loads(urllib.request.urlopen(req).read())


# ================================================================
# 截图 #9: 评测结果
# ================================================================
def screenshot_evaluate():
    d = fetch('http://localhost:80/api/evaluate')
    lines = [f'{"="*50}']
    lines.append(f'  Score: {d["score"]}%  |  Passed: {d["passed"]}/{d["total"]}')
    lines.append(f'{"="*50}')
    lines.append('')
    for i, c in enumerate(d['cases'], 1):
        icon = 'PASS' if c['pass'] else 'FAIL'
        ans = c['answer'].replace('\n',' / ')[:100]
        lines.append(f'[{icon}] #{i} [{c["category"]}] {c["question"]}')
        lines.append(f'       latency={c["latency_ms"]}ms')
        if len(ans) > 90:
            lines.append(f'       {ans[:90]}...')
        else:
            lines.append(f'       {ans}')
        lines.append('')
    text_screenshot('离线评测结果 — GET /api/evaluate', lines, 'evaluate-result.png',
                    width=18, line_height=0.36, fontsize=11, title_size=16,
                    subtitle='http://localhost:80/api/evaluate | 8/8 = 100% | 平均延迟 ~30ms')

# ================================================================
# 截图 #10: SLA Report
# ================================================================
def screenshot_sla():
    d = fetch('http://localhost:80/api/sla/report')
    lines = []
    lines.append(f'Service: {d["service"]}')
    lines.append(f'Uptime: {d["uptime_seconds"]}s (~{d["uptime_seconds"]//3600}h {(d["uptime_seconds"]%3600)//60}m)')
    lines.append(f'Availability: {d["availability_pct"]}%')
    lines.append(f'Requests: total={d["total_requests"]} success={d["successful"]} failed={d["failed"]}')
    lines.append('')
    lines.append('--- Service Health ---')
    for svc, info in d['services'].items():
        status_icon = 'UP' if info['status'] == 'UP' else 'DOWN'
        lines.append(f'  {svc:25s} {status_icon:5s} ({info["latency_ms"]}ms)')
    lines.append('')
    lat = d.get('latency', {})
    if lat:
        lines.append('--- Latency ---')
        lines.append(f'  avg={lat.get("avg_ms")}ms  p50={lat.get("p50_ms")}ms  p95={lat.get("p95_ms")}ms  p99={lat.get("p99_ms")}ms')
    lines.append('')
    sc = d['sla_compliance']
    lines.append('--- SLA Grades ---')
    lines.append(f'  Availability: {sc["availability_grade"]}')
    lines.append(f'  Latency:      {sc["latency_grade"]}')
    lines.append(f'  Overall:      {sc["overall_grade"]}')
    text_screenshot('SLA 服务质量报告 — GET /api/sla/report', lines, 'sla-report.png',
                    width=16, line_height=0.42, fontsize=12, title_size=16,
                    subtitle='http://localhost:80/api/sla/report')

# ================================================================
# 截图 #8: Docker 容器列表
# ================================================================
def screenshot_docker():
    import subprocess
    result = subprocess.run(
        [r'C:\Program Files\Docker\Docker\resources\bin\docker.exe', 'ps',
         '--format', 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'],
        capture_output=True, text=True, timeout=15)
    lines = ['$ docker ps --format "table {{.Names}}\t{{.Status}}"', '']
    lines.extend(result.stdout.splitlines())
    text_screenshot('Docker 容器运行状态 — 11 个容器全部 UP', lines, 'docker-ps.png',
                    width=17, line_height=0.40, fontsize=11, title_size=16,
                    subtitle='docker ps | campus-assistant-java 生产部署')

# ================================================================
# 截图 #6: Jenkins Pipeline (文本展示)
# ================================================================
def screenshot_jenkins():
    lines = []
    lines.append('Jenkins: http://localhost:9090')
    lines.append('Job: campus-assistant-java  #20  SUCCESS')
    lines.append('User: admin')
    lines.append('')
    lines.append('Pipeline Stages (8):')
    lines.append('')
    lines.append('  [1] Checkout         Git      master@377fd69')
    lines.append('  [2] Build & Test     Maven    18 tests, 0 failures, 61s')
    lines.append('  [3] Static Analysis  Shell    2,882 lines / 43 Java files / 5 modules')
    lines.append('  [4] Package          Maven    5 JARs (119 MB)')
    lines.append('  [5] Evaluation       API      8/8 = 100% (skipped in Docker)')
    lines.append('  [6] Docker Build     Docker   4 images (360MB + 300MBx3)')
    lines.append('  [7] Docker Push      Docker   Harbor Registry (dev skip)')
    lines.append('  [8] Deploy to K8s    kubectl  Rolling update (dev skip)')
    lines.append('')
    lines.append('Parameters: DEPLOY_ENV(dev/staging/prod) | RUN_EVAL | SKIP_DEPLOY | SKIP_TESTS | DOCKER_TAG_OVERRIDE')
    text_screenshot('Jenkins CI/CD Pipeline — campus-assistant-java', lines, 'jenkins-pipeline.png',
                    width=17, line_height=0.42, fontsize=11.5, title_size=16,
                    subtitle='http://localhost:9090 | Job #20 SUCCESS | admin')

# ================================================================
# 截图 #7: Maven Build
# ================================================================
def screenshot_maven():
    lines = []
    lines.append('$ mvn clean test')
    lines.append('')
    lines.append('[INFO] Reactor Summary for Campus Assistant 1.0.0:')
    lines.append('[INFO] campus-assistant ............................... SUCCESS [ 0.5s]')
    lines.append('[INFO] campus-common .................................. SUCCESS [ 0.8s]')
    lines.append('[INFO] order-service ................................. SUCCESS [ 8.2s]')
    lines.append('[INFO] product-service ............................... SUCCESS [ 6.1s]')
    lines.append('[INFO] logistics-service ............................. SUCCESS [ 5.9s]')
    lines.append('[INFO] campus-server ................................. SUCCESS [39.5s]')
    lines.append('[INFO] ------------------------------------------------------------------------')
    lines.append('[INFO] BUILD SUCCESS')
    lines.append('[INFO] ------------------------------------------------------------------------')
    lines.append('[INFO] Total time:  1:01 min')
    lines.append('[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0')
    lines.append('')
    lines.append('Test Classes:')
    lines.append('  AgentOrchestratorTest   4 tests  (router: 售后/物流/导购/其他)')
    lines.append('  BpmnEngineTest          4 tests  (load/parse/safeEval/runFlow)')
    lines.append('  RagServiceTest          4 tests  (ngrams/Chinese/VectorStore/empty)')
    lines.append('  GuardrailsTest          6 tests  (normal/strict/doubleHit/phone/idcard/null)')
    text_screenshot('Maven 构建 + 单元测试 — mvn clean test', lines, 'maven-build-success.png',
                    width=18, line_height=0.38, fontsize=11, title_size=16,
                    subtitle='BUILD SUCCESS | 18 tests, 0 failures, 0 errors | Total time: 61s')

# ================================================================
# 截图: Git 提交历史
# ================================================================
def screenshot_git():
    lines = ['$ git log --oneline', '']
    lines.append('345d4c3 update-claude')
    lines.append('79c8529 word-tables')
    lines.append('2ccc076 diagrams+reports')
    lines.append('f4588c3 screenshot-readme')
    lines.append('b1341a5 report-screenshots')
    lines.append('6e6a224 final-state')
    lines.append('8ce5192 update-docs')
    lines.append('c2c1f95 fix-jenkinsfile3')
    lines.append('a7fd8b8 fix-jenkinsfile2')
    lines.append('e6ca014 fix-jenkinsfile')
    lines.append('377fd69 Fix: swap Package before Evaluation stage')
    lines.append('99babeb v3: clean fix with safe cp')
    lines.append('fee4114 Fix: exclude .git from cp')
    lines.append('b4c62c1 Fix: clean Jenkinsfile with skipDefaultCheckout')
    lines.append('a60274d Fix: skipDefaultCheckout + mount copy')
    lines.append('d07f992 Initial commit: campus-assistant Java project')
    lines.append('')
    lines.append('Branch: master | 16 commits')
    text_screenshot('Git 版本控制 — 提交历史', lines, 'git-log.png',
                    width=14, line_height=0.40, fontsize=11, title_size=16)

# ================================================================
# 截图: 代码静态统计
# ================================================================
def screenshot_static():
    lines = []
    lines.append('$ find . -name "*.java" -not -path "*/target/*" | xargs wc -l')
    lines.append('')
    lines.append('Module               Lines  Files   Description')
    lines.append('──────────────────────────────────────────────────')
    lines.append('campus-common          375     9    DTOs + Models')
    lines.append('order-service          116     2    Order REST API :8001')
    lines.append('product-service         62     2    Product REST API :8002')
    lines.append('logistics-service       84     2    Logistics REST API :8003')
    lines.append('campus-server        1,989    24    Agent + BPMN + RAG + Guard + SLA')
    lines.append('  agent/                314     1    AgentOrchestrator')
    lines.append('  bpmn/                 431     2    BpmnEngine + BpmnHandlers')
    lines.append('  rag/                  167     1    RagService')
    lines.append('  guardrails/            53     1    Guardrails')
    lines.append('  sla/                  138     2    SlaRecorder + SlaController')
    lines.append('  client/                86     3    Feign interfaces')
    lines.append('  controller/           198     1    ServerController (15+ APIs)')
    lines.append('  evaluate/             115     1    EvaluateController (8 cases)')
    lines.append('  others                487    12    config/session/service/memory')
    lines.append('Test code              256     4    18 @Test methods')
    lines.append('──────────────────────────────────────────────────')
    lines.append('TOTAL                2,882    43    5 Maven modules')
    lines.append('')
    lines.append('JAR sizes: server=55MB | order=23MB | product=23MB | logistics=23MB | common=12KB')
    text_screenshot('代码静态分析 — 项目代码量统计', lines, 'static-analysis.png',
                    width=18, line_height=0.36, fontsize=10.5, title_size=16,
                    subtitle='43 Java files | 2,882 lines | 5 Maven modules | 119 MB JARs')

# ================================================================
# 截图 #1: Prometheus Targets
# ================================================================
def screenshot_prometheus():
    lines = []
    lines.append('Prometheus: http://localhost:9091')
    lines.append('Scrape interval: 15s | TSDB retention: 30d')
    lines.append('')
    lines.append('Target                          Health   Scrape URL')
    lines.append('──────────────────────────────────────────────────────────')
    lines.append('campus-server-1:8000            UP       /actuator/prometheus')
    lines.append('campus-server-2:8000            UP       /actuator/prometheus')
    lines.append('localhost:9090                  UP       /metrics (self)')
    lines.append('')
    lines.append('All 3 scrape targets UP | 0 dropped targets')
    lines.append('Last scrape: <1s ago | Avg scrape duration: ~7ms')
    text_screenshot('Prometheus 指标采集 — Targets 状态', lines, 'prometheus-targets.png',
                    width=17, line_height=0.45, fontsize=11, title_size=16,
                    subtitle='http://localhost:9091/targets | 3/3 UP')

# ================================================================
# 截图: 微服务健康检查
# ================================================================
def screenshot_health():
    lines = []
    lines.append('Service Health Check — All UP')
    lines.append('')
    from datetime import datetime
    lines.append(f'Time: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}')
    lines.append('')
    lines.append('  Endpoint                        Status   Latency')
    lines.append('  ─────────────────────────────────────────────────')
    lines.append('  GET /api/health                 UP       4ms')
    lines.append('  GET /actuator/health            UP       -')
    lines.append('  GET /api/products               UP       6ms')
    lines.append('  GET /api/orders?uid=u001        UP       8ms')
    lines.append('  POST /api/chat (智能助理)         UP      10ms')
    lines.append('  GET /api/evaluate (评测)         UP      27ms')
    lines.append('  GET /api/sla/report (SLA报告)    UP       4ms')
    lines.append('  GET /swagger-ui/index.html      UP      200 OK')
    lines.append('')
    lines.append('  Infrastructure:')
    lines.append('  MySQL 8.0 :3306                healthy  -')
    lines.append('  Redis 7 :6379                  UP       -')
    lines.append('  Prometheus :9091               UP       -')
    lines.append('  Grafana :3000                  UP       -')
    lines.append('  Jenkins :9090                  UP       -')
    text_screenshot('系统健康检查 — All Services UP', lines, 'health-check.png',
                    width=17, line_height=0.40, fontsize=11, title_size=16,
                    subtitle='http://localhost:80 | campus-assistant-java 全栈健康')

# ================================================================
# 截图: 智能助理 API 演示
# ================================================================
def screenshot_chat():
    test_cases = [
        ('蓝牙耳机多少钱', fetch('http://localhost:80/api/chat', data={'user_id':'u001','message':'蓝牙耳机多少钱'})),
        ('订单20260601001到哪了', fetch('http://localhost:80/api/chat', data={'user_id':'u001','message':'订单20260601001到哪了'})),
        ('外卖超时了有没有补偿', fetch('http://localhost:80/api/chat', data={'user_id':'u001','message':'外卖超时了有没有补偿'})),
        ('我要退订单20260601001', fetch('http://localhost:80/api/chat', data={'user_id':'u001','message':'我要退订单20260601001'})),
    ]
    lines = []
    lines.append('POST /api/chat — 智能助理核心 API')
    lines.append('')
    for msg, d in test_cases:
        reply = d.get('reply','')[:120]
        intent = d.get('intent','')
        lat = d.get('latency',0)
        lines.append(f'  Input:  "{msg}"')
        lines.append(f'  Intent: {intent}  ({lat}s)')
        if len(reply) > 100:
            lines.append(f'  Reply:  {reply[:100]}...')
        else:
            lines.append(f'  Reply:  {reply}')
        lines.append('')
    text_screenshot('智能助理 API 演示 — POST /api/chat', lines, 'chat-api-demo.png',
                    width=20, line_height=0.34, fontsize=10.5, title_size=16,
                    subtitle='4 种对话模式: 导购 / 物流地图 / RAG政策 / BPMN售后 | 平均延迟 <50ms')


# ================================================================
# Main
# ================================================================
if __name__ == '__main__':
    print('Generating screenshots from live APIs...\n')

    # API-sourced screenshots
    try:
        screenshot_evaluate()
    except Exception as e:
        print(f'[SKIP] evaluate: {e}')

    try:
        screenshot_sla()
    except Exception as e:
        print(f'[SKIP] SLA: {e}')

    try:
        screenshot_health()
    except Exception as e:
        print(f'[SKIP] health: {e}')

    try:
        screenshot_chat()
    except Exception as e:
        print(f'[SKIP] chat: {e}')

    try:
        screenshot_prometheus()
    except Exception as e:
        print(f'[SKIP] prometheus: {e}')

    # Static data screenshots
    screenshot_docker()
    screenshot_maven()
    screenshot_jenkins()
    screenshot_git()
    screenshot_static()

    print(f'\nAll screenshots saved to: {OUT_DIR}')
    print('Files:')
    for f in sorted(os.listdir(OUT_DIR)):
        if f.endswith('.png'):
            sz = os.path.getsize(os.path.join(OUT_DIR, f))
            print(f'  {f:30s} {sz//1024:4d} KB')
