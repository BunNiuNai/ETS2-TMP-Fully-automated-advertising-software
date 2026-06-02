import sys
import os
import subprocess
import tempfile
import shutil
import atexit


def main():
    # PyInstaller 打包后的资源路径
    if getattr(sys, 'frozen', False):
        bundle_dir = sys._MEIPASS
    else:
        bundle_dir = os.path.dirname(os.path.abspath(__file__))

    app_dir = os.path.join(bundle_dir, 'TMPAdSoftware')
    exe_path = os.path.join(app_dir, 'TMPAdSoftware.exe')

    if not os.path.exists(exe_path):
        # Fallback: 尝试当前目录
        exe_path = os.path.join(os.path.dirname(sys.executable), 'TMPAdSoftware', 'TMPAdSoftware.exe')

    if not os.path.exists(exe_path):
        print(f"错误：找不到 {exe_path}")
        input("按任意键退出...")
        sys.exit(1)

    subprocess.run([exe_path], cwd=os.path.dirname(exe_path))


if __name__ == '__main__':
    main()
