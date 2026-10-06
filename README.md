# 大肥鱼桌宠（Coopanion Pet · WebView 极简版）

安卓桌面宠物 App：不用秒哒、不用低代码平台，**100% 使用原版开源项目
[Pal-AI-Lab/Coopanion](https://github.com/Pal-AI-Lab/Coopanion) 的原版形象素材**，
由内部保留的原版 `figure.js / rig.js` 渲染脚本绘制（原版是 PNG 网格骨架渲染，
桌面端看到的所有动画——呼吸、眨眼、头发/尾巴/裙子摆动——都来自这两份脚本，
没有用任何 Java 重写、也没有引入 Live2D SDK）。

架构：**原生空壳（4 个 Java 文件） + WebView 加载 `assets/index.html`**。

---

## 一、目录结构

```
Coopanion-Pet-Android-WebView/
├── .github/workflows/build-apk.yml   ← GitHub Actions 云端编译配置（无电脑也能出 APK）
├── build.gradle                      ← 工程级构建脚本（只声明 AGP 8.5.2）
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle                  ← 应用级构建脚本（compileSdk 34 / minSdk 26）
│   └── src/main/
│       ├── AndroidManifest.xml       ← 悬浮窗/前台服务/开机自启/网络权限
│       ├── java/com/coopanion/pet/
│       │   ├── MainActivity.java     ← ① 引导开启悬浮窗权限 + 填 API Key
│       │   ├── Prefs.java            ← ② 只保存 DeepSeek API Key
│       │   ├── BootReceiver.java     ← ③ 开机自启
│       │   └── FloatPetService.java  ← ④ 悬浮窗 + 透明 WebView + 拖拽移动
│       └── assets/
│           ├── index.html            ← ★ 桌宠全部交互（形象/表情/聊天/台词）
│           ├── dafeiyu/              ← 原版形象素材（figure.js/model.json/tex/feat）
│           ├── rig/rig.js            ← 原版 WebGL2 网格变形引擎
│           └── stickers/             ← 表情包图片（命名 1.png ~ 15.png）
└── README.md
```

> 只有上方 `java/` 里的 **4 个 Java 文件**，没有任何其他 Java 代码。

---

## 二、三步云端编译出 APK（没电脑也能装）

### 第 1 步：把工程传到 GitHub

手机浏览器打开 **github.com** 登录后：

- 新建一个仓库（Public / Private 都行，比如 `coopanion-pet`），**不要勾选任何初始化文件**；
- 打开 **[github.com/upload](https://github.com/upload)** 选择这个仓库，
  把本文件夹**整个拖进上传框**（手机上可先把文件夹压成 zip 上传，GitHub 会自动解压；
- 上传完成后 Commit。

> 传整个文件夹源码即可（zip 会太大，先解压再上传）。
> 手机上传不方便时，见「第五部分：请人代编译」。

### 第 2 步：等 Actions 自动编译

- 打开仓库页面 → **Actions** 标签页；
- 能看到一条正在运行的 `一键编译 APK` 工作流（绿色转圈），点进去等它跑完（约 3~5 分钟）；
- 如果没自动触发：Actions 页面 → 左侧 **一键编译 APK** → 右边 **Run workflow** 手动触发一次。

### 第 3 步：下载 APK 并安装

- 工作流跑完后，页面最底部有个 **Artifacts（构件）** 区块；
- 下载 **`CoopanionTablePet-debug`**（zip），解压得到 **`app-debug.apk`**；
- 手机上点击安装；提示“未知来源”时在弹窗里允许即可。

---

## 三、安装后的使用

1. 打开 **大肥鱼桌宠** App：
   - 点「① 去开启悬浮窗权限」，在系统设置里把「大肥鱼桌宠」的「显示在其他应用上层」打开；
   - 在 App 里粘贴你的 **DeepSeek API Key**（[platform.deepseek.com](https://platform.deepseek.com) 申请），点「保存 API Key」；
2. 点「② 开始陪伴」→ 大肥鱼出现在屏幕上，可自由拖动；
3. 玩法：
   - **单击**：随机傲娇台词（4 句），30% 概率随机弹一张表情包（把图片放进 `assets/stickers/`，按 `1.png ~ 15.png` 命名即可生效）；
   - **双击**：弹出隐藏聊天框（此时才消耗 Token），输入文字发送，大肥鱼用 DeepSeek API 回复；
   - **按住拖动**：随机“被提起”台词（3 句），形象进入原版“被拎起”动画；
4. 长按常驻通知可关闭桌宠；或在 App 里点「停止悬浮窗」；
5. **开机自启**：已注册开机广播，重启手机且悬浮窗权限已开启时自动恢复桌宠。

> API Key 只保存在本机（SharedPreferences），不会上传；聊天请求由网页端直接发给
> DeepSeek 官方接口 `https://api.deepseek.com/v1`（模型 `deepseek-chat`）。

---

## 四、想改的东西都在 index.html 里

| 想改什么 | 在哪里改 |
| --- | --- |
| 单击台词（4 句傲娇话） | `assets/index.html` 里的 `TAP_LINES` |
| 拖拽台词（3 句） | 同文件里的 `LIFT_LINES` |
| 表情包出现概率（默认 30%） | 同文件 `popSticker()` 里的 `Math.random() < 0.3` |
| 表情包图片 | 把 `1.png ~ 15.png` 放进 `assets/stickers/` |
| 待机自动表情频率 | 同文件 `EXPR_POOL` 与 `exprAt` |
| AI 人设 / 模型 / 接口地址 | 同文件 `SYSTEM_PROMPT` 与 `sendChat()` 里的 fetch 地址 |
| 悬浮窗大小 / 位置 | `FloatPetService.java` 里的 `dp(120) × dp(200)` 常量 |

改完把新文件传回 GitHub → Actions 自动重新编译 → 下载安装新版即可。

---

## 五、请人代编译（没电脑、GitHub 也不想弄）

1. 在手机上把整个 `Coopanion-Pet-Android-WebView` 文件夹压缩成 zip；
2. 发给会电脑的朋友，附上这句话：
   > “帮我用 Android Studio 打开这个工程，等 Gradle 同步完直接
   > Run（或 Build → Build APK），把 `app-debug.apk` 发我。工程里也有 GitHub Actions
   > 配置（`.github/workflows/build-apk.yml`），推 GitHub 也能自动编译。”
3. 朋友装 Android Studio 后正常流程即可，工程无任何额外配置。

---

## 六、常见问题

- **APK 安装提示“未安装/解析失败”**：最低系统要求 Android 8.0（minSdk 26），请升级手机系统；
- **启动后屏幕上没有鱼**：先回 App 确认「悬浮窗权限」已开启（部分 ROM 需要手动在应用权限里打开）；
- **聊天回复“HTTP 401”**：API Key 填错或已失效，回 App 重新粘贴保存；
- **聊天无反应但有网络**：DeepSeek 接口需要能正常访问外网；
- **想换形象配色**：原版自带 8 套配色（model.json 的 schemes），可在 index.html 里调用
  `fig.setScheme('claude')` 等切换（默认 deepseek 最还原电脑端演示）。