#!/bin/bash
# ============================================================================
# SLA 服务质量评价脚本
# 对运行中的系统进行压测并生成服务质量报告
# 用法: bash scripts/sla_evaluate.sh [BASE_URL]
# ============================================================================

BASE_URL=${1:-http://localhost}
REPORT_FILE="sla_report_$(date +%Y%m%d_%H%M%S).json"
ITERATIONS=50

echo "=============================================="
echo "  SLA 服务质量评价 — 校园电商/外卖智能服务平台"
echo "  目标: ${BASE_URL}"
echo "  迭代次数: ${ITERATIONS}"
echo "=============================================="

# 测试用例
declare -A TEST_CASES=(
  ["health"]="/api/health"
  ["evaluate"]="/api/evaluate"
  ["products"]="/api/products"
  ["rider_available"]="/api/rider/available"
)

TOTAL=0
PASSED=0
declare -A LATENCIES
declare -A ERRORS

# ── 1. 可用性测试 ──
echo ""
echo ">>> 1. 可用性测试"

for name in "${!TEST_CASES[@]}"; do
  url="${TEST_CASES[$name]}"
  echo -n "  GET ${url} ... "
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${url}" 2>/dev/null)
  if [ "${HTTP_CODE}" = "200" ]; then
    echo "✅ ${HTTP_CODE}"
    ((PASSED++))
  else
    echo "❌ ${HTTP_CODE}"
  fi
  ((TOTAL++))
done

# ── 2. 响应时间测试 (多次采样) ──
echo ""
echo ">>> 2. 响应时间基准测试 (${ITERATIONS} 次采样)"

CHAT_URL="${BASE_URL}/api/chat"
CHAT_BODY='{"message":"蓝牙耳机多少钱"}'
TIMES=()

for i in $(seq 1 ${ITERATIONS}); do
  START=$(python3 -c "import time; print(int(time.time()*1000))" 2>/dev/null || echo 0)
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${CHAT_URL}" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "${CHAT_BODY}" 2>/dev/null)
  END=$(python3 -c "import time; print(int(time.time()*1000))" 2>/dev/null || echo 0)

  if [ "$START" != "0" ] && [ "$END" != "0" ]; then
    ELAPSED=$((END - START))
    TIMES+=($ELAPSED)
    if [ "${CODE}" = "200" ]; then
      echo -n "."
      ((PASSED++))
    else
      echo -n "x"
    fi
  fi
  ((TOTAL++))
done
echo ""

# ── 3. 统计计算 ──
if [ ${#TIMES[@]} -gt 0 ]; then
  # 排序
  SORTED=($(printf '%s\n' "${TIMES[@]}" | sort -n))

  MIN=${SORTED[0]}
  MAX=${SORTED[-1]}

  # 平均值
  SUM=0
  for t in "${TIMES[@]}"; do SUM=$((SUM + t)); done
  AVG=$((SUM / ${#TIMES[@]}))

  # P50/P95/P99
  P50_IDX=$(( ${#SORTED[@]} * 50 / 100 ))
  P95_IDX=$(( ${#SORTED[@]} * 95 / 100 ))
  P99_IDX=$(( ${#SORTED[@]} * 99 / 100 ))
  P50=${SORTED[$P50_IDX]}
  P95=${SORTED[$P95_IDX]}
  P99=${SORTED[$P99_IDX]}

  AVAILABILITY=$(python3 -c "print(round(${PASSED}/${TOTAL}*100, 2))" 2>/dev/null || echo "N/A")

  echo ""
  echo "=============================================="
  echo "  SLA 评价结果"
  echo "=============================================="
  echo "  服务端点总数:    ${#TEST_CASES[@]}"
  echo "  采样总数:        ${TOTAL}"
  echo "  成功:            ${PASSED}"
  echo ""
  echo "  ── 可用性 ──"
  echo "  可用性:          ${AVAILABILITY}%"
  echo ""
  echo "  ── 响应时间 ──"
  echo "  平均 (avg):      ${AVG}ms"
  echo "  最小 (min):      ${MIN}ms"
  echo "  最大 (max):      ${MAX}ms"
  echo "  P50:             ${P50}ms"
  echo "  P95:             ${P95}ms"
  echo "  P99:             ${P99}ms"
  echo ""
  echo "  ── SLA 评级 ──"

  # 可用性评级
  if python3 -c "exit(0 if ${AVAILABILITY} >= 99.9 else 1)" 2>/dev/null; then
    AVAIL_GRADE="A+ (3个9以上)"
  elif python3 -c "exit(0 if ${AVAILABILITY} >= 99.0 else 1)" 2>/dev/null; then
    AVAIL_GRADE="A (2个9)"
  elif python3 -c "exit(0 if ${AVAILABILITY} >= 95.0 else 1)" 2>/dev/null; then
    AVAIL_GRADE="B (95%+)"
  else
    AVAIL_GRADE="C (需改进)"
  fi

  # 响应时间评级
  if [ ${AVG} -lt 100 ]; then
    LAT_GRADE="A+ (优秀, <100ms)"
  elif [ ${AVG} -lt 300 ]; then
    LAT_GRADE="A (良好, <300ms)"
  elif [ ${AVG} -lt 1000 ]; then
    LAT_GRADE="B (可接受, <1s)"
  elif [ ${AVG} -lt 3000 ]; then
    LAT_GRADE="C (需优化, <3s)"
  else
    LAT_GRADE="D (性能瓶颈)"
  fi

  echo "  可用性评级:      ${AVAIL_GRADE}"
  echo "  响应时间评级:    ${LAT_GRADE}"
  echo ""

  # 综合评分
  if [ "${AVAIL_GRADE:0:1}" = "A" ] && [ "${LAT_GRADE:0:1}" = "A" ]; then
    echo "  ★★★ 综合评分: 优秀"
  elif [ "${AVAIL_GRADE:0:1}" = "A" ] || [ "${LAT_GRADE:0:1}" = "A" ]; then
    echo "  ★★☆ 综合评分: 良好"
  else
    echo "  ★☆☆ 综合评分: 及格"
  fi
  echo "=============================================="

  # ── 4. 生成 JSON 报告 ──
  cat > "${REPORT_FILE}" <<EOF
{
  "report_time": "$(date -Iseconds)",
  "service": "校园电商/外卖智能服务平台 - Java 重构版",
  "test_config": {
    "base_url": "${BASE_URL}",
    "iterations": ${ITERATIONS}
  },
  "availability": {
    "total_requests": ${TOTAL},
    "successful": ${PASSED},
    "failed": $((TOTAL - PASSED)),
    "availability_pct": ${AVAILABILITY},
    "grade": "${AVAIL_GRADE}"
  },
  "latency": {
    "avg_ms": ${AVG},
    "min_ms": ${MIN},
    "max_ms": ${MAX},
    "p50_ms": ${P50},
    "p95_ms": ${P95},
    "p99_ms": ${P99},
    "grade": "${LAT_GRADE}"
  }
}
EOF
  echo "报告已保存: ${REPORT_FILE}"
fi
