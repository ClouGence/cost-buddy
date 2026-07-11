# Cost Buddy

Cost Buddy 是一个面向公共云账单的手动审计工具，当前优先支持阿里云账单。它的目标不是做实时成本告警，而是帮助你定期发现那些不熟悉、金额可能不大、但持续按量收费的产品或计费项。

## 功能

- 云账号管理：配置阿里云 AK/SK 和可选费用归属账号 ID。
- 手动账单审计：选择云账号和账单日，采集该账单日的明细并聚合为审计项。
- 审计项聚合：按产品、产品明细、计费项、账单类型等维度汇总金额和资源数量。
- 资源明细：查看某个审计项背后的资源 ID、地域、资源组、用量和金额。
- 忽略规则：对已确认无需关注的计费项建立忽略规则，后续审计自动匹配。
- 优化说明：调用已配置的 AI 引擎，解释产品用途、费用来源，并给出释放或关闭路径建议。
- 中英双语界面：默认中文，可在页面右上角切换语言。

## 基本使用

1. 在 `云账号` 页面添加阿里云账号，并使用检查按钮确认账单 API 可访问。
2. 在 `AI` 页面配置可选的 OpenAI-compatible AI 引擎，用于生成优化说明。
3. 在 `审计` 页面选择云账号和稳定账单日，点击运行审计。
4. 进入聚合明细，重点查看陌生且非零金额的计费项。
5. 对需要进一步排查的项查看资源明细，或点击优化说明获取产品用途和释放路径建议。
6. 对确认无需关注的计费项建立忽略规则，再重新应用规则。

## 本地开发

默认元数据库：

```text
jdbc:mysql://127.0.0.1:3306/costbuddy
username: costbuddy_user
password: CostBuddy@2026
```

后端：

```bash
./gradlew bootRun
```

前端：

```bash
cd frontend
npm install
npm run dev
```

如果通过 Spring Boot 直接访问完整应用，需要先构建前端：

```bash
cd frontend
npm run build
cd ..
./gradlew bootRun
```

默认访问地址：

```text
http://localhost:8766
```

## 本地构建

首次构建前，需确保 `motherboard-sdk` 已发布到本地 Maven 仓库。然后构建前端和后端：

```bash
cd frontend
npm ci
npm run build
cd ..
./gradlew clean build
```

可执行 JAR 生成在 `build/libs/` 目录。

## 打包部署

Docker 镜像包构建和离线部署方式见 [DEPLOY.md](DEPLOY.md)。
