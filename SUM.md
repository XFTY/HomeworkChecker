# HomeworkChecker 项目总结

## 项目概述

**HomeworkChecker** 是一款基于 JavaFX 的桌面应用程序，旨在帮助教师在教室内展示作业内容。支持多语言（11种语言）、作业记录隐式保存、历史作业查询、一键截屏、锁定防误触机制等功能。版本 `1.7.0-beta`，采用 Maven 构建，目标平台为 Windows 64位。

---

## 项目结构

```
HomeworkChecker/
├── pom.xml                          # Maven 构建 (Java 25, JavaFX 25.0.1)
├── module-info.java                 # Java模块声明
│
├── src/main/java/
│   └── com/xfty/homeworkchecker/
│       ├── Entry.java               # 应用入口 (JavaFX Application.launch)
│       ├── Idf.java                 # 全局静态配置/状态常量池
│       │
│       ├── model/                   # 数据模型
│       │   └── CardItem.java            # 警示卡片模型 (严重程度/标题/内容/时间戳)
│       │
│       ├── controller/              # MVC 控制器层 (UI交互逻辑)
│       │   ├── MainPage.java            # 主页面控制器 (核心界面)
│       │   ├── Settings.java            # 设置页控制器 (字体/字号/初始模板)
│       │   ├── About.java               # 关于页面控制器 (含彩蛋游戏)
│       │   ├── EggPlant.java            # 彩蛋页面 (空控制器)
│       │   ├── languageChooser.java     # 语言选择控制器 (11种语言)
│       │   ├── LoadHistoryHomework.java # 历史作业加载对话框控制器
│       │   ├── HistoryHomeworkChecker.java # 历史作业展示窗口控制器
│       │   ├── setupWizard/
│       │   │   └── SetupWizardController.java # 首次运行向导 (5步, 含动画)
│   │       └── settings/
│   │           ├── Index.java           # 设置面板索引 (左侧导航栏+右侧内容区切换)
│   │           ├── Updater.java         # 更新管理控制器
│   │           └── ResetThings.java     # 重置作业控制器
│   │
│       ├── service/                 # 服务层 (业务逻辑)
│       │   ├── FileInitManager.java         # 首次运行初始化 & 目录/配置文件管理
│       │   ├── SingletonInstanceManager.java # 单例锁 (文件锁 + WatchService 激活)
│       │   ├── DatabaseManager.java         # 数据库文件名扫描
│       │   ├── HomeworkDatabase.java        # 作业数据库 CRUD (SHA256 校验)
│       │   ├── ui/
│       │   │   ├── mainPage/
│       │   │   │   ├── MainPageInitService.java  # 主页初始化 (加载当日/周末作业)
│       │   │   │   ├── TopButtonService.java     # 顶部按钮服务 (截图/历史/设置/关于)
│       │   │   │   ├── EditMainService.java      # 编辑区逻辑 (可爱模式/抖动/自动缩进)
│   │   │   │   ├── EditStateService.java     # 编辑状态看门狗 (自动锁定计时器)
│   │   │   │   ├── PopupService.java         # 弹窗管理 (高斯模糊/缩放动画)
│   │   │   │   ├── ReminderCardService.java  # 警示卡片数据 CRUD + 持久化 + SHA256
│   │   │   │   ├── CardUiService.java        # 卡片 UI 渲染与交互 (添加/编辑/删除/持久化, 756行)
│   │   │   │   ├── LockService.java          # 锁定/解锁模块 (更新图标/缓存内容/写入数据库)
│   │   │   │   └── WindowListener.java       # 窗口状态监听 (最大化/Ctrl+~快捷键)
│       │   │   ├── loadHistoryHomework/
│       │   │   │   ├── WeekdayCalculatorService.java    # 工作日计算器
│       │   │   │   ├── HomeworkContentFetcherService.java # 作业内容批量获取
│       │   │   │   └── ButtonStateManagerService.java   # 按钮状态管理 (绿灯/红灯)
│       │   │   └── settings/
│       │   │       └── UpdaterService.java  # 更新服务 (版本检查/下载/安装)
│       │   └── updater/
│       │       ├── HttpClientService.java    # GitHub API HTTP 客户端
│       │       ├── JSONParsingService.java   # GitHub Release JSON 解析
│       │       └── HtmlBuilder.java          # Markdown→HTML 暗色主题转换 (未被引用, 功能被 UpdaterService.generateUpdateInfoHtml() 替代)
│       │
│       └── resources/                # 资源文件
│           ├── fxml/                 # 23个 FXML 界面布局文件
│           │   ├── mainPage.fxml             # 主界面 (SplitPane 左: editMain, 右: cardContainer)
│           │   ├── settings.fxml             # 设置窗口
│           │   ├── about.fxml / updateWhat.fxml / openSourceLicence.fxml
│           │   ├── languageChooser.fxml      # 语言选择
│           │   ├── loadHistoryHomework.fxml / historyHomeworkChecker.fxml
│           │   ├── cardItem.fxml             # 警示卡片项目组件 (由CardUiService动态加载)
│           │   ├── setupWizard.fxml          # 首次运行向导壳
│           │   ├── eggPlant.fxml             # 彩蛋页面
│           │   ├── setupWizard/             # 向导子步骤 (5个)
│           │   │   ├── welcome.fxml / language.fxml / fontSettings.fxml
│           │   │   ├── initialTemplate.fxml / finish.fxml
│           │   └── settings/                # 设置子页面 (7个)
│           │       ├── index.fxml / homeworkArea.fxml / homeworkArea-back.fxml
│   │       ├── dataBaseEditor.fxml / reset.fxml / updater.fxml
│           │
│           ├── theme/                 # 主题 CSS (3套 × 13个 = 39个文件)
│           │   ├── darkness/               # 暗色主题 (默认)
│           │   ├── light/                  # 亮色主题
│           │   ├── paper/                  # 纸张质感主题
│           │   └── darker/                 # (空目录, 预留)
│           │
│           │   darkness/、light/、paper/ 各自含:
│           │   ├── mainPage.css
│           │   ├── checkbox.css / slider.css / choicebox.css / datepicker.css
│           │   ├── language-button.css
│           │   ├── textarea/text-area.css / text-area-test.css
│           │   ├── scroolPane/scroolPane.css
│           │   ├── button/functional-button.css / danger-button.css
│           │   ├── splitPane/main-split-pane.css
│           │   └── card/reminder-card.css (扁平现代填充色圆角卡片)
│           │
│           ├── i18n/                 # 国际化 (11个语言包)
│           │   ├── language_zh_CN.properties / zh_HK / en_US
│           │   ├── es_ES / fr_FR / de_DE / ja_JP / pt_PT
│           │   ├── ru_RU / ar_SA / bn_BD
│           │
│           ├── icon/                 # 图标资源 (21个PNG + 1个PSD)
│           │   ├── logo.png / logo-classic.png / close.png
│           │   ├── lock/lock.png / unlock.png
│           │   ├── light/green.png / red.png
│           │   ├── topbar/ (窗口控制按钮)
│           │   └── card/ (7个: 编辑/警告/删除/时间/时间-back/提示/严重)
│           │
│           ├── config/               # 配置模板
│           │   └── modelConfigV1.json
│           ├── modelConfig.json      # 默认配置模板
│           ├── modelDatabase.json    # 数据库模板
│           ├── githubApiTemple.json  # GitHub API 模拟JSON (测试用)
│           ├── language.json         # 默认语言配置
│           ├── initTemple.txt        # 默认初始模板
│           ├── log4j2.xml            # Log4j2 日志配置
│           └── logback.xml           # Logback 日志配置
│
├── docs/                            # 产品展示网页
│   ├── index.html                   # 营销落地页 (Vue3 + ElementPlus + Anime.js)
│   ├── style.css                    # 暗色液态玻璃风格 (1039行)
│   ├── script.js                    # 粒子背景/多语言/动画 (584行)
│   └── pic/                         # 截图 & 图标素材
│
├── .github/ISSUE_TEMPLATE/          # GitHub Issue 模板 (6个, 中英双版)
│
├── HomeworkChecker-cache/           # 安装缓存 (Advanced Installer)
├── HomeworkChecker-SetupFiles/      # 安装包 (MSI)
├── HomeworkChecker.aip              # Advanced Installer 项目文件
├── HomeworkCheckerLauncher.vbs      # VBS 启动器
├── app.bat                          # CMD 启动脚本
├── updater.bat                      # 更新脚本
├── logo.ico                         # 应用图标
│
├── .idea/                           # IntelliJ IDEA 配置
├── .lingma/                         # Lingma AI 配置
├── cloud-sync/                      # 云同步功能设计文档（按模块拆分）
│   ├── server/                      # 服务端方案（Spring Boot）
│   │   ├── 01-overview.md               # 项目概述与技术栈
│   │   ├── 02-project-structure.md      # 项目包结构
│   │   ├── 03-data-model.md             # 数据模型与表设计
│   │   ├── 04-api-design.md             # API 接口设计
│   │   ├── 05-auth-system.md            # 认证与权限体系
│   │   ├── 06-web-frontend.md           # Web 前端设计（darkness主题）
│   │   ├── 07-conflict-resolution.md    # 冲突处理机制
│   │   ├── 08-security.md               # 安全加固方案
│   │   └── 09-deployment.md             # 部署方案
│   └── client/                      # 客户端修改方案（JavaFX 同步模块）
│       ├── 01-overview.md               # 概述与模块结构
│       ├── 02-config.md                 # 配置扩展与设置UI
│       ├── 03-sync-flow.md              # 推送/拉取/轮询/迁移流程
│       ├── 04-hmac-signing.md           # HMAC 签名模块
│       ├── 05-conflict-ui.md            # 冲突处理 UI
│       └── 06-existing-changes.md       # 现有代码修改点
├── .agents/                         # AI Agent 技能库 (39个技能文件)
├── .agents.7z                       # 技能库压缩包
├── skills-lock.json                 # 技能锁文件
│
├── AGENTS.md                        # AI Agent 构建/规则备忘录
├── .gitignore / LICENSE / Readme.md / Readme-cn.md
│
├── mvnw / mvnw.cmd                  # Maven Wrapper
└── logs/                            # 运行时日志文件
```

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
- **View**: 22 个 FXML 布局 + 39 个主题 CSS (darkness 默认主题)
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

设置 (Settings → Settings.Index):
   字体/字号调整 → HomeworkDatabase.updateConfig() → 写 config.json
   初始模板编辑 → HomeworkDatabase.changeInitTemple() → 写 initTemple.txt
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
| `HomeworkDatabase.java` | 核心数据服务，被 `MainPage`, `Settings`, `MainPageInitService`, `HomeworkContentFetcherService`, `Index`, `languageChooser`, `LockService` 等使用 |
| `TopButtonService.java` | 含4个内部类: ScreenshotService, HistoryHomeworkService, SettingsService, AboutService |
| `CardUiService.java` | 卡片 UI 核心 (756行)，依赖 `ReminderCardService`, `PopupService`, `CardItem`; 管理卡片添加/编辑/删除/渲染/折叠 |
| `LockService.java` | 锁定/解锁服务，依赖 `HomeworkDatabase`, `Idf`; 被 `MainPage` 调用 |
| `ReminderCardService.java` | 卡片数据 CRUD + SHA256 校验 + persistentCards.json 同步; 被 `CardUiService` 调用 |
| `UpdaterService.java` | 依赖 `HttpClientService` (GitHub API), OkHttp 下载, CommonMark 渲染 HTML; 内部 `generateUpdateInfoHtml()` 替代了 `HtmlBuilder` |
| `SetupWizardController.java` | 管理5步向导，依赖 `Entry` 加载子步骤 FXML |
| `Settings/Index.java` | 设置面板导航，动态加载设置子页面到右侧区域 |
| `LoadHistoryHomework.java` | 依赖 `WeekdayCalculatorService`, `HomeworkContentFetcherService`, `ButtonStateManagerService` |

---

## 配置文件目录 (用户目录/homeworkChecker/)

首次启动时由 `FileInitManager` 创建在 `%USERPROFILE%/homeworkChecker/` 下：
```
homeworkChecker/
├── config.json              # 用户配置 (字体/字号)
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
| `fxml/settings.fxml` | `controller.Settings` |
| `fxml/settings/index.fxml` | `controller.settings.Index` |
| `fxml/settings/homeworkArea.fxml` | `controller.Settings` |
| `fxml/settings/homeworkArea-back.fxml` | `controller.Settings` |
| `fxml/settings/updater.fxml` | `controller.settings.Updater` |
| `fxml/settings/reset.fxml` | `controller.settings.ResetThings` |
| `fxml/setupWizard.fxml` | `controller.setupWizard.SetupWizardController` |

### 无 `fx:controller`（被其他 Controller 动态加载，无需独立控制器）

| FXML 文件 | 由谁加载 | 用途 |
|---|---|---|
| `fxml/cardItem.fxml` | `CardUiService.java` | 卡片项目组件 (动态渲染) |
| `fxml/updateWhat.fxml` | `About.java:36` | 更新内容展示弹窗 |
| `fxml/openSourceLicence.fxml` | `About.java:65` | 开源许可证弹窗 |
| `fxml/settings/dataBaseEditor.fxml` | `Index.java:242` | 数据库编辑器占位页 |
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
| `Index.java:242` | `fxml/settings/dataBaseEditor.fxml` | `rightShowingArea.setContent()` | 设置→数据库编辑器占位页 |
| `Index.java:276` | `fxml/settings/reset.fxml` | `rightShowingArea.setContent()` | 设置→重置页面 |
| `Index.java:310` | `fxml/settings/updater.fxml` | `rightShowingArea.setContent()` | 设置→软件更新页 |
| `Settings.java:87` | `fxml/historyHomeworkChecker.fxml` | 独立 `Stage`（模态） | 初始模板编辑器（旧版） |
| `Settings.java:130` | `fxml/languageChooser.fxml` | 独立 `Stage`（模态） | 语言选择器（旧版） |
| `LoadHistoryHomework.java:183` | `fxml/historyHomeworkChecker.fxml` | 独立 `Stage`（模态） | 历史作业详情窗口 |
| `About.java:36` | `fxml/updateWhat.fxml` | 独立 `Stage`（模态） | 更新内容 |
| `About.java:65` | `fxml/openSourceLicence.fxml` | 独立 `Stage`（模态） | 开源许可证 |
| `About.java:121` | `fxml/eggPlant.fxml` | 独立 `Stage`（模态） | 彩蛋游戏窗口 |
