# 记一笔 PocketBook

## 自动编译 APK（GitHub Actions）

最简单的方式——不需要本地安装 Android Studio：

### 步骤

1. **Fork 或创建 GitHub 仓库**
   - 登录 GitHub
   - 创建新仓库（如 `pocketbook-android`）

2. **推送代码**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/pocketbook-android.git
   git push -u origin main
   ```

3. **自动编译**
   - 推送后 GitHub Actions 自动触发
   - 进入仓库 → Actions 标签 → 看构建进度
   - 约 5-8 分钟完成

4. **下载 APK**
   - 构建完成后进入最新 workflow run
   - 底部 Artifacts 区域 → 下载 `debug-apk`
   - 解压得到 `app-debug.apk`

### 本地编译（Android Studio）

见 `编译指南.md`

## 项目说明

见 `记一笔_项目说明.md`
