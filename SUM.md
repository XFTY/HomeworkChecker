# HomeworkChecker 项目总结

## 概述

JavaFX 桌面应用，帮助教师在教室展示作业。功能：作业隐式保存、历史查询、一键截屏、锁定防误触、警示卡片、云同步（含连接测试）。版本 `1.7.0-beta`，Maven 构建，目标 Windows x64（NSIS）与 Linux x86_64（jpackage `.deb`）。

**多语言仅维护 3 种**：简体中文 `zh_CN`、繁体中文 `zh_HK`、英语 `en_US`。其余 8 种（es/fr/ar/ru/bn/de/ja/pt）`.properties` 保留但 UI 已隐藏，不再新增或维护翻译。

> 项目结构请用 codegraph MCP 获取；若无 `.codegraph/`，停止作业并提示用户安装。

---

## 依赖栈

JavaFX 25.0.1（controls/fxml/web）、FastJSON 2.0.47、OkHttp 4.12.0、CommonMark 0.21.0、Logback 1.5.13、Commons IO 2.15.1、JUnit 5.10.0

---

## 架构

**MVC + Service Layer**：

- **Model**：`Idf`（全局状态池，静态变量共享配置/语言包/缓存）
- **View**：24 FXML + 39 主题 CSS（darkness 默认）
- **Controller**：`controller/` 处理 FXML 事件与 UI 状态
- **Service**：`service/` 封装业务逻辑，`service/ui/` 按模块分子包
- **持久化**：JSON 文件数据库，位于 `~/homeworkChecker/`

---

## 核心数据流

```
Entry.main()
  ├─ 首次运行 → SetupWizard(5步) → FileInitManager.initializeFirstRun()
  └─ 正常启动 → FileInitManager.initializeUserDirectories()
                → 读 config.json / initTemple.txt / config/language.json 到 Idf
                → SingletonInstanceManager(文件锁+WatchService 防多开) → MainPage

MainPage:
  编辑作业 → LockService.unlock() → 编辑 → LockService.lock()(缓存+写库)
  自动保存看门狗: EditStateService 每5s检测, 30s无变化 → LockService.lock()
  警示卡片: CardUiService.onAddCard → ReminderCardService.addCard(写库+persistentCards.json)
            → CardUiService.renderCard; hover 编辑/删除 → ReminderCardService 持久化
  历史查询: LoadHistoryHomework → WeekdayCalculatorService → HomeworkContentFetcherService
            → HomeworkDatabase → HistoryHomeworkChecker 展示
  设置: 字体/字号/图片 → HomeworkArea(Service) → HomeworkDatabase.updateConfig() → config.json
        初始模板 → InitTemplateEditor → changeInitTemple() → initTemple.txt
        语言切换 → languageChooser → changeLanguage() → language.json
        更新 → UpdaterService → HttpClientService(GitHub API) → 下载 .msi → msiexec
        重置 → ResetThings → Idf.needHomeworkShowingAreaClear
```

---

## 关键文件

| 文件 | 职责/依赖 |
|------|-----------|
| `Entry.java` | 启动入口；依赖 `FileInitManager`、`SingletonInstanceManager`、`MainPage`、`SetupWizardController` |
| `Idf.java` | 全局引用池，被所有 controller/service 引用 |
| `MainPage.java` | 主控；依赖约 11 个服务（`HomeworkDatabase`、`MainPageInitService`、`TopButtonService.*`、`PopupService`、`EditMainService`、`EditStateService`、`WindowListener`、`CardUiService`、`LockService`、`ReminderCardService`）；管理 cardContainer 折叠（divider ≥0.90 时 `setManaged(false)`） |
| `HomeworkDatabase.java` | 核心数据服务，被 `MainPage`/`HomeworkArea`/`MainPageInitService`/`HomeworkContentFetcherService`/`Index`/`languageChooser`/`LockService` 使用 |
| `TopButtonService.java` | 含 4 内部类：ScreenshotService、HistoryHomeworkService、SettingsService、AboutService |
| `CardUiService.java` | 卡片 UI 核心（~1534 行），依赖 `ReminderCardService`/`PopupService`/`CardItem`；卡片增删改渲染折叠 |
| `service/ui/motion/MotionEasing.java` | 统一类 iOS 弹簧缓动曲线（`SPRING`/`SPRING_EXIT`），复用于卡片、弹窗、设置窗口与欢迎页的位移/缩放/尺寸动画 |
| `LockService.java` | 锁定/解锁；依赖 `HomeworkDatabase`、`Idf` |
| `ReminderCardService.java` | 卡片 CRUD + SHA256 校验 + persistentCards.json 同步 |
| `UpdaterService.java` | 依赖 `HttpClientService`、OkHttp 下载、CommonMark 渲染；内部 `generateUpdateInfoHtml()` 替代旧 `HtmlBuilder` |
| `SetupWizardController.java` | 5 步向导，依赖 `Entry` 加载子步骤 FXML |
| `Settings/Index.java` | 设置面板导航，动态加载子页面到右侧（含 about.fxml） |
| `HomeworkArea(Service).java` | 字体/字号/图片卡片设置，封装配置读写与 `updateConfig()` 持久化 |
| `LoadHistoryHomework.java` | 依赖 `WeekdayCalculatorService`/`HomeworkContentFetcherService`/`ButtonStateManagerService` |
| `DataBaseEditor(Service).java` | 数据库编辑器：列表渲染、查看/删除（含图片清理）、大小计算、retentionDays 持久化 |
| `CloudSync.java` | 云同步设置页：总开关 + 服务器连接测试（OkHttp，复用截屏成功动画反馈） |

---

## 配置文件目录（`~/homeworkChecker/`）

首次启动由 `FileInitManager` 创建：

```
config.json              # 用户配置(字体/字号/自动化清理)
initTemple.txt           # 初始作业模板
config/language.json     # 语言设置
homeworkDatabase/        # 作业数据库(YYYYMMDD, JSON含SHA256)
progress.lock            # 单例文件锁(运行时)
repeatedly.start         # 激活信号文件(临时)
```

---

## 关键特性

- **单例运行**：文件锁防多开；WatchService 监听激活信号使已有窗口前置
- **自动隐式保存**：看门狗每 5s 检测，30s 无变化自动保存并锁定
- **SHA256 校验**：数据库每条记录携带 `dataSHA256`
- **一键截图**：Canvas 渲染文本为图片并存入剪贴板
- **自动更新**：检测 GitHub Releases，下载 .msi 经 msiexec 静默安装（Windows 专属）
- **多语言**：ResourceBundle + properties，仅维护 zh_CN/zh_HK/en_US
- **首次向导**：5 步设置语言/字体/模板，含动画
- **彩蛋**：About 页面快速连点触发 EggPlant 游戏
- **自动缩进**：编辑区 Enter 保持当前行缩进
- **可折叠卡片容器**：SplitPane 分隔线 ≥ 9:1 自动折叠，拖回恢复（`setManaged` 控制）
- **警示卡片**：三种严重程度（提示/警告/严重），彩色边框+图标，hover 内联编辑/删除（编辑展开与删除收起均为动画），删除前在卡片内播放模糊变暗确认层（标题/说明/红灰按钮渐显，取消可逆），持久化至作业库 + `persistentCards.json`

---

## Controller ↔ FXML 映射

**直接绑定（FXML 内 `fx:controller`）**：`mainPage`→MainPage；`about`/`settings/about`→About；`eggPlant`→EggPlant；`languageChooser`→languageChooser；`loadHistoryHomework`、`historyHomeworkChecker`；`settings/{index,homeworkArea,initTemplateEditor,updater,reset,dataBaseEditor,cloudsync}`；`setupWizard`→SetupWizardController。

**无独立控制器（被动态加载）**：`cardItem`（CardUiService）、`updateWhat`/`openSourceLicence`（About）、`setupWizard/{welcome,language,fontSettings,initialTemplate,finish}`（SetupWizardController）。

**动态加载（加载方 → FXML → 显示方式）**：

| 加载方 | FXML | 显示方式 |
|---|---|---|
| `Entry` | `setupWizard` / `mainPage` | 独立 `Stage` |
| `TopButtonService.{Settings,HistoryHomework,About}Service` | `settings/index`、`loadHistoryHomework`、`about` | `PopupService.showPopup()`→`centerShowingArea` |
| `Index` | `settings/{homeworkArea,dataBaseEditor,reset,updater,about,cloudsync}`、`languageChooser`、`historyHomeworkChecker` | `rightShowingArea.setContent()` |
| `LoadHistoryHomework` | `historyHomeworkChecker` | 独立模态 `Stage` |
| `About` | `updateWhat`、`openSourceLicence`、`eggPlant` | 独立模态 `Stage` |

---

## Linux .deb 打包

`build-deb.sh`（JDK 25 的 `jpackage`）：

```bash
./build-deb.sh                         # 或 JAVAFX_JMODS=/path ./build-deb.sh
# 产物：package/homeworkchecker_<version>_amd64.deb
```

**构建机依赖**：JDK 25（含 jpackage/jlink）、`dpkg-deb`、`fakeroot`、JavaFX jmods（下载解压到仓库根，目录名 `javafx-jmods-<ver>`，脚本自动探测）。

**要点**：
- `linux-deb` profile 的 shade 排除 `org.openjfx:*`，JavaFX 由 jmods 以模块进入 jlink 镜像，避免 `libjfxwebkit.so` 重复打包（67MB → 10MB）。
- 以**非模块化 fat jar** 运行（`--main-jar`）：依赖含 automatic module，jlink 不支持 `--module`；JavaFX 用 `--add-modules` 运行时解析。
- 版本号从 pom 解析：`1.7.0-beta2` → `--app-version 1.7.0` + `--linux-app-release beta2`。
- jpackage 自动扫描原生库生成依赖（GTK/X11 等 `*-t64`），脚本仅补 `fonts-noto-cjk`。
- 安装到 `/opt/homeworkchecker`，启动器 `/opt/homeworkchecker/bin/HomeworkChecker`，创建 `Education` 菜单项。

**适配注意**：自动更新为 Windows 专属（非 Windows 调 `onUnsupportedPlatform()` 提示）；用户数据目录用 `user.home`，跨平台一致。
