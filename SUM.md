# HomeworkChecker 项目总结

## 项目概述

**HomeworkChecker** 是一款基于 JavaFX 的桌面应用程序，旨在帮助教师在教室内展示作业内容。支持多语言（11种语言）、作业记录隐式保存、历史作业查询、一键截屏、锁定防误触机制等功能。版本 `1.7.0-beta`，采用 Maven 构建，目标平台为 Windows 64位。

## 如需要项目结构请使用codegraph MCP工具获取，如果没有，请立即停止作业并告知使用者安装codegraph.

---

## 依赖栈

| 依赖 | 用途 |
|------|------|
| JavaFX 25.0.1 (controls/fxml/web) | UI 框架 & WebView |
| FastJSON 2.0.47 | JSON 解析 |
| OkHttp 4.12.0 | HTTP 客户端 (GitHub API, 文件下载) |
| CommonMark 0.21.0 | Markdown→HTML 渲染 |
| Logback 1.5.13 | 日志记录 |
| Commons IO 2.15.1 | 文件操作工具 |
| JUnit 5.10.0 | 单元测试 |

---

## 架构模式

**MVC + Service Layer** 分层架构：

- **Model**: `Idf.java` (全局状态池，所有模块通过静态变量共享配置、语言包、缓存)
- **View**: 24 个 FXML 布局 + 39 个主题 CSS (darkness 默认主题)
- **Controller**: `controller/` 包处理 FXML 事件、UI 状态切换
- **Service**: `service/` 包封装所有业务逻辑，`service/ui/` 子包按功能模块组织
- **数据持久化**: JSON 文件数据库 (`用户目录/homeworkChecker/` 下存储 `config.json`, `homeworkDatabase/YYYYMMDD`, `initTemple.txt`, `config/language.json`)

---

## 核心数据流

```
Entry.main()
  ├─ 首次运行? → [SetupWizard (5步向导)] → FileInitManager.initializeFirstRun()
  └─ 正常启动 → FileInitManager.initializeUserDirectories()
                → 读取 config.json → Idf.userConfig
                → 读取 initTemple.txt → Idf.initTemple
                → 读取 config/language.json → Idf.userLanguage
                → SingletonInstanceManager (文件锁 + WatchService 防多开)
                → MainPage (主界面)

MainPage 交互流程:
  编辑作业 → LockService.unlock() (解锁图标+启用编辑)
            → 编辑内容
            → LockService.lock() (锁定图标+缓存内容+写入数据库)
  自动保存看门狗: EditStateService 每5秒检测内容变化, 30秒无变化 → LockService.lock()

  警示卡片:
  CardUiService.onAddCard() → 展开添加框 → 输入标题/内容/选择严重性
  → ReminderCardService.addCard() → 写入数据库 (含 persistentCards.json)
  → CardUiService.renderCard() → 渲染卡片 DOM
  hover 显示编辑/删除按钮 → 内联编辑/删除 → ReminderCardService 持久化

历史作业查询:
  LoadHistoryHomework → WeekdayCalculatorService (计算日期) 
  → HomeworkContentFetcherService → HomeworkDatabase → 返回 JSON 内容
  → HistoryHomeworkChecker 展示

设置 (Settings.Index → 各子页面):
   字体/字号/图片卡片调整 → HomeworkArea → HomeworkAreaService → HomeworkDatabase.updateConfig() → 写 config.json
   初始模板编辑 → InitTemplateEditor → HomeworkDatabase.changeInitTemple() → 写 initTemple.txt
   语言切换 → languageChooser → HomeworkDatabase.changeLanguage() → 写 language.json → 重启
   更新管理 → UpdaterService → HttpClientService (GitHub API) → 下载 .msi → msiexec 安装
   重置作业 → ResetThings → Idf.needHomeworkShowingAreaClear → 主页面重置为 initTemple
```

---

## 关键文件间关系

| 文件 | 依赖/调用关系 |
|------|--------------|
| `Entry.java` | 启动入口，依赖 `FileInitManager`, `SingletonInstanceManager`, `MainPage`, `SetupWizardController` |
| `Idf.java` | 全局引用池，被所有 controller/service 引用 |
| `MainPage.java` | 依赖 11 个服务: `HomeworkDatabase`, `MainPageInitService`, `TopButtonService.ScreenshotService/HistoryHomeworkService/SettingsService/AboutService`, `PopupService`, `EditMainService`, `EditStateService`, `WindowListener`, `CardUiService`, `LockService`, `ReminderCardService`; 管理 cardContainer 折叠/展开 (divider ≥0.90 时 setManaged(false)) |
| `HomeworkDatabase.java` | 核心数据服务，被 `MainPage`, `HomeworkArea`, `MainPageInitService`, `HomeworkContentFetcherService`, `Index`, `languageChooser`, `LockService` 等使用 |
| `TopButtonService.java` | 含4个内部类: ScreenshotService, HistoryHomeworkService, SettingsService, AboutService |
| `CardUiService.java` | 卡片 UI 核心 (756行)，依赖 `ReminderCardService`, `PopupService`, `CardItem`; 管理卡片添加/编辑/删除/渲染/折叠 |
| `LockService.java` | 锁定/解锁服务，依赖 `HomeworkDatabase`, `Idf`; 被 `MainPage` 调用 |
| `ReminderCardService.java` | 卡片数据 CRUD + SHA256 校验 + persistentCards.json 同步; 被 `CardUiService` 调用 |
| `UpdaterService.java` | 依赖 `HttpClientService` (GitHub API), OkHttp 下载, CommonMark 渲染 HTML; 内部 `generateUpdateInfoHtml()` 替代了 `HtmlBuilder` |
| `SetupWizardController.java` | 管理5步向导，依赖 `Entry` 加载子步骤 FXML |
| `Settings/Index.java` | 设置面板导航，动态加载设置子页面到右侧区域（含 about.fxml） |
| `HomeworkArea.java` | 字体/字号/图片卡片设置控制器，依赖 `HomeworkAreaService`; 被 `Index.java` 加载 homeworkArea.fxml |
| `HomeworkAreaService.java` | 字体/字号/图片卡片设置业务逻辑，封装配置读写与 `HomeworkDatabase.updateConfig()` 持久化 |
| `LoadHistoryHomework.java` | 依赖 `WeekdayCalculatorService`, `HomeworkContentFetcherService`, `ButtonStateManagerService` |
| `DataBaseEditor.java` | 设置→数据库编辑器控制器，依赖 `DataBaseEditorService`; 负责历史作业集列表渲染、查看/删除操作、保留天数配置 |
| `DataBaseEditorService.java` | 数据库编辑器业务逻辑，封装文件扫描、作业数据读写、删除（含图片清理）、大小计算、retentionDays 配置持久化 |

---

## 配置文件目录 (用户目录/homeworkChecker/)

首次启动时由 `FileInitManager` 创建在 `%USERPROFILE%/homeworkChecker/` 下：
```
homeworkChecker/
├── config.json              # 用户配置 (字体/字号/自动化清理设置)
├── initTemple.txt           # 初始作业模板
├── config/language.json     # 语言设置
├── homeworkDatabase/        # 作业数据库 (YYYYMMDD 格式文件, JSON含SHA256)
├── progress.lock             # 单例文件锁 (运行时)
└── repeatedly.start          # 激活信号文件 (临时)
```

---

## 关键特性

- **单例运行**: 文件锁防止多开，WatchService 监听激活信号使已有窗口弹出前置
- **自动隐式保存**: 解锁编辑后，看门狗线程每5秒检测内容变化，30秒无变化自动保存并锁定
- **SHA256 完整性校验**: 作业数据库每条记录携带 `dataSHA256` 字段
- **一键截图**: Canvas 渲染文本为图片并存入剪贴板
- **自动更新**: 检测 GitHub Releases，下载 .msi 并通过 msiexec 静默安装
- **多语言**: 通过 ResourceBundle + properties 国际化，支持 11 种语言
- **首次运行向导**: 5 步设置语言/字体/模板，含花式动画
- **彩蛋**: About 页面快速连点触发隐藏游戏 (输入密码序列启动 EggPlant)
- **自动缩进**: 编辑区按 Enter 自动保持当前行缩进
- **可折叠卡片容器**: 编辑区右侧通过 SplitPane 集成卡片区域，拖拽分隔线 ≥ 9:1 自动折叠，向左拖拽恢复（`setManaged` 控制，无动画冲突）
- **警示卡片系统**: 支持在右侧面板添加三种严重程度的警示卡片（提示/警告/严重），含对应颜色左边框和图标；hover 显示编辑/删除按钮，内联编辑；数据持久化至 homework 数据库文件 + `persistentCards.json`，含 SHA256 完整性校验

---

## Controller ↔ FXML 映射表

### 直接绑定（FXML 中声明 `fx:controller`）

| FXML 文件 | Controller 类 |
|---|---|
| `fxml/mainPage.fxml` | `controller.MainPage` |
| `fxml/about.fxml` | `controller.About` |
| `fxml/eggPlant.fxml` | `controller.EggPlant` |
| `fxml/languageChooser.fxml` | `controller.languageChooser` |
| `fxml/loadHistoryHomework.fxml` | `controller.LoadHistoryHomework` |
| `fxml/historyHomeworkChecker.fxml` | `controller.HistoryHomeworkChecker` |
| `fxml/settings/index.fxml` | `controller.settings.Index` |
| `fxml/settings/homeworkArea.fxml` | `controller.settings.HomeworkArea` |
| `fxml/settings/initTemplateEditor.fxml` | `controller.settings.InitTemplateEditor` |
| `fxml/settings/updater.fxml` | `controller.settings.Updater` |
| `fxml/settings/reset.fxml` | `controller.settings.ResetThings` |
| `fxml/settings/dataBaseEditor.fxml` | `controller.settings.DataBaseEditor` |
| `fxml/setupWizard.fxml` | `controller.setupWizard.SetupWizardController` |
| `fxml/settings/about.fxml` | `controller.About` |

### 无 `fx:controller`（被其他 Controller 动态加载，无需独立控制器）

| FXML 文件 | 由谁加载 | 用途 |
|---|---|---|
| `fxml/cardItem.fxml` | `CardUiService.java` | 卡片项目组件 (动态渲染) |
| `fxml/updateWhat.fxml` | `About.java:36` | 更新内容展示弹窗 |
| `fxml/openSourceLicence.fxml` | `About.java:65` | 开源许可证弹窗 |
| `fxml/setupWizard/welcome.fxml` | `SetupWizardController.java` | 向导第1步：欢迎 |
| `fxml/setupWizard/language.fxml` | `SetupWizardController.java` | 向导第2步：语言 |
| `fxml/setupWizard/fontSettings.fxml` | `SetupWizardController.java` | 向导第3步：字体 |
| `fxml/setupWizard/initialTemplate.fxml` | `SetupWizardController.java` | 向导第4步：初始模板 |
| `fxml/setupWizard/finish.fxml` | `SetupWizardController.java` | 向导第5步：完成 |

### 动态加载关系（Controller → FXML）

| 加载方 | 加载的 FXML | 注入/显示方式 | 用途 |
|---|---|---|---|
| `Entry.java:56` | `fxml/setupWizard.fxml` | `Scene` → `Stage.show()` | 首次运行向导窗口 |
| `Entry.java:84` | `fxml/mainPage.fxml` | `Scene` → `Stage.show()` | 主窗口 |
| `TopButtonService.SettingsService` | `fxml/settings/index.fxml` | `PopupService.showPopup()` → `centerShowingArea` | 设置弹窗（含缩放按钮） |
| `TopButtonService.HistoryHomeworkService` | `fxml/loadHistoryHomework.fxml` | `PopupService.showPopup()` → `centerShowingArea` | 历史作业弹窗 |
| `TopButtonService.AboutService` | `fxml/about.fxml` | `PopupService.showPopup()` → `centerShowingArea` | 关于弹窗 |
| `Index.java:96` | `fxml/settings/homeworkArea.fxml` | `rightShowingArea.setContent()` | 设置→字体/字号页 |
| `Index.java:132` | `fxml/languageChooser.fxml` | `rightShowingArea.setContent()` | 设置→语言页 |
| `Index.java:169` | `fxml/historyHomeworkChecker.fxml` | `rightShowingArea.setContent()` | 设置→初始模板页 |
| `Index.java:242` | `fxml/settings/dataBaseEditor.fxml` | `rightShowingArea.setContent()` | 设置→数据库编辑器 |
| `Index.java:276` | `fxml/settings/reset.fxml` | `rightShowingArea.setContent()` | 设置→重置页面 |
| `Index.java:310` | `fxml/settings/updater.fxml` | `rightShowingArea.setContent()` | 设置→软件更新页 |
| `Index.java:369` | `fxml/settings/about.fxml` | `rightShowingArea.setContent()` | 设置→关于页面 |
| `LoadHistoryHomework.java:183` | `fxml/historyHomeworkChecker.fxml` | 独立 `Stage`（模态） | 历史作业详情窗口 |
| `About.java:36` | `fxml/updateWhat.fxml` | 独立 `Stage`（模态） | 更新内容 |
| `About.java:65` | `fxml/openSourceLicence.fxml` | 独立 `Stage`（模态） | 开源许可证 |
| `About.java:121` | `fxml/eggPlant.fxml` | 独立 `Stage`（模态） | 彩蛋游戏窗口 |
