# Cost Buddy 打包部署

Cost Buddy 默认以一个应用镜像运行，镜像内包含后端 Spring Boot 和前端静态资源。目标机器不需要访问镜像仓库：构建阶段会生成本地镜像包，目标机器收到镜像包后执行 `install.sh`，脚本会 `docker load` 并通过 `docker compose` 启动服务。

构建阶段使用打包机本地的 Node、npm、Gradle 和 Java。Docker 只负责制作运行镜像，默认运行基础镜像为 `registry.cn-hangzhou.aliyuncs.com/cloudcanal/baseimage:centos8_x86_v2`。镜像内会创建 `costbuddy` 用户和 `/home/costbuddy` 家目录，应用在 `/home/costbuddy/app` 下运行。

## 构建镜像包

```bash
cd cost-buddy
IMAGE_TAG=0.1.0 ./deploy/docker/build.sh
```

默认会生成：

```text
deploy/dist/cost-buddy-0.1.0-amd64.image.tar
deploy/dist/cost-buddy-0.1.0-amd64.image.tar.sha256
deploy/dist/docker-compose.yml
deploy/dist/.env.example
deploy/dist/install.sh
```

默认镜像名保留仓库前缀，方便和后续镜像仓库部署兼容：

```text
cloudcanal-registry.cn-shanghai.cr.aliyuncs.com/clougence/cost-buddy:0.1.0-amd64
```

本地开发镜像包：

```bash
IMAGE_TAG=dev TAG_ARCH_SUFFIX=false ./deploy/docker/build.sh
```

如果需要覆盖运行基础镜像，可以指定：

```bash
RUNTIME_IMAGE=registry.cn-hangzhou.aliyuncs.com/cloudcanal/baseimage:centos8_x86_v2 \
./deploy/docker/build.sh
```

默认优先使用项目里的 `./gradlew`，找不到才使用系统 `gradle`。如果需要覆盖 Gradle 命令，可以指定：

```bash
GRADLE_CMD=./gradlew ./deploy/docker/build.sh
```

## 交付到目标机器

目标目录至少需要包含 `deploy/dist` 中的这些文件：

```text
docker-compose.yml
.env.example
install.sh
cost-buddy-0.1.0-amd64.image.tar
```

默认部署模式只启动 Cost Buddy 应用，连接已有 MySQL。可以按需把 `.env.example` 复制成 `.env` 并修改元数据库连接、端口、挂载目录等配置：

```env
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/costbuddy?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=costbuddy_user
SPRING_DATASOURCE_PASSWORD=CostBuddy@2026
```

如果 MySQL 在另一台机器上，把 `host.docker.internal` 换成那台机器的 IP 或域名。若 MySQL 在 Docker 宿主机上，需要确保 MySQL 监听的地址能被容器访问。

若目标目录没有 `.env`，`install.sh` 会自动从 `.env.example` 创建。

执行安装：

```bash
./install.sh
```

脚本会完成：

```text
docker load --input cost-buddy-*.image.tar
更新 .env 中的 COST_BUDDY_IMAGE
创建日志目录，并按 COST_BUDDY_UID/COST_BUDDY_GID 调整权限
docker compose --env-file .env -f docker-compose.yml up -d --force-recreate
```

如果当前目录有多个镜像包，可以显式指定：

```bash
IMAGE_PACKAGE=./cost-buddy-0.1.0-amd64.image.tar ./install.sh
```

默认访问地址：

```text
http://localhost:8766
```

应用日志默认落在：

```text
deploy/data/logs/cost-buddy.log
```

容器内对应路径为：

```text
/home/costbuddy/app/logs/cost-buddy.log
```
