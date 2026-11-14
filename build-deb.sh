#!/usr/bin/env bash
#
# build-deb.sh — 在 Linux 上把 HomeworkChecker 打包成 .deb 安装包
#
# 依赖（构建机）：
#   - JDK 25（含 jpackage / jlink），Ubuntu: openjdk-25-jdk
#   - dpkg-deb、fakeroot（jpackage 生成 deb 所需）
#   - 本机 Maven（mvn）能解析项目依赖
#
# 依赖（JavaFX jmods）：
#   JavaFX 的 jmods 不在 Maven Central，需手动下载 Linux 版 jmods 包并解压：
#     https://download2.gluonhq.com/openjfx/<ver>/openjfx-<ver>_linux-x64_bin-jmods.zip
#   解压后放在仓库根目录（脚本会自动探测 javafx-jmods-*），
#   或用环境变量 JAVAFX_JMODS=/path/to/javafx-jmods 指定。
#
# 用法：
#   ./build-deb.sh
#   可用环境变量覆盖：JAVAFX_JMODS、DEB_MAINTAINER、VENDOR、PACKAGE_DEPS
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# ---- 可配置项（均可用环境变量覆盖）----
# 注意：jpackage 会通过解析运行时镜像里的原生库自动生成 GTK/X11 等系统依赖，
# 这里只需补充它无法自动检测的项（如中文字体）。
DEB_MAINTAINER="${DEB_MAINTAINER:-xfty@example.com}"
VENDOR="${VENDOR:-XFTY}"
PACKAGE_DEPS="${PACKAGE_DEPS:-fonts-noto-cjk}"
BUILD_DIR="$ROOT/.build"
OUT_DIR="$ROOT/package"
JAVA_MODULES="java.se,jdk.unsupported,jdk.crypto.ec,jdk.charsets,jdk.zipfs,javafx.controls,javafx.fxml,javafx.web"
JAVAFX_RUNTIME_MODULES="javafx.controls,javafx.fxml,javafx.web"

# ---- 1. 读取项目版本 ----
VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)"
if [[ -z "$VERSION" ]]; then
    echo "[build-deb] 错误：无法从 pom.xml 解析版本号" >&2
    exit 1
fi
APP_VERSION="${VERSION%%-*}"
if [[ "$VERSION" == *-* ]]; then
    DEB_RELEASE="${VERSION#*-}"
else
    DEB_RELEASE="1"
fi
echo "[build-deb] 项目版本: $VERSION  ->  app-version=$APP_VERSION  deb-release=$DEB_RELEASE"

# ---- 2. 构建 fat jar（linux-deb profile 会排除 JavaFX）----
echo "[build-deb] 构建 fat jar ..."
mvn -q -Plinux-deb -DskipTests clean package
MAIN_JAR="target/homeworkChecker-${VERSION}-shaded.jar"
if [[ ! -f "$MAIN_JAR" ]]; then
    echo "[build-deb] 错误：未找到 $MAIN_JAR" >&2
    exit 1
fi
if unzip -l "$MAIN_JAR" | grep -qE "javafx/|\.so$"; then
    echo "[build-deb] 错误：$MAIN_JAR 仍包含 JavaFX，请确认 linux-deb profile 生效" >&2
    exit 1
fi

# ---- 3. 定位 JavaFX jmods ----
if [[ -z "${JAVAFX_JMODS:-}" ]]; then
    for d in "$ROOT"/javafx-jmods-*/; do
        if [[ -f "${d}javafx.controls.jmod" ]]; then
            JAVAFX_JMODS="$d"
            break
        fi
    done
fi
if [[ -z "${JAVAFX_JMODS:-}" || ! -f "${JAVAFX_JMODS}/javafx.controls.jmod" ]]; then
    cat >&2 <<'EOF'
[build-deb] 错误：未找到 JavaFX jmods。
  请下载（约 53 MB）：
    https://download2.gluonhq.com/openjfx/25.0.1/openjfx-25.0.1_linux-x64_bin-jmods.zip
  解压到仓库根目录（目录名形如 javafx-jmods-<ver>），
  或设置环境变量 JAVAFX_JMODS=/path/to/javafx-jmods。
EOF
    exit 1
fi
JAVAFX_JMODS="$(cd "$JAVAFX_JMODS" && pwd)"
echo "[build-deb] 使用 JavaFX jmods: $JAVAFX_JMODS"

# ---- 4. 准备 jpackage 输入目录 ----
rm -rf "$BUILD_DIR/input"
mkdir -p "$BUILD_DIR/input" "$OUT_DIR"
cp "$MAIN_JAR" "$BUILD_DIR/input/"

# ---- 5. 调用 jpackage 生成 deb ----
echo "[build-deb] 调用 jpackage 生成 .deb ..."
jpackage \
    --type deb \
    --name HomeworkChecker \
    --linux-package-name homeworkchecker \
    --app-version "$APP_VERSION" \
    --linux-app-release "$DEB_RELEASE" \
    --dest "$OUT_DIR" \
    --input "$BUILD_DIR/input" \
    --main-jar "$(basename "$MAIN_JAR")" \
    --main-class com.xfty.homeworkchecker.Entry \
    --module-path "$JAVAFX_JMODS" \
    --add-modules "$JAVA_MODULES" \
    --java-options "--add-modules=$JAVAFX_RUNTIME_MODULES" \
    --java-options "--enable-native-access=javafx.graphics,javafx.web" \
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.web/com.sun.javafx.sg.prism.web=ALL-UNNAMED" \
    --icon "$ROOT/launcher/logo.png" \
    --license-file "$ROOT/LICENSE" \
    --vendor "$VENDOR" \
    --description "HomeworkChecker" \
    --linux-deb-maintainer "$DEB_MAINTAINER" \
    --linux-app-category "education" \
    --linux-menu-group "Education" \
    --linux-shortcut \
    --linux-package-deps "$PACKAGE_DEPS"

echo "[build-deb] 完成，产物："
ls -la "$OUT_DIR"/*.deb
