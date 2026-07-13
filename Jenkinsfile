// ============================================================================
// 校园电商/外卖智能服务平台 — Java 重构版 CI/CD Pipeline
// 阶段: Checkout → Build+Test → Static Analysis → Package
//       → Docker Build → Docker Push → Deploy to K8s → Post
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
                     description: '跳过 K8s 部署')
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
                        // 从 Job SCM 环境变量取 commit，显式用 HTTPS clone
                        branch = env.BRANCH_NAME ?: env.GIT_BRANCH?.replace('origin/', '') ?: 'master'
                        checkout([$class: 'GitSCM',
                            branches: [[name: "refs/heads/${branch}"]],
                            userRemoteConfigs: [[url: 'https://github.com/2023111998/agentproject.git']]
                        ])
                        commit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        echo "Git SCM checkout: ${branch} @ ${commit}"
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

        // ===== Stage 5: 离线评测 (跳过—Docker-in-Docker nginx 挂载问题) =====
        stage('Evaluation') {
            when { expression { params.RUN_EVAL } }
            steps {
                echo '离线评测跳过 (Jenkins Docker 环境中 nginx volume 挂载冲突)'
                echo '宿主机执行: curl http://localhost/api/evaluate'
            }
        }

        // ===== Stage 6: Docker 构建 =====
        stage('Docker Build') {
            steps {
                echo 'Docker 构建跳过 (dev 模式)'
            }
        }

        // ===== Stage 7: Docker 推送 =====
        stage('Docker Push') {
            steps {
                echo 'Docker Push 跳过 (dev 模式)'
            }
        }

        // ===== Stage 8: K8s 部署 =====
        stage('Deploy to Kubernetes') {
            steps {
                echo 'K8s 部署跳过 (dev 模式)'
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
