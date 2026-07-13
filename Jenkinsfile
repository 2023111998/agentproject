// ============================================================================
// 校园电商/外卖智能服务平台 — Java 重构版 CI/CD Pipeline
// 阶段: Checkout → Build+Test → Static Analysis → Package
//       → Deploy to Local → Smoke Test → Health Check → Post
// 触发: 手动 Build Now (Jenkins UI)
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
        JENKINS_LOCAL_TEST = 'false'
        GIT_REPO_URL      = 'https://github.com/2023111998/agentproject.git'
        GIT_BRANCH        = 'master'
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
        stage('Checkout') {
            steps {
                script {
                    def commit = 'unknown'
                    def branch = 'unknown'

                    if (env.GIT_COMMIT && env.GIT_COMMIT != 'null') {
                        // Job 从 SCM 获取 Jenkinsfile 后，这里直接用 HTTPS 从 GitHub clone
                        branch = env.BRANCH_NAME ?: env.GIT_BRANCH?.replace('origin/', '') ?: 'master'
                        sh """
                            rm -rf ./* ./.[!.]* 2>/dev/null || true
                            git clone --depth 1 --branch ${branch} https://github.com/2023111998/agentproject.git .
                            git rev-parse --short HEAD > .git_commit
                        """
                        commit = sh(script: 'cat .git_commit', returnStdout: true).trim()
                        echo "Git HTTPS clone: ${branch} @ ${commit}"
                    } else if (env.GIT_URL) {
                        branch = env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'main'
                        sh """
                            git clone --depth 1 --branch ${branch} ${env.GIT_URL} .
                            commit=\$(git rev-parse --short HEAD)
                        """
                        commit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        echo "Git clone: ${branch} @ ${commit}"
                    } else if (env.JENKINS_LOCAL_TEST == 'true') {
                        sh '''
                            set +e
                            cp -r /mnt/campus-assistant-java/* . 2>/dev/null
                            cp -r /mnt/campus-assistant-java/.[!.]* . 2>/dev/null
                            rm -rf .git 2>/dev/null
                            rm -rf nginx mysql 2>/dev/null
                            mkdir -p nginx mysql
                            cp /mnt/campus-assistant-java/nginx/nginx.conf nginx/nginx.conf
                            cp /mnt/campus-assistant-java/mysql/init.sql mysql/init.sql
                            echo "Local test mode"
                        '''
                        commit = 'local'
                        branch = 'local-test'
                        echo "Local test mode"
                    } else if (env.GIT_REPO_URL) {
                        checkout([$class: 'GitSCM',
                            branches: [[name: env.GIT_BRANCH ?: '*/main']],
                            userRemoteConfigs: [[url: env.GIT_REPO_URL]]
                        ])
                        commit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        echo "Standard SCM checkout: ${commit}"
                    } else {
                        echo "Fallback: copy from /mnt"
                        sh 'cp -r /mnt/campus-assistant-java/* . 2>/dev/null || echo "OK"; cp -r /mnt/campus-assistant-java/.[!.]* . 2>/dev/null || true'
                        commit = 'local'
                        branch = 'local'
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
                    sh """
                        echo "=== Maven 编译 + 单元测试 ==="
                        ${mvnCmd}
                    """
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
                sh """
                    echo "=== Maven 打包 ==="
                    ${MAVEN_HOME}/bin/mvn clean package -DskipTests
                    find . -name "*.jar" -path "*/target/*" | grep -v original | sort
                """
            }
        }

        // ===== Stage 5: 离线评测 (跳过) =====
        stage('Evaluation') {
            when { expression { params.RUN_EVAL } }
            steps {
                echo '离线评测跳过 (Jenkins Docker 环境中 nginx volume 挂载冲突)'
                echo '宿主机执行: curl http://localhost/api/evaluate'
            }
        }

        // ===== Stage 6: 本地 Docker 部署 =====
        // 注意: Jenkins DinD 环境下 nginx volume 挂载有已知问题
        // 当前策略: 仅重建 Java 服务，nginx/mysql/redis 由宿主机管理
        stage('Deploy to Local') {
            when {
                allOf {
                    expression { params.DEPLOY_ENV == 'dev' }
                    expression { !params.SKIP_DEPLOY }
                }
            }
            steps {
                script {
                    timeout(time: 120, unit: 'SECONDS') {
                        sh '''
                            echo "=== 只停止 Java 微服务（保留 nginx/mysql/redis）==="
                            docker compose stop campus-server-1 campus-server-2 order-service product-service logistics-service 2>/dev/null || true
                            docker compose rm -f campus-server-1 campus-server-2 order-service product-service logistics-service 2>/dev/null || true

                            echo "=== 确保 nginx.conf 文件存在（而非目录）==="
                            rm -rf nginx 2>/dev/null || true
                            mkdir -p nginx
                            if [ -f nginx/nginx.conf ]; then
                                echo "nginx.conf 已存在，跳过复制"
                            else
                                if [ -f nginx.conf ]; then
                                    cp nginx.conf nginx/nginx.conf
                                elif [ -f /mnt/campus-assistant-java/nginx/nginx.conf ]; then
                                    cp /mnt/campus-assistant-java/nginx/nginx.conf nginx/nginx.conf
                                else
                                    echo "ERROR: nginx.conf 未找到"
                                    exit 1
                                fi
                            fi

                            echo "=== 仅构建并启动 Java 微服务（跳过 nginx/mysql/redis — 避免 DinD volume 冲突）==="
                            docker compose up -d --build --no-deps campus-server-1 campus-server-2 order-service product-service logistics-service

                            echo "=== 重新加载 nginx（宿主机管理，仅 restart）==="
                            docker restart campus-nginx 2>/dev/null || echo "nginx 未运行（由宿主机管理，跳过）"

                            echo "=== 等待服务就绪 ==="
                            sleep 3
                            for i in $(seq 1 30); do
                                if curl -sf http://host.docker.internal/api/health > /dev/null 2>&1; then
                                    echo "服务就绪 (${i}s)"
                                    break
                                fi
                                [ $i -eq 30 ] && echo "ERROR: 服务启动超时" && exit 1
                                sleep 2
                            done

                            echo ""
                            echo "=== 容器运行状态 ==="
                            docker compose ps
                        '''
                    }
                }
            }
            post {
                failure {
                    sh 'docker compose stop campus-server-1 campus-server-2 order-service product-service logistics-service 2>/dev/null || true'
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
                        def evalResult = sh(
                            script: 'curl -s http://host.docker.internal/api/evaluate',
                            returnStdout: true
                        ).trim()
                        def evalPassed = sh(
                            script: "echo '${evalResult}' | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get(\"passed\",0))'",
                            returnStdout: true
                        ).trim()
                        def evalTotal = sh(
                            script: "echo '${evalResult}' | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get(\"total\",0))'",
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

                        // 2. Docker 容器状态
                        echo "--- Docker 容器状态 ---"
                        sh 'docker compose ps'

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
                            script: "curl -s http://host.docker.internal/actuator/prometheus 2>/dev/null | head -5",
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
