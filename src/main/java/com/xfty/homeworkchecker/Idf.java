package com.xfty.homeworkchecker;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.controller.MainPage;
import com.xfty.homeworkchecker.service.SingletonInstanceManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Idf {
    public static String softwareVersion = "1.7.0 - Beta";
    public static boolean UpdaterTestMode = false;

    public static String year;
    public static String month;
    public static String day;
    public static String weekdays;

    public static Double uiFontOverflow;
    public static Double editMainFontSize;

    public static boolean isEditable = false;
    public static final BooleanProperty editableProperty = new SimpleBooleanProperty(false);

    
    public static boolean isMainPageMaximized = false;
    // 检测是否正在显示前置窗口
    public static boolean isPreviewWindowShowing = false;

    public static JSONObject userConfig;
    public static String homeworkContextCache;
    public static List<String> fontFamilies = Font.getFamilies();

    public static boolean isSoftwareClosing;

    // 添加initTemple和userLanguage变量
    public static volatile String initTemple;
    public static JSONObject userLanguage;

    public volatile static ResourceBundle userLanguageBundle;

    public static Stage primaryStage;
    public static MainPage mainPageController;

    public static void reloadLanguageBundle(String languageCode) {
        String[] languageParts = languageCode.split("_");
        Locale locale;
        if (languageParts.length == 2) {
            locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
        } else {
            locale = new Locale.Builder().setLanguage(languageParts[0]).build();
        }
        userLanguageBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale);
    }

    public static volatile boolean needHomeworkShowingAreaClear = false;

    public static final List<String> cuteWarningsIdf = List.of(
            "点我 o((>ω< ))o !!!!!!",
            "为什么不看看我 (っ °Д °;)っ",
            "解锁后才能编辑哦 (๑•̀ω•́๑)✧",
            "记得点击解锁按钮 ♪(^∇^*)",
            "先解锁再编辑 \\(≧▽≦)/",
            "编辑前请先解锁 (｡•̀ᴗ-)✧",
            "解锁按钮在我左边哦 (ง •_•)ง",
            "点击锁头解锁编辑功能 (๑•̀ω•́๑)✧",
            "点一下锁头就可以编辑啦 (◕‿◕)✨",
            "解锁按钮在锁头上，点它 (,,•́ . •̀,,)",
            "点击锁头图标解锁编辑 (๑• . •๑)♡",
            "解锁才能编辑，点锁头哦 \\(≧▽≦)/",
            "点锁头解锁，开始编辑吧 ٩(๑>◡<๑)۶",
            "编辑前请先点击锁头解锁 (｡•̀ᴗ-)✧",
            "锁头一点，编辑无限 (๑•̀ω•́๑)✧",
            "解锁编辑请点击锁头 (◕‿◕)✨",
            "点锁头，解封编辑功能 (,,•́ . •̀,,)",
            "点击锁头即可编辑内容 (๑• . •๑)♡",
            "编辑需要先解锁，点锁头 (ง'-'︻╦╤─",
            "解锁按钮就是这个锁头 (๑>◡<๑)♡",
            "点一下锁头，开启编辑模式 \\(≧▽≦)/",
            "点击锁头解锁编辑权限 (｡•̀ᴗ-)✧",
            "编辑功能被锁住了，点锁头解锁 (๑•̀ω•́๑)✧",
            "点锁头，释放编辑潜能 (◕‿◕)✨",
            "解锁编辑功能请点这里 (,,•́ . •̀,,)",
            "点击锁头，解除编辑限制 (๑• . •๑)♡",
            "编辑前必须解锁，点锁头 (ง'-'︻╦╤─",
            "点锁头按钮进行解锁 (๑>◡<๑)♡",
            "点击锁头图标开始编辑 \\(≧▽≦)/",
            "解锁后方可编辑，点锁头 (｡•̀ᴗ-)✧",
            "编辑需要解锁，点锁头吧 (๑•̀ω•́๑)✧",
            "点一下锁头就能编辑啦 (◕‿◕)✨",
            "点击锁头解锁，畅快编辑 (,,•́ . •̀,,)",
            "编辑功能在此，点锁头解锁 (๑• . •๑)♡",
            "点锁头，开启创作之旅 (ง'-'︻╦╤─",
            "解锁编辑请点击锁头图标 (๑>◡<๑)♡",
            "点一下锁头，进入编辑状态 \\(≧▽≦)/",
            "点击解锁按钮（锁头）开始编辑 (｡•̀ᴗ-)✧",
            "编辑前请解锁，点锁头 (๑•̀ω•́๑)✧",
            "点锁头解锁编辑 (◕‿◕)✨",
            "点击锁头，编辑大门为你敞开 (,,•́ . •̀,,)",
            "解锁按钮即锁头图标 (๑• . •๑)♡",
            "点锁头开始编辑内容 (ง'-'︻╦╤─",
            "点击锁头解锁编辑模式 (๑>◡<๑)♡",
            "编辑需解锁，点锁头操作 \\(≧▽≦)/",
            "点一下锁头解锁 (｡•̀ᴗ-)✧",
            "点击锁头，解除编辑封锁 (๑•̀ω•́๑)✧",
            "解锁编辑功能在此 (◕‿◕)✨",
            "点锁头按钮开始编辑 (,,•́ . •̀,,)",
            "点击锁头图标进行解锁 (๑• . •๑)♡",
            "编辑前要点锁头解锁哦 (ง'-'︻╦╤─",
            "点锁头，释放编辑力 (๑>◡<๑)♡",
            "点击解锁编辑功能 \\(≧▽≦)/",
            "解锁后才能编辑，点锁头 (｡•̀ᴗ-)✧",
            "点一下锁头开启编辑 (๑•̀ω•́๑)✧",
            "点击锁头进行解锁操作 (◕‿◕)✨",
            "编辑权限需解锁，点锁头 (,,•́ . •̀,,)"
    );

    // 下面这些变量是给一些彩蛋的
    public static int iconClickedCount = 0;
    public static boolean isAboutIconTriggered = false;
    public static boolean isAboutIconTriggeredAgain = false;
    
    // 看门狗自动锁定空闲超时（秒）
    public static int watchdogIdleTimeoutSeconds = 45;

    // 单例管理器引用
    public static SingletonInstanceManager singletonManager;
    
    // 设置窗口关闭按钮引用（用于更新期间禁用）
    public static volatile javafx.scene.shape.Circle settingsWindowCloseButton;
}