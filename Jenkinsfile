// ============================================================================
// 校园电商/外卖智能服务平台 — Java 重构版 CI/CD Pipeline
// 阶段: Checkout → Build+Test → Static Analysis → Package
//       → Evaluation → Deploy to Local → Smoke Test → Health Check
// 触发: 手动 Build Now (Jenkins UI)
//
// 部署策略: Jenkins DinD 构建镜像 + SSH 单次调用 deploy.sh
//           deploy.sh 在 Windows 宿主机原生执行容器生命周期管理
// ============================================================================

pipeline {
    options { skipDefaultCheckout() }
    agent any

    environment {
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'harbor.example.com/campus'}"
        DOCKER_IMAGE    = 'campus-assistant-java'
        DOCKER_TAG      = "${env.BUILD_NUMBER}"
        K8S_NAMESPACE   = 'campus-prod'
        MAVEN_OPTS      = '-Dmaven.repo.local=.m2/repository'
        MAVEN_HOME      = tool name: 'maven-3', type: 'maven'

        // SSH 远程执行 — 所有 docker compose 命令通过 SSH 在宿主机原生执行
        WINDOWS_HOST    = '18489@host.docker.internal'
        PROJECT_DIR_WIN = 'D:/lab/Agent服务工程/campus-assistant-java'
        SSH_OPTS        = '-o StrictHostKeyChecking=no -o ConnectTimeout=10'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'],
               description: '部署环境')
        booleanParam(name: 'RUN_EVAL', defaultValue: true,
                     description: '运行离线评测')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false,
                     description: '勾选以跳过本地 Docker 部署（默认不勾选，即执行部署）')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false,
                     description: '跳过单元测试')
        string(name: 'DOCKER_TAG_OVERRIDE', defaultValue: '',
               description: '手动指定镜像 Tag')
    }

    stages {

        // ===== Stage 1: 代码检出 =====
        // 策略: 从 GitHub HTTPS clone（含 retry 容错 GnuTLS 间歇失败）
        //       /mnt 本地目录仅作 fallback
        stage('Checkout') {
            steps {
                script {
                    def commit = 'unknown'
                    def branch = 'master'

                    // 主路径: GitHub HTTPS clone（含 3 次重试）
                    try {
                        retry(3) {
                            sh '''
                                rm -rf ./* ./.[!.]* 2>/dev/null || true
                                git clone --depth 1 --branch master https://github.com/2023111998/agentproject.git .
                                git rev-parse --short HEAD > .git_commit
                            '''
                        }
                        commit = sh(script: 'cat .git_commit', returnStdout: true).trim()
                        echo "GitHub HTTPS clone: ${branch} @ ${commit}"
                    } catch (Exception e) {
                        // GitHub 不可用时 fallback 到本地 /mnt 挂载
                        echo "GitHub clone 失败: ${e.message}"
                        if (fileExists('/mnt/campus-assistant-java/pom.xml')) {
                            sh '''
                                echo "=== Fallback: 从本地 /mnt 复制 ==="
                                rm -rf ./* ./.[!.]* 2>/dev/null || true
                                cp -r /mnt/campus-assistant-java/* . 2>/dev/null
                                cp -r /mnt/campus-assistant-java/.[!.]* . 2>/dev/null || true
                                rm -rf .git 2>/dev/null || true
                            '''
                            commit = sh(script: 'cd /mnt/campus-assistant-java && git rev-parse --short HEAD 2>/dev/null || echo "local"', returnStdout: true).trim()
                            echo "本地 /mnt 复制: ${branch} @ ${commit}"
                        } else {
                            error('GitHub 和本地 /mnt 均不可用，无法获取源码')
                        }
                    }

                    env.GIT_COMMIT = commit
                    env.GIT_BRANCH = branch
                    env.DOCKER_TAG = params.DOCKER_TAG_OVERRIDE ?: "${env.BUILD_NUMBER}-${commit}"
                    echo "Branch: ${branch}, Build: #${env.BUILD_NUMBER}, Commit: ${commit}"
                }
            }
        }

        // ===== Stage 2: Maven 编译 + 单元测试 =====
        stage('Build & Unit Test') {
            steps {
                script {
                    def mvnCmd = "${MAVEN_HOME}/bin/mvn clean test"
                    if (params.SKIP_TESTS) {
                        mvnCmd = "${MAVEN_HOME}/bin/mvn clean compile -DskipTests"
                    }
                    // retry 容错 Maven Central SSL 间歇断开
                    retry(3) {
                        sh """
                            echo "=== Maven 编译 + 单元测试 ==="
                            ${mvnCmd}
                        """
                    }
                }
            }
            post {
                success { echo '编译 + 单元测试通过' }
                failure { error('编译或测试失败，流水线中止') }
            }
        }

        // ===== Stage 3: 代码质量分析 =====
        stage('Static Analysis') {
            steps {
                sh '''
                    echo "=== 代码统计 ==="
                    find . -name "*.java" -not -path "*/target/*" | xargs wc -l 2>/dev/null | tail -1
                    find . -name "*.java" -not -path "*/target/*" | wc -l
                    grep -c "<module>" pom.xml 2>/dev/null || echo "0"
                '''
            }
        }

        // ===== Stage 4: Maven 打包 =====
        stage('Package') {
            steps {
                retry(3) {
                    sh """
                        echo "=== Maven 打包 ==="
                        ${MAVEN_HOME}/bin/mvn clean package -DskipTests
                        find . -name "*.jar" -path "*/target/*" | grep -v original | sort
                    """
                }
            }
        }

        // ===== Stage 5: 离线评测 =====
        stage('Evaluation') {
            when { expression { params.RUN_EVAL } }
            steps {
                script {
                    echo '=== 离线评测 (通过宿主机 API) ==='
                    try {
                        def passed = sh(
                            script: "curl -s http://host.docker.internal/api/evaluate | python3 -c \"import sys,json; d=json.load(sys.stdin); print(d.get('passed',0))\"",
                            returnStdout: true
                        ).trim()
                        def total = sh(
                            script: "curl -s http://host.docker.internal/api/evaluate | python3 -c \"import sys,json; d=json.load(sys.stdin); print(d.get('total',0))\"",
                            returnStdout: true
                        ).trim()
                        if (passed == total && total != '0') {
                            echo "评测通过: ${passed}/${total} (100%)"
                        } else {
                            echo "WARNING: 评测未全部通过: ${passed}/${total}"
                        }
                    } catch (Exception e) {
                        echo "WARNING: 无法解析评测结果（服务可能未运行）: ${e.message}"
                    }
                }
            }
        }

        // ===== Stage 6: 本地 Docker 部署 (SSH 远程执行) =====
        // 策略: Jenkins DinD 构建镜像 → SSH 单次调用 deploy.sh
        //       deploy.sh 在 Windows 宿主机原生执行: stop → up --no-deps → wait → restart nginx
        stage('Deploy to Local') {
            when {
                allOf {
                    expression { params.DEPLOY_ENV == 'dev' }
                    expression { !params.SKIP_DEPLOY }
                }
            }
            steps {
                script {
                    timeout(time: 300, unit: 'SECONDS') {
                        // Step 1: Jenkins DinD 构建镜像
                        sh '''
                            echo "=== [Jenkins DinD] 构建 Docker 镜像 ==="
                            docker compose build campus-server-1 campus-server-2 order-service product-service logistics-service
                            echo "镜像构建完成"
                        '''

                        // Step 2: SSH 远程部署 — 单次调用 deploy.sh
                        sh """
                            echo "=== [SSH] 远程部署 ==="
                            ssh ${SSH_OPTS} ${WINDOWS_HOST} 'bash /d/lab/Agent服务工程/campus-assistant-java/deploy.sh'
                            echo "部署完成"
                        """
                    }
                }
            }
            post {
                failure {
                    echo 'WARNING: 本地部署失败，请检查构建日志。nginx/mysql/redis 由宿主机管理，不受影响。'
                    error('本地部署失败，请检查 Jenkins 构建日志')
                }
            }
        }

        // ===== Stage 7: 烟雾测试 =====
        stage('Smoke Test') {
            steps {
                script {
                    timeout(time: 60, unit: 'SECONDS') {
                        def passed = 0
                        def failed = 0

                        echo "=== 烟雾测试：关键端点验证 ==="

                        // 页面端点
                        def endpoints = [
                            [name: '用户端首页',       url: 'http://host.docker.internal/'],
                            [name: '商家端',           url: 'http://host.docker.internal/merchant'],
                            [name: '骑手端',           url: 'http://host.docker.internal/rider'],
                            [name: '智能助理页面',     url: 'http://host.docker.internal/chat'],
                            [name: '商品列表 API',     url: 'http://host.docker.internal/api/products'],
                        ]

                        endpoints.each { ep ->
                            def status = sh(
                                script: "curl -s -o /dev/null -w '%{http_code}' ${ep.url}",
                                returnStdout: true
                            ).trim()
                            if (status == '200') {
                                echo "  [PASS] ${ep.name}: ${ep.url} -> ${status}"
                                passed++
                            } else {
                                echo "  [FAIL] ${ep.name}: ${ep.url} -> ${status}"
                                failed++
                            }
                        }

                        // Agent API (POST)
                        echo "--- Agent API 测试 ---"
                        def chatStatus = sh(
                            script: """curl -s -o /dev/null -w '%{http_code}' \
                                -X POST http://host.docker.internal/api/chat \
                                -H 'Content-Type: application/json' \
                                -d '{"message":"你好"}'""",
                            returnStdout: true
                        ).trim()
                        if (chatStatus == '200') {
                            echo "  [PASS] Agent API (POST /api/chat) -> ${chatStatus}"
                            passed++
                        } else {
                            echo "  [FAIL] Agent API (POST /api/chat) -> ${chatStatus}"
                            failed++
                        }

                        // 评测 API
                        echo "--- 评测 API 测试 ---"
                        def evalPassed = sh(
                            script: "curl -s http://host.docker.internal/api/evaluate | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get(\"passed\",0))'",
                            returnStdout: true
                        ).trim()
                        def evalTotal = sh(
                            script: "curl -s http://host.docker.internal/api/evaluate | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get(\"total\",0))'",
                            returnStdout: true
                        ).trim()
                        if (evalPassed == evalTotal && evalTotal != '0') {
                            echo "  [PASS] 评测 API: ${evalPassed}/${evalTotal} (100%)"
                            passed++
                        } else {
                            echo "  [FAIL] 评测 API: ${evalPassed}/${evalTotal}"
                            failed++
                        }

                        echo ""
                        echo "=== 烟雾测试结果: ${passed} PASS, ${failed} FAIL ==="

                        if (failed > 0) {
                            currentBuild.result = 'UNSTABLE'
                            echo "WARNING: 烟雾测试有 ${failed} 项失败，标记为 UNSTABLE"
                        }
                    }
                }
            }
        }

        // ===== Stage 8: 健康检查 =====
        stage('Health Check') {
            steps {
                script {
                    timeout(time: 30, unit: 'SECONDS') {
                        def healthOk = true

                        echo "=== 健康检查 ==="

                        // 1. Actuator 聚合健康检查
                        echo "--- Actuator 健康检查 ---"
                        def actuatorResult = sh(
                            script: 'curl -s http://host.docker.internal/actuator/health',
                            returnStdout: true
                        ).trim()
                        echo "Actuator: ${actuatorResult}"
                        def actuatorStatus = sh(
                            script: "echo '${actuatorResult}' | python3 -c \"import sys,json; d=json.load(sys.stdin); print(d.get('status','DOWN'))\"",
                            returnStdout: true
                        ).trim()
                        if (actuatorStatus == 'UP') {
                            echo "  [PASS] Actuator 聚合状态: UP"
                        } else {
                            echo "  [FAIL] Actuator 聚合状态: ${actuatorStatus}"
                            healthOk = false
                        }

                        // 列出子组件状态
                        def components = sh(
                            script: "echo '${actuatorResult}' | python3 -c 'import sys, json; d=json.load(sys.stdin); components=d.get(\"components\",{}); [print(f\"    {k}: {v.get(\"status\",\"UNKNOWN\")}\") for k,v in components.items()]'",
                            returnStdout: true
                        ).trim()
                        if (components) echo "${components}"

                        // 2. Docker 容器状态 (SSH 远程获取)
                        echo "--- Docker 容器状态 ---"
                        sh "ssh ${SSH_OPTS} ${WINDOWS_HOST} \"cd /d D:\\\\lab\\\\Agent服务工程\\\\campus-assistant-java && docker compose --project-name campus-assistant-java ps\""

                        // 3. 微服务直连健康检查 (容器网络)
                        echo "--- 微服务直连健康检查 ---"
                        def microservices = [
                            'order-service:8001',
                            'product-service:8002',
                            'logistics-service:8003'
                        ]
                        microservices.each { svc ->
                            def svcHealth = sh(
                                script: "curl -s -o /dev/null -w '%{http_code}' http://${svc}/actuator/health 2>/dev/null || echo '000'",
                                returnStdout: true
                            ).trim()
                            if (svcHealth == '200') {
                                echo "  [PASS] ${svc} -> ${svcHealth}"
                            } else {
                                echo "  [FAIL] ${svc} -> ${svcHealth}"
                                healthOk = false
                            }
                        }

                        // 4. Prometheus 指标验证
                        echo "--- Prometheus 指标验证 ---"
                        def promMetrics = sh(
                            script: "curl -s http://host.docker.internal/actuator/prometheus 2>/dev/null | grep jvm_ | head -1",
                            returnStdout: true
                        ).trim()
                        if (promMetrics.contains('jvm_')) {
                            echo "  [PASS] Prometheus 指标正常产出"
                        } else {
                            echo "  [FAIL] Prometheus 指标异常"
                            healthOk = false
                        }

                        echo ""
                        if (!healthOk) {
                            currentBuild.result = 'UNSTABLE'
                            echo "WARNING: 健康检查未通过，标记为 UNSTABLE"
                        } else {
                            echo "=== 健康检查: 全部通过 ==="
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true,
                    patterns: [[pattern: '*/target/', type: 'INCLUDE'],
                               [pattern: '.m2/', type: 'INCLUDE']])
        }
        success { echo "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" }
        failure { echo "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}" }
    }
}
