# Git 仓库报告

## 基本信息

- 仓库位置: `D:\lab\Agent服务工程\campus-assistant-java\.git`
- 分支: `master`
- 提交数: **20 次**
- 远程仓库: **无** (纯本地仓库)
- 开发周期: 2026-07-06 至 2026-07-10 (5 天)
- 当前状态: 工作区干净

## 20 次提交历史

```
ddbf167  2026-07-10  chore: update commit count to 19 in CLAUDE.md
a780d86  2026-07-10  chore: update commit count to 18 in CLAUDE.md
e1c2995  2026-07-10  docs: 更新 CLAUDE.md 反映最新代码统计和 Git 历史
f55b65c  2026-07-10  feat: 课程报告完善 + 架构图v2 + 截图输出 + CLAUDE.md更新
345d4c3  2026-07-09  update-claude
79c8529  2026-07-09  word-tables
2ccc076  2026-07-09  "diagrams+reports"
f4588c3  2026-07-08  screenshot-readme
b1341a5  2026-07-08  report-screenshots
6e6a224  2026-07-07  final-state
8ce5192  2026-07-07  update-docs
c2c1f95  2026-07-07  fix-jenkinsfile3
a7fd8b8  2026-07-07  fix-jenkinsfile2
e6ca014  2026-07-07  fix-jenkinsfile
377fd69  2026-07-06  Fix: swap Package before Evaluation stage
99babeb  2026-07-06  v3: clean fix with safe cp
fee4114  2026-07-06  Fix: exclude .git from cp
b4c62c1  2026-07-06  Fix: clean Jenkinsfile with skipDefaultCheckout
a60274d  2026-07-06  Fix: skipDefaultCheckout + mount copy
d07f992  2026-07-06  Initial commit: campus-assistant Java project
```

## 项目统计

- Java 文件: **43** 个
- Java 行数: **2,882** 行
- 测试方法: **18** 个 @Test
- 测试类: **4** 个
- BPMN 流程: **2** 个
- 前端页面: **4** 个 HTML
- 配置/文档: **35** 个
- Docker 容器: **11** 个全运行
- 评测通过率: **8/8 = 100%**
- SLA 评级: **A+ (4个9)**

## 截图清单

- 架构图 6 张: usecase/architecture/deployment/soa/bpmn/jenkins
- 运维 10 张: evaluate/sla/docker/git/maven/static/health/prometheus/chat/jenkins

## Jenkins 说明

当前使用策略 3 (本地文件拷贝, cp -r /mnt/...)。
Jenkinsfile 支持 4 种拉取策略。要从远程拉取:
1. 创建远程仓库 (GitHub/GitLab/Gitee)
2. `git remote add origin <URL>`
3. `git push -u origin master`
4. 在 Jenkinsfile 中将 Checkout 策略切到 `checkout scm`
