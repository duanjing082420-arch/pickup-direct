# 快递取件 2.1（改造版）

一个 Android 取件码工具：既能一键打开菜鸟 / 淘宝 / 拼多多的身份码和待取列表，也能把你自己的取件码整理成卡片，大字号显示、一键复制。

上游项目：[EyanLiu/pickup-code-launcher](https://github.com/EidenLiu/pickup-code-launcher)（MIT）
当前仓库：https://github.com/duanjing082420-arch/pickup-code

## 下载 APK

每次推送代码后，GitHub Actions 会自动编译出调试版 APK，在这里下载：

https://github.com/duanjing082420-arch/pickup-code/actions

- 点进最新一次成功的 **Build Debug APK** 任务
- 页面底部 **Artifacts** → 下载 `pickup-code-debug-apk`（zip，解压得到 `app-debug.apk`）
- 需要登录 GitHub 才能下载，产物保留 30 天

安装注意：这是**调试签名**版本，每次云端编译签名都不同，覆盖安装会失败，**先卸载旧版再安装**。已录入的取件码保存在手机本地，卸载会一起清掉，重装后需重新录入。

## 界面结构

两个底部标签页：

**待取快递**

- 「我的包裹」：已保存的取件码卡片，大字号取件码 + 复制按钮 + 右上角删除
- 「＋ 添加包裹」：手动填写快递公司、取件码、驿站位置
- 「自动获取取件码」：短信识别、通知识别的开关与状态
- 五个平台待取入口（淘宝 / 拼多多 / 京东 / 小红书 / 抖音）与「添加到桌面」

**取件码**

- 菜鸟 / 淘宝 / 拼多多三平台切换器，默认淘宝
- 点击大卡直接打开对应平台的官方身份码页面
- 身份码列表与「添加到桌面」

## 取件码从哪来

App 不联网、不登录任何平台账号，取件码来自四个渠道（全部保存在手机本地）：

| 渠道 | 是否需要授权 | 说明 |
| --- | --- | --- |
| 手动录入 | 不需要 | 点「＋ 添加包裹」填写 |
| 剪贴板捕获 | 不需要 | 在菜鸟 / 淘宝复制取件码后切回本 App，自动弹窗询问是否保存 |
| 短信识别 | 需要短信权限 | 收到驿站取件短信时自动提取取件码并保存 |
| 通知识别 | 需要通知使用权 | 监听菜鸟 / 淘宝 / 拼多多的取件通知，命中即保存 |

短信与通知识别默认关闭，在「自动获取取件码」里点击开启。通知识别必须在系统设置的「通知使用权」里手动打开本 App，Android 不允许应用自行开启。

## 源码结构

```text
app/src/main/java/cn/pickup/launcher/
├── MainActivity.java               主界面：双标签页、包裹卡片、录入与设置
├── Destination.java                各平台入口定义（包名、DeepLink、网页兜底）
├── DeepLinkLauncher.java           DeepLink 跳转，失败时降级到网页或打开 App
├── ShortcutPinning.java            把入口固定为桌面图标
├── PickupWidgetProvider.java       桌面小组件
├── LaunchReceiver.java             桌面图标 / 小组件的点击分发
├── PackageStore.java               包裹本地存储（SharedPreferences + JSON，按取件码去重）
├── CodeParser.java                 从短信 / 通知 / 剪贴板文本中识别取件码、快递公司、驿站
├── SmsReceiver.java                短信监听（需 RECEIVE_SMS）
└── PickupNotificationListener.java 通知监听（需通知使用权）
```

其他目录：`web/` 为网页版，`ios/` 为 iOS 版，本次改造未改动这两部分。

Manifest 中声明的权限只有 `RECEIVE_SMS`（以及通知监听服务的绑定权限），没有网络权限。

## 构建

### 本地构建（JDK 17 + Android SDK 35）

```powershell
.\gradlew.bat assembleDebug
```

输出到 `app/build/outputs/apk/debug/app-debug.apk`。

### 云端构建（GitHub Actions）

推送代码到 `main` 分支即自动触发，配置见 `.github/workflows/build-apk.yml`：

```text
JDK 17 (temurin) → setup-gradle → ./gradlew assembleDebug → 上传 APK 产物
```

## 发布版签名

发布前生成自己的证书，并在后续版本中一直沿用：

```powershell
keytool -genkeypair -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kuaidi_qujian
```

复制 `keystore.properties.example` 为 `keystore.properties` 并填入签名信息，然后：

```powershell
.\gradlew.bat assembleRelease
```

签名文件和密码不要提交到公开仓库（已在 `.gitignore` 中排除）。用了固定签名后，用户就能覆盖升级、不必再卸载重装。

## 链接维护

应用跳转的是第三方 App 的内部页面入口，这些入口不是稳定的开放 API。发布前应在装了最新版对应 App 的真机上逐个测试，尤其是抖音入口——抖音没有公开的稳定订单深链，失败时会降级为直接打开抖音 App。

## 开源许可

MIT License，详见 `LICENSE`。

当前版本：2.1.0（改造版）
最后核对日期：2026-09-29。
