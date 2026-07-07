import sys
import os
import subprocess


def find_exe():
    """定位 TMPAdSoftware.exe，按优先级返回首个存在的路径。"""
    if getattr(sys, 'frozen', False):
        bundle_dir = sys._MEIPASS
    else:
        bundle_dir = os.path.dirname(os.path.abspath(__file__))

    candidates = [
        os.path.join(bundle_dir, 'TMPAdSoftware', 'TMPAdSoftware.exe'),
        # Fallback: PyInstaller 生成器所在目录
        os.path.join(os.path.dirname(sys.executable), 'TMPAdSoftware', 'TMPAdSoftware.exe'),
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    return None


def main():
    exe_path = find_exe()
    if exe_path is None:
        print("错误：找不到 TMPAdSoftware.exe")
        print("请确认应用镜像目录 TMPAdSoftware/ 与本启动器同级")
        input("按回车键退出...")
        sys.exit(1)

    try:
        result = subprocess.run([exe_path], cwd=os.path.dirname(exe_path))
        if result.returncode != 0:
            print(f"警告：TMPAdSoftware 退出代码 {result.returncode}")
            input("按回车键关闭...")
    except Exception as e:
        print(f"启动失败: {e}")
        input("按回车键关闭...")
        sys.exit(1)


if __name__ == '__main__':
    main()
