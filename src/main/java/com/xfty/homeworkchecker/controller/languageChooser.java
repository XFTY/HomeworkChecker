package com.xfty.homeworkchecker.controller;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class languageChooser implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(languageChooser.class);
    
    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();
    
    // 定义各种语言的提示文本
    private final Map<String, String[]> languageMessages = new HashMap<>();
    
    @FXML
    private AnchorPane chineseButton;
    
    @FXML
    private AnchorPane traditionalChineseButton;
    
    @FXML
    private AnchorPane englishButton;
    
    @FXML
    private AnchorPane spanishButton;
    
    @FXML
    private AnchorPane frenchButton;
    
    @FXML
    private AnchorPane arabicButton;
    
    @FXML
    private AnchorPane russianButton;
    
    @FXML
    private AnchorPane bengaliButton;
    
    @FXML
    private AnchorPane germanButton;
    
    @FXML
    private AnchorPane japaneseButton;
    
    @FXML
    private AnchorPane portugueseButton;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化各语言的提示信息
        initializeLanguageMessages();
        
        // 为所有语言按钮添加点击事件监听器
        chineseButton.setOnMouseClicked(event -> {
            handleLanguageSelection("zh_CN");
        });
        
        traditionalChineseButton.setOnMouseClicked(event -> {
            handleLanguageSelection("zh_HK");
        });
        
        englishButton.setOnMouseClicked(event -> {
            handleLanguageSelection("en_US");
        });
        
        spanishButton.setOnMouseClicked(event -> {
            handleLanguageSelection("es_ES");
        });
        
        frenchButton.setOnMouseClicked(event -> {
            handleLanguageSelection("fr_FR");
        });
        
        arabicButton.setOnMouseClicked(event -> {
            handleLanguageSelection("ar_SA");
        });
        
        russianButton.setOnMouseClicked(event -> {
            handleLanguageSelection("ru_RU");
        });
        
        bengaliButton.setOnMouseClicked(event -> {
            handleLanguageSelection("bn_BD");
        });
        
        germanButton.setOnMouseClicked(event -> {
            handleLanguageSelection("de_DE");
        });
        
        japaneseButton.setOnMouseClicked(event -> {
            handleLanguageSelection("ja_JP");
        });
        
        portugueseButton.setOnMouseClicked(event -> {
            handleLanguageSelection("pt_PT");
        });
    }
    
    private void initializeLanguageMessages() {
        // 简体中文 [确认标题, 确认头, 确认内容, 退出标题, 退出头, 退出内容]
        languageMessages.put("zh_CN", new String[]{
            "确认语言选择", 
            "您选择了简体中文", 
            "是否要保存此语言选择？", 
            "退出应用程序", 
            "简体中文已设置", 
            "是否要退出应用程序以应用更改？"
        });
        
        // 繁体中文
        languageMessages.put("zh_HK", new String[]{
            "確認語言選擇", 
            "您選擇了繁體中文", 
            "是否要保存此語言選擇？", 
            "退出應用程序", 
            "繁體中文已設置", 
            "是否要退出應用程序以應用更改？"
        });
        
        // 英文
        languageMessages.put("en_US", new String[]{
            "Confirm Language Selection", 
            "You have selected English", 
            "Do you want to save this language selection?", 
            "Exit Application", 
            "Language changed to English", 
            "Do you want to exit the application to apply the changes?"
        });
        
        // 西班牙文
        languageMessages.put("es_ES", new String[]{
            "Confirmar selección de idioma", 
            "Ha seleccionado español", 
            "¿Desea guardar esta selección de idioma?", 
            "Salir de la aplicación", 
            "El idioma se ha cambiado a español", 
            "¿Desea salir de la aplicación para aplicar los cambios?"
        });
        
        // 法文
        languageMessages.put("fr_FR", new String[]{
            "Confirmer la sélection de langue", 
            "Vous avez sélectionné français", 
            "Voulez-vous enregistrer cette sélection de langue ?", 
            "Quitter l'application", 
            "La langue a été changée en français", 
            "Voulez-vous quitter l'application pour appliquer les changements ?"
        });
        
        // 阿拉伯文
        languageMessages.put("ar_SA", new String[]{
            "تأكيد اختيار اللغة", 
            "لقد اخترت العرب", 
            "هل تريد حفظ اختيار اللغة هذا؟", 
            "الخروج من التطبيق", 
            "تم تغيير اللغة إلى العرب", 
            "هل تريد الخروج من التطبيق لتطبيق التغييرات؟"
        });
        
        // 俄文
        languageMessages.put("ru_RU", new String[]{
            "Подтвердить выбор языка", 
            "Вы выбрали русский язык", 
            "Вы хотите сохранить этот выбор языка?", 
            "Выйти из приложения", 
            "Язык изменен на русский", 
            "Вы хотите выйти из приложения, чтобы применить изменения?"
        });
        
        // 孟加拉文
        languageMessages.put("bn_BD", new String[]{
            "ভাষা নির্বাচন নিশ্চিত করুন", 
            "আপনি বাংলা নির্বাচন করেছেন", 
            "আপনি কি এই ভাষা নির্বাচনটি সংরক্ষণ করতে চান?", 
            "অ্যাপ্লিকেশন থেকে প্রস্থান করুন", 
            "ভাষা বাংলায় পরিবর্তন করা হয়েছে", 
            "পরিবর্তনগুলি প্রয়োগ করতে আপনি কি অ্যাপ্লিকেশন থেকে প্রস্থান করতে চান?"
        });
        
        // 德文
        languageMessages.put("de_DE", new String[]{
            "Sprachauswahl bestätigen", 
            "Sie haben Deutsch ausgewählt", 
            "Möchten Sie diese Sprachauswahl speichern?", 
            "Anwendung beenden", 
            "Die Sprache wurde auf Deutsch geändert", 
            "Möchten Sie die Anwendung beenden, um die Änderungen zu übernehmen?"
        });
        
        // 日文
        languageMessages.put("ja_JP", new String[]{
            "言語選択の確認", 
            "日本語が選択されました", 
            "この言語設定を保存しますか？", 
            "アプリケーションを終了", 
            "言語が日本語に変更されました", 
            "変更を適用するためにアプリケーションを終了しますか？"
        });
        
        // 葡萄牙文
        languageMessages.put("pt_PT", new String[]{
            "Confirmar seleção de idioma", 
            "Você selecionou português", 
            "Deseja salvar esta seleção de idioma?", 
            "Sair do aplicativo", 
            "O idioma foi alterado para português", 
            "Deseja sair do aplicativo para aplicar as alterações?"
        });
    }
    
    private void handleLanguageSelection(String languageCode) {
        String[] messages = languageMessages.get(languageCode);
        String languageName = getLanguageName(languageCode);
        
        // 显示确认对话框确认语言选择
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(messages[0]); // 确认标题
        confirmAlert.setHeaderText(messages[1]); // 确认头
        confirmAlert.setContentText(messages[2]); // 确认内容
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.info(languageName + " language selected");
            JSONObject languageJson = new JSONObject();
            languageJson.put("language", languageCode);
            homeworkDatabase.changeLanguage(languageJson);
            
            // 询问用户是否要退出软件以应用更改
            Alert exitAlert = new Alert(Alert.AlertType.CONFIRMATION);
            exitAlert.setTitle(messages[3]); // 退出标题
            exitAlert.setHeaderText(messages[4]); // 退出头
            exitAlert.setContentText(messages[5]); // 退出内容
            
            Optional<ButtonType> exitResult = exitAlert.showAndWait();
            if (exitResult.isPresent() && exitResult.get() == ButtonType.OK) {
                // 退出软件
                System.exit(0);
            }
        }
    }
    
    private String getLanguageName(String languageCode) {
        switch (languageCode) {
            case "zh_CN": return "简体中文";
            case "zh_HK": return "繁體中文";
            case "en_US": return "English";
            case "es_ES": return "español";
            case "fr_FR": return "français";
            case "ar_SA": return "العرب";
            case "ru_RU": return "русский язык";
            case "bn_BD": return "বাংলা";
            case "de_DE": return "Deutsch";
            case "ja_JP": return "日本語";
            case "pt_PT": return "português";
            default: return "Unknown";
        }
    }
    
    private void closeWindow() {
        // 关闭当前窗口
        Stage stage = (Stage) chineseButton.getScene().getWindow();
        stage.close();
    }
}