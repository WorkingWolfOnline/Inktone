# 墨音 Inktone

> 沉浸式阅读与空间记录 · 把「阅读」变成有音乐、有光晕、有陪伴的专注体验

一款纯本地运行的 Android 阅读应用，用 Kotlin 与 Jetpack Compose 编写。支持导入 PDF / EPUB / Word / TXT，长按屏幕进入专注阅读，配本地音乐与阅读指针；内置指南针方向拍照与照片管理模块，用于按东南西北和楼层记录、查看空间照片。

---

## ✨ 功能特性

| 模块 | 说明 |
| --- | --- |
| 📚 书架 | 本地书库，导入 PDF / EPUB / Word / TXT 自动解析章节目录，阅读进度本地保存 |
| 🎵 专注模式 | 手指长按屏幕，音乐淡入、金色光晕跟随、专注灯呼吸，累计专注时长 |
| 🎼 音乐库 | 导入本地音乐作为阅读背景音，隐私安全，不依赖在线曲库 |
| 🖋️ 阅读指针 | 类似眼动仪的逐字引导，当前字放大变亮、金色指针跟随、已读轨迹、页末自动翻页，多种「流星」配色 |
| 🧭 指南针 | 按东南西北方向拍照并自动归入方向文件夹，支持楼层管理，照片可重命名 |
| 🌸 拾花集 | 阅读时长兑换繁花图鉴（41 种花），集齐收藏，可设为开屏 |
| 🧩 拼图记忆 | 图片拼图记忆 |
| 📇 知识卡片 | 伴读时回顾复习知识点 |
| 🖼️ 壁纸 | 静态 / 动态壁纸，多套主题与深色模式 |
| 💬 墨音萌娘 | 「墨音」伴读角色，专注阅读时的温柔提醒与鼓励 |

---

## 📹 界面展示

<div align="center">
  <video src="docs/videos/指南针功能演示效果.mp4" width="32%" controls muted loop playsinline></video>
  <video src="docs/videos/音乐卡片功能演示效果.mp4" width="32%" controls muted loop playsinline></video>
  <video src="docs/videos/薰衣草壁纸阅读效果.mp4" width="32%" controls muted loop playsinline></video>
</div>

> 更多演示视频见 [docs/videos](./docs/videos)。

---

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose（Material 3）
- **架构**：单 Activity + Compose 导航
- **数据**：全部本地存储（DataStore / 文件系统），无服务器、无网络依赖
- **最低系统**：Android 8.0（API 26）

---

## 🔨 构建

```bash
# 在项目根目录执行
./gradlew assembleDebug      # 生成 debug 包
./gradlew assembleRelease    # 生成 release 包
```

APK 输出目录：`app/build/outputs/apk/`

> 需要 JDK 17 与 Android SDK（compileSdk 34）。

---

## 🧭 使用说明

1. 打开书架，点击「导入文件」导入 PDF / EPUB / Word / TXT。
2. 打开一本书，长按屏幕进入专注模式：音乐淡入、光晕跟随、专注灯亮起。
3. 阅读指针自动逐字推进，遇到标点停顿，页末自动翻页。
4. 手指松开暂停，再次长按继续。
5. 阅读时长自动累计，可在「拾花集」兑换繁花图鉴。
6. 进入「指南针」：可新建或切换楼层，转动手机对准目标朝向，点击拍照按钮拍摄。
7. 照片自动归入对应方向文件夹；点开文件夹查看该方向照片，点击照片可重命名。

---

## 🙏 致谢

这个项目对我有特别的意义。我是一名化工厂里的普通员工，**第一次**尝试做软件，全程借助 [DeepSeek](https://www.deepseek.com/) 完成了从想法到实现的过程。

> 从一行代码都不会写，到看着「墨音」一点点跑起来，这个过程让我学到了太多东西，也让我发自内心地感到开心。

感谢 DeepSeek 提供的帮助，感谢 Jetpack Compose 与开源社区的生态，也感谢每一个愿意点开这个仓库、试用这个软件的你。

---

## ❤️ 支持

如果你喜欢「墨音 Inktone」，欢迎在 [爱发电](https://afdian.com/a/Kaiyu_132) 支持我的创作，或在 GitHub 上点个 Star、提 Issue 反馈问题。

---

## 📄 许可证

本项目采用 [MIT License](./LICENSE) 开源。
