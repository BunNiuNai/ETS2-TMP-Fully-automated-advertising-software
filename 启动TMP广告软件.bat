@echo off
setlocal

cd /d "%~dp0"

if not exist "jre\bin\java.exe" (
    echo 错误：找不到 jre\bin\java.exe
    echo 请将Java运行环境放置到 jre 文件夹中
    pause
    exit /b 1
)

echo 启动TMP广告软件...
jre\bin\java.exe --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 TMPAdSoftware

endlocal
