# TMP 广告软件

项目完全开源，可以放心使用。

自动循环发送游戏内广告消息的桌面工具，Java Swing + Robot 键盘模拟实现。

## 快速使用

下载 `TMPAdSoftware.exe`（Releases 页面），双击运行即可，**无需安装 Java 环境**。

## 功能

- 设置倒计时间隔（分钟），范围 1~1440（24 小时）
- 5 个消息输入框，循环发送，空消息自动跳过
- 当前发送消息高亮显示，发送日志实时记录
- 消息和设置自动持久化（Preferences），关闭后重启自动恢复
- 模拟键盘：**Y**（呼出聊天框）→ **Ctrl+V**（粘贴消息）→ **Enter**（发送）

## 使用步骤

1. 填写倒计时时间（分钟）
2. 在"发送消息 1~5"中填入广告内容
3. 点击"开始"
4. **立即切换到目标游戏窗口**
5. 倒计时归零后自动按顺序循环发送（1→2→3→4→5→1）
6. 点击"暂停"停止发送

## 更新日志

### v3.0 (2026-06-08) — 稳定性大版本

**线程安全修复**：`volatile` 修饰关键字段，`sendMessage()` 使用局部变量快照防竞态，`ExecutorService` 单线程池替代裸线程。

**输入校验增强**：倒计时限制 1~1440 分钟 + `Math.multiplyExact` 防溢出，提取 `validateCountdown()` / `parseCountdown()` 静态方法便于测试。

**异常处理修复**：`pressKey()` Shift 键 try-finally 防卡键，Robot 初始化失败禁用开始按钮并提示，发送失败写入日志区域。

**资源管理**：`releaseResources()` 中 ExecutorService 优雅关闭（shutdown → awaitTermination → shutdownNow）。

**代码质量**：新增 `ValidationTest.java`（9 个自动化测试），覆盖空值/零/负数/边界/溢出/非数字所有场景。

**后续增强 (2026-07)**：字体常量统一管理 + fallback 机制、颜色常量提取、日志区 500 行上限防内存泄漏、`launcher.py` 健壮性增强、`.bat` 编码修复。

### v2.0 — 界面美化 + 高亮/持久化/日志

新增消息高亮、Preferences 持久化、发送日志、界面美化。

### v1.0 — 初始版本

基础功能：倒计时循环发送、键盘模拟。

## 源码编译

最低 JDK 17+（打包版使用 JDK 26，无需用户安装）。

```bash
# 编译
javac -encoding UTF-8 -d build/app TMPAdSoftware.java

# 运行
java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -cp build/app TMPAdSoftware

# 运行测试
javac -encoding UTF-8 -d build/app TMPAdSoftware.java ValidationTest.java
java -ea -cp build/app ValidationTest
```

或使用 bat 脚本：`启动TMP广告软件.bat`（需将 JRE 放入 `jre/` 目录）。

## 打包为 EXE

```bash
# 编译并打 JAR
javac -encoding UTF-8 -d build/app TMPAdSoftware.java
cd build/app && jar --create --file ../TMPAdSoftware.jar --main-class TMPAdSoftware *.class && cd ../..

# 生成应用镜像（含 JRE）
jpackage --name TMPAdSoftware --input build --main-jar TMPAdSoftware.jar \
  --main-class TMPAdSoftware --type app-image --dest dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --java-options "-Dfile.encoding=UTF-8" --app-version 3.0

# 打包为单文件 EXE（使用 PyInstaller 自解压方案）
pyinstaller --clean TMPAdSoftware.spec
```

## 注意事项

- 请遵守相关法律法规，仅在合规场景下使用
- 如杀毒软件误报，请添加信任（Robot 键盘模拟行为可能触发警报）
