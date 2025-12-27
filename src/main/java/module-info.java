module com.xfty.homeworkchecker {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires com.alibaba.fastjson2;
    requires java.desktop;


    opens com.xfty.homeworkchecker to javafx.fxml;
    opens com.xfty.homeworkchecker.controller to javafx.fxml;
    exports com.xfty.homeworkchecker;
}