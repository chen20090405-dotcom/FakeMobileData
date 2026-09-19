# 伪装移动数据（FakeMobileData）

一个 LSPosed / Xposed 模块：在**没有插 SIM 卡**、**没有 WiFi** 的情况下，
让状态栏显示移动数据图标（信号格 + 5G/4G 图标 + 运营商名）。

## 功能

- 中文可视化设置界面：总开关、运营商名称、网络类型（5G/4G/3G/2G）、信号格数（0-4）
- 保存后一键重启 SystemUI，免重启手机
- 兼容 Android 11 - 16（HyperOS / MIUI / 类原生）

## 原理

Hook 作用域 `com.android.systemui`，修改以下返回值为 SystemUI "伪造" 有卡有网的状态：

1. `TelephonyManager.getSimState` 等 → 假装 SIM 卡就绪
2. `ServiceState.getState` 等 → 假装在网
3. `SignalStrength.getLevel` → 信号格数
4. `MobileSignalController / NetworkControllerImpl` 的 hasService 等 → 让图标可见

设置保存在模块自己的 SharedPreferences，通过 Root `chmod 644` 放开读取权限。

## 编译（无需电脑，用 GitHub 免费云端编译）

1. 注册/登录 GitHub，新建一个仓库（比如叫 `FakeMobileData`），**不要**勾选添加 README
2. 把本项目所有文件上传到仓库（网页上 "uploading an existing file"，或直接用网页创建文件粘贴）
   - 上传 zip 的话网页只支持单文件，建议解压后整个文件夹拖进去
3. 仓库顶部点 **Actions** → 左侧选 **Build APK** → 右侧 **Run workflow**
4. 等几分钟变成绿色后，点进这次运行记录，页面最下方 **Artifacts** 里下载 **app-debug**
5. 解压得到 `app-debug.apk`，传到手机安装

## 使用

1. 安装 APK
2. 打开 **LSPosed 管理器 → 模块**，启用"伪装移动数据"，作用域勾选 **系统界面（SystemUI）**
3. 重启手机（或打开模块 APP 点"重启系统界面"）
4. 在模块 APP 里设置运营商名 / 5G / 满格，点保存，再点"重启系统界面"

## 已知限制

- 部分机型 5G 图标会带漫游 "R" 角标（4G/5G 网络类型图标必须靠漫游状态触发），属正常
- 只影响 SystemUI 显示，不会提供真实网络
- 个别 ROM 的 SystemUI 类名不同，如果图标完全不显示，需要用本机框架 dump 类名再补 Hook

## 本地编译（可选）

需要 JDK 17 + Android SDK，或 Android Studio：

```
gradle assembleDebug
```
