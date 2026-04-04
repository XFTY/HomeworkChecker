@echo off

java --module-path "javafx-sdk-25.0.1\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED -jar "homeworkChecker-1.5.0 - snapshot-shaded.jar"

pause