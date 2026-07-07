# Jenkins CI/CD 部署指南

## 校园电商/外卖智能服务平台 — Java 重构版

### 一、前提条件

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| Jenkins | 2.414+ | 推荐最新 LTS |
| Java (Jenkins节点) | JDK 17 | Maven 编译需要 |
| Docker | 24+ | 镜像构建 |
| kubectl | 1.28+ | K8s 部署 (可选) |
| Maven | 3.9+ | 或在 Jenkins Tool Configuration 中配置 |

### 二、Jenkins 插件安装

在 **系统管理 → Plugins → Available plugins** 中安装:

```text
Pipeline (workflow-aggregator)          # 流水线核心 (通常已内置)
Pipeline: Stage View                    # 阶段可视化
Docker Pipeline                         # docker build/push
Kubernetes CLI                          # kubectl 命令
Email Extension                         # 邮件通知
Blue Ocean                              # 可视化流水线 UI (推荐)
```

### 三、凭据配置

在 **系统管理 → Credentials → System → Global credentials** 中添加:

| Credential ID | 类型 | 说明 | 内容 |
|---------------|------|------|------|
| `docker-registry-cred` | Username with password | Docker 镜像仓库登录 | Harbor/ACR 用户名+密码 |
| `k8s-config` | Secret file | K8s 集群 kubeconfig | 上传 `~/.kube/config` 文件 |
| `notify-email` | Secret text | 通知邮箱列表 | `admin@example.com` |

### 四、Maven 工具配置

在 **系统管理 → Tools → Maven 安装** 中:

- **Name:** `maven-3`
- **Install automatically:** ✅
- **Version:** 3.9.x

### 五、创建 Jenkins Pipeline 任务

#### 方式A: 手动创建

1. **New Item → Pipeline**
2. **General:**
   - GitHub project: `https://github.com/<org>/campus-assistant-java`
3. **Build Triggers:**
   - GitHub hook trigger for GITScm polling (可选)
   - Poll SCM: `H/5 * * * *` (可选)
4. **Pipeline:**
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: `https://github.com/<org>/campus-assistant-java.git`
   - Script Path: `Jenkinsfile`
5. **保存**

#### 方式B: Jenkins Job DSL (自动化创建)

```groovy
// job-dsl/campus_assistant_java.groovy
pipelineJob('campus-assistant-java') {
    definition {
        cpsScm {
            scm {
                git {
                    remote { url('https://github.com/<org>/campus-assistant-java.git') }
                    branch('*/main')
                }
                scriptPath('Jenkinsfile')
            }
        }
        triggers {
            scm('H/5 * * * *')
        }
    }
}
```

### 六、流水线参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `DEPLOY_ENV` | Choice | `dev` | dev=仅构建, staging=构建+推送, prod=构建+推送+部署 |
| `RUN_EVAL` | Boolean | `true` | 是否运行离线评测 |
| `SKIP_DEPLOY` | Boolean | `false` | 跳过 K8s 部署 |
| `SKIP_TESTS` | Boolean | `false` | 跳过单元测试 (仅紧急修复) |
| `DOCKER_TAG_OVERRIDE` | String | (空) | 手动指定镜像 Tag |

### 七、流水线阶段说明

```
Stage 1: Checkout         → 代码检出 (Git SCM / 本地测试模式)
Stage 2: Build & Test     → Maven 编译 + JUnit 19 测试
Stage 3: Static Analysis  → 代码统计报告
Stage 4: Evaluation       → 启动 Docker 全栈 → 运行评测 → 保存结果
Stage 5: Package          → Maven 打包 (跳过测试)
Stage 6: Docker Build     → 构建 4 个微服务镜像
Stage 7: Docker Push      → 推送到 Registry (staging/prod)
Stage 8: Deploy to K8s    → kubectl 滚动更新 (仅 prod)
Stage 9: Post             → 通知 + 工作区清理
```

### 八、环境变量覆盖

在 Jenkins Job Configuration 中可设置全局环境变量:

```properties
DOCKER_REGISTRY=harbor.example.com/campus    # 镜像仓库地址
K8S_NAMESPACE=campus-prod                     # K8s 命名空间
GIT_REPO_URL=https://github.com/<org>/campus-assistant-java.git
```

### 九、本地测试 Jenkinsfile

无需 Jenkins 服务器，可在本地通过 Jenkins 命令行测试:

```bash
# 1. 安装 Jenkins CLI Runner
wget https://get.jenkins.io/war-stable/latest/jenkins.war

# 2. 本地运行验证语法
java -jar jenkins.war --httpPort=8090

# 3. 或者使用 Jenkinsfile Runner (实验性)
docker run --rm -v $(pwd):/workspace \
    jenkins/jenkinsfile-runner \
    --file /workspace/Jenkinsfile
```

### 十、故障排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| `mvn: command not found` | Maven 工具未配置 | 系统管理 → Tools → 添加 Maven 安装 |
| `docker: permission denied` | Jenkins 用户无 Docker 权限 | `usermod -aG docker jenkins` |
| `kubectl: command not found` | Kubernetes CLI 插件缺失 | 安装插件或手动安装 kubectl |
| `Connection refused` (评测阶段) | Docker Compose 启动失败 | 检查 Docker 服务状态 + 端口占用 |
| `Unable to mount volumes` | Docker 卷权限问题 | Windows: 检查 Docker Desktop 文件共享设置 |
| `ImagePullBackOff` (K8s) | 镜像仓库认证失败 | 检查 `docker-registry-cred` 凭据 + K8s imagePullSecrets |

---

## Python 原版 Jenkins 部署

Python 版 (`campus-assistant/Jenkinsfile`) 的搭建步骤类似，区别如下:

| 维度 | Java 版 | Python 版 |
|------|---------|----------|
| 构建工具 | Maven 3.9 | pip (无构建) |
| 插件需求 | Docker Pipeline, Kubernetes CLI, Maven Integration | Docker Pipeline, Kubernetes CLI |
| 凭据需求 | 相同 | 相同 |
| Dockerfile 数量 | 4 (多模块) | 1 (单容器) |
| K8s 镜像名称 | `${DOCKER_IMAGE}-<svc>:<tag>` | `${DOCKER_IMAGE}:<tag>` |
