# 沉浸安全区与输入法回归记录

## 回归点与证据

- `33401e8`：沉浸独立窗口的主 Column 未读取 WindowInsets，WebView 可接收原生 Insets。
- `388249e`：引入 `imePadding()`，用于修复软键盘遮挡。
- 项目实际使用 Compose Foundation Layout / UI 1.6.7。本地依赖字节码确认：`WindowInsetsHolder` 在首次读取 Insets 时安装 listener，默认 `consumes=true`；`InsetsListener.onApplyWindowInsets` 与 `onProgress` 返回整包 `WindowInsetsCompat.CONSUMED`，并非仅消费 IME。
- 因而加入 IME 读取后，即使键盘未展开，WebView 的状态栏/切口 Insets 也可能被截断。
- `360c3af` 和 `209796a` 没有改动这条顶部布局链路。

## 修复约束

1. PWA 容器用专属 ComposeView，在首次 composition 之前设置 `consumeWindowInsets=false`。不要在 SideEffect 中迟设（holder 会缓存初始化配置）。其他管理界面保持原设置。
2. 保留 `adjustResize`、`imePadding()`，不要通过撤回输入法避让来恢复顶部安全区。
3. 在 WebView 边界按窗口像素和真实视口几何裁减已经避让的空间：沉浸顶部不扣，非沉浸/原生栏按实际 top 扣除，键盘按实际底部空缺扣除，避免双重 Insets。
4. 不注入网站私有类名的 `top: ... !important`，不硬编码 38dp，不把 dp 当作所有网页的 CSS px，也不覆盖网站的 `--safe-area-inset-top`。
5. CSS 自定义属性不等于浏览器的 `env(safe-area-inset-top)`。只移动顶栏会使第二层图例/状态条被遮挡。

## 已有自动回归覆盖

`WebViewInsetsTest` 验证：全屏视口、非沉浸顶部保护、IME 缩高、原生栏与 IME 同时存在、横屏左右安全区与动画越界的几何映射。

这些 JVM 测试不能替代 Android WebView 对 CSS env 的渲染验证。

## 真机复验清单（本次环境无 adb 设备，待验）

- [ ] 同一个 PWA、相同页面和系统 WebView 版本，对比旧验证包、`388249e` 和修复包。
- [ ] 沉浸模式：地图背景穿透状态栏，返回/标题/图例/状态条相互不遮挡。
- [ ] 非沉浸模式：顶部原生保护区域只有一份，网页不重复下移。
- [ ] 带操作栏模式：原生栏避让状态栏，网页内部不重复增加顶部安全区。
- [ ] 打开/关闭键盘：底部输入框可见，顶部安全区保持不变，键盘高度不重复扣除。
- [ ] 横竖屏切换、切后台再恢复、网页路由切换、刷新后复验上述布局。
- [ ] 全屏模式：隐藏系统栏时没有人为的固定顶部空白。

可在网页 DevTools 中临时添加元素 `padding-top: env(safe-area-inset-top)` 并读取 computed paddingTop，以检验实际 CSS env；不要仅以 theme-color 或自定义 CSS 属性代替安全区值。需要同时记录宿主与 WebView 收到的 statusBars / displayCutout / ime Insets 和 WebView 窗口坐标。
