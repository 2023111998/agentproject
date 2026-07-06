package com.campus.server.bpmn;

import javax.xml.stream.*;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/**
 * 极简 BPMN 执行引擎 — 解析 .bpmn XML 文件并按流程执行。
 * 对应 Python 版 bpmn_engine.py
 *
 * 支持：开始事件→任务→网关(条件分支)→结束事件
 * 每个"任务"绑定一个处理器(handler)，由 BPMN 节点上的 camunda:delegateExpression 指定。
 */
public class BpmnEngine {

    /** BPMN 节点 */
    public record BpmnNode(String id, String type, String name, String impl) {}

    /** 顺序流 */
    public record SequenceFlow(String id, String src, String tgt, String name, String cond) {}

    /** 解析结果 */
    public record BpmnModel(Map<String, BpmnNode> nodes, List<SequenceFlow> flows, String startId) {}

    /**
     * 从 classpath 加载并解析 .bpmn 文件。
     */
    public static BpmnModel load(String classpathResource) throws Exception {
        InputStream is = BpmnEngine.class.getClassLoader().getResourceAsStream(classpathResource);
        if (is == null) throw new IllegalArgumentException("BPMN 文件未找到: " + classpathResource);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(is);

        Map<String, BpmnNode> nodes = new LinkedHashMap<>();
        List<SequenceFlow> flows = new ArrayList<>();
        String startId = null;

        boolean inProcess = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String local = reader.getLocalName();
                if ("process".equals(local)) {
                    inProcess = true;
                    continue;
                }
                if (!inProcess) continue;

                // 节点
                if (Set.of("startEvent", "endEvent", "task", "serviceTask", "userTask", "exclusiveGateway")
                        .contains(local)) {
                    String nid = reader.getAttributeValue(null, "id");
                    String name = reader.getAttributeValue(null, "name");
                    if (name == null) name = nid;

                    // 读取 camunda:delegateExpression / class / expression
                    String impl = reader.getAttributeValue(null, "delegateExpression");
                    if (impl == null) impl = reader.getAttributeValue(null, "class");
                    if (impl == null) impl = reader.getAttributeValue(null, "expression");
                    if (impl != null) impl = impl.strip().replace("${", "").replace("}", "").trim();

                    nodes.put(nid, new BpmnNode(nid, typeName(local), name, impl));
                    if ("startEvent".equals(local)) startId = nid;
                }

                // 顺序流
                if ("sequenceFlow".equals(local)) {
                    String fid = reader.getAttributeValue(null, "id");
                    String src = reader.getAttributeValue(null, "sourceRef");
                    String tgt = reader.getAttributeValue(null, "targetRef");
                    String fname = reader.getAttributeValue(null, "name");
                    flows.add(new SequenceFlow(fid, src, tgt, fname, null));
                }

                // 条件表达式（在 sequenceFlow 内部）
                if ("conditionExpression".equals(local)) {
                    String cond = reader.getElementText();
                    if (cond != null && !flows.isEmpty()) {
                        SequenceFlow last = flows.get(flows.size() - 1);
                        String cleaned = cond.strip().replace("${", "").replace("}", "").trim();
                        flows.set(flows.size() - 1,
                            new SequenceFlow(last.id, last.src, last.tgt, last.name, cleaned));
                    }
                }
            }
            if (event == XMLStreamConstants.END_ELEMENT && "process".equals(reader.getLocalName())) {
                inProcess = false;
            }
        }
        reader.close();

        if (startId == null) throw new IllegalArgumentException("BPMN 文件中未找到 startEvent");
        return new BpmnModel(nodes, flows, startId);
    }

    /** 将 BPMN 标签名转换为类型名 */
    private static String typeName(String local) {
        return switch (local) {
            case "startEvent" -> "startEvent";
            case "endEvent" -> "endEvent";
            case "task", "serviceTask" -> "serviceTask";
            case "userTask" -> "userTask";
            case "exclusiveGateway" -> "exclusiveGateway";
            default -> local;
        };
    }

    /**
     * 按 BPMN 流程执行。
     *
     * @param flowPath  classpath 下的 BPMN 文件路径（如 "flows/aftersale_refund.bpmn"）
     * @param handlers  实现名→处理函数的映射（key 对应 delegateExpression 的值）
     * @param ctx       上下文数据（可在条件表达式中引用）
     * @param log       步骤日志追加器
     * @return          执行结束后的上下文
     */
    public static Map<String, Object> run(String flowPath,
                                           Map<String, Function<Map<String, Object>, String>> handlers,
                                           Map<String, Object> ctx,
                                           StringBuilder log) throws Exception {
        return run(flowPath, handlers, ctx, log, 50);
    }

    public static Map<String, Object> run(String flowPath,
                                           Map<String, Function<Map<String, Object>, String>> handlers,
                                           Map<String, Object> ctx,
                                           StringBuilder log,
                                           int maxSteps) throws Exception {
        BpmnModel model = load(flowPath);
        Map<String, BpmnNode> nodes = model.nodes;
        List<SequenceFlow> flows = model.flows;

        // 从 Start 后面第一个节点开始
        String cur = startNodeAfter(model);
        if (cur == null) return ctx;

        for (int step = 0; step < maxSteps; step++) {
            BpmnNode nd = nodes.get(cur);
            if (nd == null) {
                log.append("[BPMN] 警告: 节点 ").append(cur).append(" 不存在\n");
                break;
            }
            String type = nd.type;

            if ("endEvent".equals(type)) {
                log.append("■ 结束:").append(nd.name).append("\n");
                return ctx;
            }

            if (Set.of("serviceTask", "userTask").contains(type)) {
                // 查找处理器：优先按 impl，其次按节点 id，再按节点名称
                Function<Map<String, Object>, String> handler = null;
                if (nd.impl != null) handler = handlers.get(nd.impl);
                if (handler == null) handler = handlers.get(cur);
                if (handler == null) handler = handlers.get(nd.name);

                String result = handler != null ? handler.apply(ctx) : "(节点未配置实现,跳过)";
                String tag = nd.impl != null ? "〔impl=" + nd.impl + "〕" : "";
                log.append("任务「").append(nd.name).append("」").append(tag).append("→ ").append(result).append("\n");

                cur = nextNode(flows, cur);
                if (cur == null) break;
            }
            else if ("exclusiveGateway".equals(type)) {
                List<SequenceFlow> outs = outgoing(flows, cur);
                SequenceFlow chosen = null;
                SequenceFlow defaultFlow = null;

                for (SequenceFlow f : outs) {
                    if (f.cond == null || f.cond.isEmpty()) {
                        defaultFlow = f;
                        continue;
                    }
                    if (safeEval(f.cond, ctx)) {
                        chosen = f;
                        break;
                    }
                }
                if (chosen == null) chosen = defaultFlow;
                if (chosen == null && !outs.isEmpty()) chosen = outs.get(0);

                log.append("网关「").append(nd.name).append("」→ 选择分支「")
                   .append(chosen != null ? (chosen.name != null ? chosen.name : "默认") : "无")
                   .append("」\n");

                cur = chosen != null ? chosen.tgt : null;
                if (cur == null) break;
            }
        }
        return ctx;
    }

    /** 获取 startEvent 后的第一个节点 */
    private static String startNodeAfter(BpmnModel model) {
        for (SequenceFlow f : model.flows) {
            BpmnNode src = model.nodes.get(f.src);
            if (src != null && "startEvent".equals(src.type)) return f.tgt;
        }
        return null;
    }

    /** 获取当前节点后的下一个节点（取第一条出边） */
    private static String nextNode(List<SequenceFlow> flows, String nodeId) {
        for (SequenceFlow f : flows) {
            if (f.src.equals(nodeId)) return f.tgt;
        }
        return null;
    }

    /** 获取节点的所有出边 */
    private static List<SequenceFlow> outgoing(List<SequenceFlow> flows, String nodeId) {
        List<SequenceFlow> out = new ArrayList<>();
        for (SequenceFlow f : flows) {
            if (f.src.equals(nodeId)) out.add(f);
        }
        return out;
    }

    // ====== 安全表达式求值 ======

    /**
     * 安全地求值 BPMN 条件表达式。仅支持简单的布尔/比较/变量引用。
     * 不使用 Java ScriptEngine（安全），改用简单的手写解析器。
     */
    static boolean safeEval(String expr, Map<String, Object> ctx) {
        expr = expr.trim();

        // 处理 == True / == False
        if (expr.matches(".*\\s*==\\s*True\\s*$")) {
            String var = expr.replace("==", " ").replace("True", "").trim();
            return Boolean.TRUE.equals(ctx.get(var));
        }
        if (expr.matches(".*\\s*==\\s*False\\s*$")) {
            String var = expr.replace("==", " ").replace("False", "").trim();
            return Boolean.FALSE.equals(ctx.get(var));
        }

        // 处理 >= / <= / > / < 数值比较
        Matcher cmp = Pattern.compile("(\\w+)\\s*(>=|<=|>|<|==)\\s*([0-9.]+)").matcher(expr);
        if (cmp.matches()) {
            String var = cmp.group(1);
            String op = cmp.group(2);
            double val = Double.parseDouble(cmp.group(3));
            Object ctxVal = ctx.get(var);
            if (ctxVal instanceof Number num) {
                double dv = num.doubleValue();
                return switch (op) {
                    case ">=" -> dv >= val;
                    case "<=" -> dv <= val;
                    case ">" -> dv > val;
                    case "<" -> dv < val;
                    case "==" -> dv == val;
                    default -> false;
                };
            }
        }

        // 简单布尔变量
        Object raw = ctx.get(expr);
        return Boolean.TRUE.equals(raw);
    }
}
