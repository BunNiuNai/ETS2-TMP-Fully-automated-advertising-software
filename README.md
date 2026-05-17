# TMP广告软件

自动循环发送游戏内广告消息的桌面工具，Java Swing + Robot 键盘模拟实现。

## 快速使用

下载 `TMPAdSoftware.exe`（Releases 页面），双击运行即可，**无需安装 Java 环境**。

## 功能

- 设置倒计时间隔（分钟）
- 5 个消息输入框，循环发送
- 空消息自动跳过
- 模拟键盘：**Y**（呼出聊天框）→ **Ctrl+V**（粘贴消息）→ **Enter**（发送）

## 使用步骤

1. 填写倒计时时间（分钟）
2. 在"发送消息 1~5"中填入广告内容
3. 点击"开始"
4. **立即切换到目标游戏窗口**
5. 倒计时归零后自动按顺序循环发送（1→2→3→4→5→1）

## 源码编译

仅开发者需要。确保已安装 JDK 24+。

```bash
javac -encoding UTF-8 TMPAdSoftware.java
java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 TMPAdSoftware
```

或使用 bat 脚本：`启动TMP广告软件.bat`（需将 JRE 放入 `jre/` 目录）。

## 打包为 EXE

```bash
# 编译并打 JAR
javac -encoding UTF-8 -d build/app TMPAdSoftware.java
cd build/app && jar --create --file ../TMPAdSoftware.jar --main-class TMPAdSoftware *.class

# 生成应用镜像（含 JRE）
cd ../..
jpackage --name TMPAdSoftware --input build --main-jar TMPAdSoftware.jar \
  --main-class TMPAdSoftware --type app-image --dest dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --java-options "-Dfile.encoding=UTF-8" --app-version 1.0

# 打包为单文件 EXE（使用 PyInstaller 自解压方案）
pip install pyinstaller
python launcher.py  # 将 dist/TMPAdSoftware 打包为 TMPAdSoftware.exe
```

## 注意事项

- 请遵守相关法律法规，仅在合规场景下使用
- 如杀毒软件误报，请添加信任（Robot 键盘模拟行为可能触发警报）
