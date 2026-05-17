# HomeworkChecker — Agents 备忘录

## 构建命令

```bash
mvn compile          # 编译
mvn test             # 运行测试
mvn package          # 打包
mvn clean compile    # 清理并编译
```

**注意：** 运行前需设置环境变量 `$env:JAVA_HOME = "D:\Program Files\java25"`

## 项目规则

### 架构
- **MVC + Service Layer**：FXML 是 View，controller/ 包是 Controller，service/ 包是业务逻辑
- Controller 通过 `@FXML` 注入 UI 组件，不直接操作 Scene Graph API
- Service 通过构造器注入所需依赖（Controller 在 `initialize()` 中实例化 Service）

### FXML 约定
- 所有 FXML 放在 `src/main/resources/com/xfty/homeworkchecker/fxml/`
- 所有 CSS 放在 `src/main/resources/com/xfty/homeworkchecker/theme/darkness/` 下按组件分类子目录
- 所有图标放在 `src/main/resources/com/xfty/homeworkchecker/icon/`
- 国际化 key 用 `%key.name%` 格式引用
- 新 FXML 文件添加后需更新 `summarize.md` 中的映射表

### CSS 约定
- 每个组件一个独立 CSS 文件，按目录分组（`button/`, `textarea/`, `splitPane/` 等）
- 深色主题，基础背景色 `#1e1e1e`
- JavaFX CSS 选择器使用标准类名（`.split-pane`, `.text-area` 等）

### Java 约定
- 不添加注释（除非被要求）
- 日志使用 SLF4J + Logback（`LoggerFactory.getLogger()`）
- 全局状态通过 `Idf.java` 静态变量共享
- 数据库操作通过 `HomeworkDatabase.java`

### 国际化
- properties 文件放在 `src/main/resources/com/xfty/homeworkchecker/i18n/`
- FXML 中使用 `%key.name%` 引用
- Java 中通过 `Idf.userLanguageBundle.getString("key.name")` 获取

## 新增功能 checklist

1. 更新 FXML 布局
2. 更新 Controller（添加 `@FXML` 字段和事件处理方法）
3. 添加 CSS 样式（如需要）
4. 添加国际化 key（如需要）
5. 添加图标资源（如需要）
6. 更新 `summarize.md`（可选）

## 约束
- 不修改 `module-info.java` 除非添加新依赖
- 不提交 `.env`、`credentials.json` 等敏感文件
- 不执行 `git push --force` 到 main/master
