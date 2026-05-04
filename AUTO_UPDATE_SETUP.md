# 自动更新系统配置指南

## 概述

应用内置了自动更新系统，支持：
- 启动时自动检查更新
- 手动检查更新（设置页面）
- 多镜像源下载（GitHub + 国内镜像）
- 后台下载 + 自动安装

## 配置步骤

### 1. 创建GitHub仓库

1. 在GitHub上创建仓库，例如: `your-username/gaojiluyin`
2. 将代码推送到仓库

### 2. 修改更新配置

编辑 `app/src/main/java/com/gaojiluyin/data/remote/update/UpdateModels.kt`：

```kotlin
object UpdateConfig {
    // 修改为你的GitHub仓库
    const val GITHUB_REPO = "your-username/gaojiluyin"
    // ...
}
```

### 3. 发布新版本

#### 方式一：自动发布（推荐）

1. 修改代码并提交
2. 打tag触发自动构建：
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```
3. GitHub Actions会自动：
   - 构建APK
   - 生成version.json
   - 创建Release

#### 方式二：手动发布

1. 构建APK：
   ```bash
   ./gradlew assembleDebug
   ```

2. 在GitHub创建Release：
   - 上传APK文件
   - 上传version.json

3. 更新version.json内容：
   ```json
   {
     "versionCode": 2,
     "versionName": "v1.1.0",
     "apkUrl": "v1.1.0/GaoJiLuYin-v1.1.0.apk",
     "apkSize": 152000000,
     "md5": "计算出的MD5值",
     "changelog": "1. 更新内容1\n2. 更新内容2",
     "forceUpdate": false
   }
   ```

### 4. 国内镜像配置

已内置以下镜像源：

| 镜像 | 地址 | 说明 |
|------|------|------|
| GitHub | github.com | 官方源 |
| ghproxy | mirror.ghproxy.com | 免费加速 |
| FastGit | hub.gitmirror.com | 免费加速 |
| Gitee | gitee.com | 需手动同步 |

#### Gitee同步方法

1. 在Gitee创建仓库，导入GitHub仓库
2. 设置同步规则，自动同步代码
3. 修改 `UpdateModels.kt` 中的Gitee地址

### 5. 测试更新

1. 安装当前版本APK
2. 在GitHub发布新版本（versionCode > 当前版本）
3. 打开应用，应该会弹出更新提示

## 更新流程

```
应用启动
  ↓
检查version.json（多镜像源）
  ↓
发现新版本 → 显示更新对话框
  ↓
用户点击"立即更新"
  ↓
后台下载APK（显示进度）
  ↓
下载完成 → 提示安装
  ↓
用户点击"立即安装"
  ↓
调用系统安装器
```

## 注意事项

- versionCode必须递增，否则不会触发更新
- apkUrl是相对于Release的路径
- md5用于校验文件完整性
- forceUpdate=true时用户无法跳过更新
- 国内用户建议配置Gitee镜像以提高下载速度
