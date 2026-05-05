module com.xfty.homeworkchecker {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires com.alibaba.fastjson2;
    requires java.desktop;
    requires fastjson;
    requires okhttp3;
    requires org.commonmark;


    opens com.xfty.homeworkchecker to javafx.fxml;
    opens com.xfty.homeworkchecker.controller to javafx.fxml;
    exports com.xfty.homeworkchecker;
    opens com.xfty.homeworkchecker.controller.settings to javafx.fxml;
    opens com.xfty.homeworkchecker.controller.setupWizard to javafx.fxml;
}