# 仓库指南

## 项目结构与模块划分
`MyAIAgent02/` 是主要的 Java 工作区，也是 Maven 父项目。当前包含 5 个活跃模块：`agent-core` 负责对话流与核心编排，`agent-rag` 负责检索增强，`agent-mcp` 负责外部工具接入，`agent-skills` 负责可复用技能，`agent-api` 作为 Spring Boot 启动入口。Java 代码位于 `src/main/java`，MyBatis XML 位于 `src/main/resources/mapper`，共享配置位于 `agent-api/src/main/resources/application.yml`。

`MyAIAgent02/backend/` 是较早的单体后端快照；除非任务明确要求维护旧代码，否则新改动优先落在 `agent-*` 模块中。Vue 前端位于 `MyAIAgent02 - frontend/frontend/src`，数据库初始化脚本位于 `create_tables.sql`。

## 构建、测试与开发命令
后端命令在 `MyAIAgent02/` 目录执行：

- `mvn clean test`：编译所有 Maven 模块并运行 Spring Boot 测试。
- `mvn -pl agent-api spring-boot:run`：启动 API，默认端口 `8081`。
- `mvn -pl agent-api -am package`：构建可运行的 API 及其依赖模块。

前端命令在 `MyAIAgent02 - frontend/frontend/` 目录执行：

- `npm install`：安装 Vite、Vue 和 TypeScript 依赖。
- `npm run dev`：启动本地前端开发服务器。
- `npm run build`：生成生产环境构建产物。
- `npm run preview`：本地预览已构建的前端包。

## 编码风格与命名约定
Java、XML、YAML 使用 4 个空格缩进；现有 Vue 文件的模板和样式使用 2 个空格。遵循 Java 常规命名：类名使用 `PascalCase`，字段和方法使用 `camelCase`。Mapper 接口与 XML 文件保持一一对应，例如 `ConversationMessageMapper.java` 对应 `ConversationMessageMapper.xml`。优先编写职责单一的 Spring Service 和聚焦业务的 Controller。不要提交 `target/` 或 `node_modules/` 等生成产物。

## 测试规范
项目已通过 `spring-boot-starter-test` 提供 JUnit 5 支持，但仓库当前没有已提交的 `src/test` 目录。新增逻辑时，请在对应模块旁补充测试，命名使用 `*Test.java`，重点覆盖 controller、service 与 mapper 的行为。前端暂未配置测试框架，因此至少用 `npm run dev` 手工验证聊天、会话、上传和技能执行流程。

## 提交与 Pull Request 规范
最近的提交信息以简短、祈使句风格为主，通常使用中文，例如 `修复服务重启后对话的记忆消失问题`。保持这一风格，并在合适时注明影响模块。避免把前端和后端的大范围重构混在同一个提交中。PR 需要说明变更范围、列出涉及模块、标明数据库结构或配置改动；如果修改了界面，请附带截图。

## 安全与配置提示
不要把数据库账号、密码或 API Key 提交到仓库。`application.yml` 中的值应视为本地开发默认配置，敏感信息应迁移到被忽略的覆盖配置或环境变量中，提交 PR 前先清理密钥与本地凭据。

## 本机固定构建环境
后续默认使用这套本机工具链，不再重复探测旧环境：

- Maven Home：`D:\DevelopmentTools\Maven\apache-maven-3.9.14`
- Maven 本地仓库：`D:\DevelopmentTools\Maven\ngRepository`
- JDK Home：`D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6`

PowerShell 推荐环境变量设置：

```powershell
$env:JAVA_HOME='D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6'
$env:PATH='D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6\bin;D:\DevelopmentTools\Maven\apache-maven-3.9.14\bin;' + $env:PATH
```

PowerShell 推荐 Maven 命令：

```powershell
mvn --% -Dmaven.repo.local=D:\DevelopmentTools\Maven\ngRepository -pl agent-api -am test
```
