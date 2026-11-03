# Zero PWA

<p align="center">
  <a href="https://github.com/NevermindZZT/zero-pwa"><img src="https://img.shields.io/badge/GitHub-NevermindZZT%2Fzero--pwa-181717.svg?style=flat-square&logo=github" alt="GitHub Repo" /></a>
  <a href="https://github.com/NevermindZZT/zero-pwa/releases"><img src="https://img.shields.io/badge/version-1.0.3-orange.svg?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square" alt="License" /></a>
  <img src="https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Target%20SDK-37-blue.svg?style=flat-square" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen.svg?style=flat-square" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat-square&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%20You)-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen.svg?style=flat-square" alt="Build Status" />
</p>

> **任意网站 · 独立窗口 · 原生体验**

Zero PWA 是一个基于 Android 平台打造的现代化 PWA 运行容器与管理套件。全量遵循 **Material You (Material 3)** 规范并使用 **Jetpack Compose** 构建。它可以将任意网站自动嗅探并转化为拥有独立任务卡片、独立窗口、桌面自适应快捷方式与极致沉浸感的原生应用。

---

## 🌟 核心特性

1. **智能配置任意网站为 PWA**
   - **全自动元数据嗅探**：输入 URL 后自动解析 Web App Manifest、Favicon 及 SVG 矢量图标，本地高清缓存，离线亦可展示；
   - **智能容错与回退**：对未提供 Manifest 的普通网站自动回退提取页面标题、Apple Touch Icon 与主题色彩；
   - **全量现代 Web 容器支持**：开启 DOM Storage、IndexedDB、第三方 Cookie 兼容、文件选择器与系统下载管理器。

2. **桌面固定快捷方式 (Pinned Shortcuts)**
   - 一键固定到手机桌面启动器；
   - 采用 Android 8.0+ 自适应图标（Adaptive Icon），自动进行安全采样与系统最大尺寸缩放，与启动器风格深度融合。

3. **长按应用图标直接打开 (Dynamic Shortcuts)**
   - 基于 Android App Shortcuts 机制，长按 Zero PWA 应用图标即可直接弹出最常用的 PWA 列表；
   - 支持在管理面板中一键“置顶到长按菜单”，结合最近访问时间动态排序。

4. **系统级独立多任务窗口 (Document-centric Tasks)**
   - 采用 Android 系统级的 Document 模式；
   - 每个打开的 PWA 在系统的“最近任务”列表中呈现为独立卡片，任务标题、图标与主题色彩动态定制，与原生应用无异。

5. **默认单窗口复用机制**
   - 默认每个 PWA 仅开启一个窗口，再次点击自动激活并带到前台已有窗口（`onNewIntent`），避免多任务卡片重复堆叠；
   - 支持在配置中按需开启“多任务窗口并发”，允许同站点多开。

6. **沉浸式状态栏与浅色 UI 智能适配**
   - 支持状态栏全透明沉浸穿透与状态栏安全区保护隔离自由切换；
   - 搭载视口顶部背景色动态采样算法，在浅白底高德地图或浅色 UI 下自动反转为清晰深色黑字；
   - 支持按 PWA 自由指定“智能适配 / 深色字 (黑) / 浅色字 (白)”。

7. **灵动贴边悬浮工具手柄 (FloatingEdgeToolHandle)**
   - 解决纯净全屏下网页无法随时刷新的痛点；
   - **平时贴边柔和半隐形**：不遮挡网页、地图与文字视线，提供 48dp 舒适触摸靶区并避开系统侧滑返回手势；
   - **自由垂直拖拽**：支持手指上下拖拽停靠在任意高度；
   - **一键快捷浮岛**：轻触即可一键**刷新网页**、**返回首页**、**复制当前链接**或**在浏览器中打开**；
   - **按 PWA 独立配置**：支持为每个 PWA 单独设置是否开启。

8. **多级智能返回导航管道与双击防误触**
   - 优先响应 WebView 原生历史回退 (`canGoBack`)；
   - 针对无 pushState 历史栈的 SPA 单页应用，自动探测页面内置返回按钮（如顶栏返回、关闭抽屉、Escape 信号）并在 PWA 内部平滑回退；
   - 根视图增加 **2 秒内连续按两次返回防误触退出机制**（Double Back to Exit），彻底避免侧滑误触直接秒关 PWA。

9. **前台长连接自愈脉冲与系统电池优化白名单**
   - 针对 Android 系统后台 Doze 深度休眠断网，在设置中心提供“忽略电池优化”一键开启入口；
   - 容器内置 WebSocket 透明代理追踪与切回前台 100ms 自愈脉冲，重回前台瞬间自动恢复连接。

10. **自定义主题色与调色盘**
    - 配备 Material 3 经典推荐色盘与 Hex 颜色代码自定义输入；
    - 支持一键还原网站原始提取色，且用户自定义颜色具备最高绝对优先级。

11. **设置中心与全局主题 (参考 AlphaPro 设计)**
    - 项目 Hero 卡片、动态版本号徽章与 GitHub 项目主页横幅；
    - 全局深浅色模式切换（跟随系统 / 浅色模式 / 深色模式）及 Android 12+ Monet 动态壁纸取色；
    - 统一遵循 Material You 无阴影纯色调海拔规范与精致操作底栏面板。

---

## 📐 自动化版本号规则

项目在 `app/build.gradle.kts` 中集成了**基于 Git 提交次数的自动化版本号计算机制**：

- **版本名称 (Version Name)**：`Major.Minor.Patch.<commit_count>`
  - 例如当前版本为 `1.0.1` 且仓库提交了 7 次，则动态生成的版本名为：`1.0.1.7`
- **版本代号 (Version Code)**：`Major * 100_00_0000 + Minor * 100_0000 + Patch * 10000 + commitCount`（9 位数字）
  - 对应的 Version Code 为：`100010007`

每次执行编译时，Gradle 会自动通过 `git rev-list --count HEAD` 读取最新提交并动态写入 `BuildConfig`，无需手动维护递增。

---

## 🔐 通用正式签名密钥 (Universal Release Keystore)

项目内置了正式的 Android 签名凭据，采用 **RSA 4096 位** 高强度算法（有效期 30 年至 2056 年）。该签名在 Debug 与 Release 构建中均已启用：

- **密钥文件**：`keystore/release.jks`（受 `.gitignore` 保护不入库）
- **配置属性**：`keystore/keystore.properties`（受 `.gitignore` 保护不入库）
- **配置模板**：`keystore/keystore.properties.example`
- **密钥别名 (Alias)**：`letter`
- **证书 DName**：`CN=Letter, OU=Developer, O=Letter Studio, L=Hangzhou, ST=Zhejiang, C=CN`

---

## 📂 项目模块架构

```text
zero-pwa/
├── app/
│   ├── src/main/java/com/letter/zeropwa/
│   │   ├── ZeroPwaApplication.kt        # 全局 Application：初始化 Coil SvgDecoder 与主题配置
│   │   ├── data/
│   │   │   ├── AppPreferences.kt        # 全局偏好与主题状态流 (AppThemeState)
│   │   │   ├── model/PwaItem.kt         # PWA 实体数据模型与常量
│   │   │   ├── db/                      # SQLite 本地持久化 (自动平滑迁移)
│   │   │   │   ├── PwaDatabaseHelper.kt
│   │   │   │   └── PwaRepository.kt
│   │   │   └── parser/                  # 智能嗅探引擎与 Manifest/SVG 图标提取
│   │   │       └── PwaManifestParser.kt
│   │   ├── shortcut/
│   │   │   └── PwaShortcutManager.kt    # 桌面固定快捷方式与长按动态快捷方式管理
│   │   └── ui/
│   │       ├── container/               # PWA 独立任务运行容器
│   │       │   ├── PwaContainerActivity.kt
│   │       │   ├── PwaChromeClient.kt
│   │       │   ├── PwaWebViewClient.kt
│   │       │   ├── WebViewInsets.kt     # 原生 Insets 与视口边界精准映射
│   │       │   └── FloatingEdgeToolHandle.kt # 贴边半隐形悬浮快捷工具手柄
│   │       ├── main/                    # 主管理界面
│   │       │   ├── MainActivity.kt
│   │       │   ├── MainViewModel.kt
│   │       │   ├── HomeScreen.kt
│   │       │   ├── AddPwaDialog.kt      # 添加 PWA 抽屉面板
│   │       │   ├── EditPwaDialog.kt     # 编辑 PWA 参数抽屉面板
│   │       │   ├── PwaActionBottomSheet.kt # 快捷操作底栏面板
│   │       │   └── ThemeColorPickerSection.kt # 主题色选择与 Hex 调色盘
│   │       ├── settings/                # 独立设置中心
│   │       │   ├── SettingsActivity.kt
│   │       │   └── SettingsScreen.kt
│   │       └── theme/                   # Material You 动态色彩与 Typography
│   └── src/test/java/                   # 单元测试 (Manifest 解析, Insets 映射, 边界约束)
├── keystore/                            # 签名凭据模板与说明
└── build.sh                             # 本地快速构建脚本
```

---

## 🛠️ 构建与运行

### 依赖环境
- **Java**: OpenJDK 17 (`JAVA_HOME`)
- **Android SDK**: API 37 (`minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`)
- **Gradle**: 8.7 (Kotlin 2.0.0, AGP 8.4.1, Compose BOM 2024.05.00)

### 快速构建

```bash
# 运行单元测试
./build.sh testDebugUnitTest

# 构建 Debug 版本
./build.sh assembleDebug

# 构建 Release 正式版本 (带签名)
./build.sh assembleRelease
```

构建生成的产物位于：
- **Debug 安装包**：`app/build/outputs/apk/debug/app-debug.apk`
- **Release 安装包**：`app/build/outputs/apk/release/app-release.apk`

---

## 📦 版本发布与下载

欢迎访问 GitHub Releases 页面直接下载最新的正式安装包：

👉 **[https://github.com/NevermindZZT/zero-pwa/releases](https://github.com/NevermindZZT/zero-pwa/releases)**

---

## 📄 开源协议

本项目采用 **[MIT License](LICENSE)** 协议开源。
