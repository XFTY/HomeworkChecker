@echo off

"jdk-25.0.2+10\bin\java.exe" --module-path "javafx-sdk-25.0.2\lib" --add-modules javafx.controls,javafx.fxml,javafx.web --enable-native-access=javafx.graphics -jar "homeworkChecker-1.7.0-beta-shaded.jar"