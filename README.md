# Zero PWA

Zero PWA 是一个基于 Android 平台、遵循 Material You (Material 3) 设计语言并完全使用 Jetpack Compose 构建的通用 PWA 运行容器与管理工具。它可以将任意网站自动探测并转换为独立运行的原生 PWA 应用。

---

## 核心特性

1. **配置任意网站为 PWA**
   - 自动嗅探目标网站的 `manifest.json`、`<meta name="application-name">`、`theme-color` 及 Favicon/Apple Touch Icon；
   - 智能提取最高清晰度图标并缓存到本地，离线亦可显示；
   - 针对无 Manifest 的网站提供无缝回退方案；
   - 支持自定义 PWA 名称、修改启动 URL、切换 User-Agent（默认 / 电脑桌面版）。

2. **创建桌面快捷方式 (Pinned Shortcuts)**
   - 一键将配置的 PWA 固定到手机桌面启动器；
   - 采用 Android 8.0+ 自适应图标（Adaptive Icon），与系统桌面风格高度契合。

3. **长按应用图标直接打开 (Dynamic Shortcuts)**
   - 基于 Android App Shortcuts 机制，长按 Zero PWA 应用图标即可直接弹出最常用的 PWA 列表；
   - 支持手动在管理界面中“置顶到长按菜单”，结合最近访问时间动态排序。

4. **独立多任务窗口运行 (Document-centric Tasks)**
   - 采用 Android 系统级的 Document 模式（`FLAG_ACTIVITY_NEW_DOCUMENT` + `intoExisting`）；
   - 每个打开的 PWA 在系统的“最近任务”（Recents Screen / 多任务列表）中呈现为一张独立的卡片，互不干扰；
   - 任务卡片动态定制：根据网站元数据动态更新任务标题、图标与主题色彩。

5. **Material You 设计与现代 Compose 架构**
   - 界面遵循 Google Material 3 规范，支持 Android 12+ Monet 动态壁纸取色；
   - 沉浸式 Edge-to-Edge 设计与深浅色模式自动适配；
   - 完整的 WebView 现代特性支持：JavaScript、DOM Storage、第三方 Cookie 支持、WebRTC 权限流、文件上传与系统下载管理器集成。

---

## 构建与运行

### 依赖环境
- **Java**: OpenJDK 17
- **Android SDK**: API 37 (Min SDK 26, Target SDK 37)
- **Gradle**: 8.7 (Kotlin 2.0.0, AGP 8.4.1)

### 快速构建

```bash
# 构建 Debug 版本 (带 release 签名)
./build.sh assembleDebug

# 构建 Release 版本
./build.sh assembleRelease
```

构建生成的产物位于：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
