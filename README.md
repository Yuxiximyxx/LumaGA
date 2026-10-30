> **本仓库是 [Duzc01/LumaGA](https://github.com/Duzc01/LumaGA) 的个人 fork，应用名「泥坛」。** 在上游基础上做 MIUIX（HyperOS 风格）深度定制，与上游的主要差异：
>
> - **底栏四功能 + 悬浮玻璃 Dock**：底栏为「主页 / 版面 / 收藏 / 我的」（「版面」页可自定义常逛版面、「主页」入口可指向任意版面），形态是自适应宽度的悬浮毛玻璃 Dock，玻璃材质三档可切换（毛玻璃 / 磨砂 / 液态玻璃，含参数微调）
> - **Miuix-KMP 真组件迁移**：全局接入 Miuix-KMP 真组件换装（含菜单/弹窗），导航由 SharedNav 驱动（卡片式转场 + 可跟随手指的预测式返回）
> - **MIUIX 版式**：大标题顶栏、分组大圆角卡片、胶囊筛选条；帖子详情页为圆形玻璃回复按钮（点开全编辑器回帖）
> - **收藏帖楼层阅读记忆**：打开收藏的帖子自动恢复到上次阅读的楼层
> - **小说评分 BBCode 适配**：`[style]` / `[randomblock]` / `[fixsize]` / `[comment]` 等上游未渲染的标签已补齐渲染，漩涡书院小说评分帖可正常阅读
> - **搜索深链**：`mnga://search?key=关键词` 直达搜索
> - **自有应用图标**（月牙 + 海浪），移除扫码登录
>
> **签名说明**：本 fork 使用自有签名，与上游版证书不同——安装 fork 版前需先卸载上游版；fork 版之间可直接覆盖安装。
>
> **维护方式**：本地功能已收官（当前版本 v1.8.8），不再新增功能；改为**每周同步一次上游**，有上游更新才合并并发版。正式签名安装包见 [本仓库 Releases](https://github.com/Yuxiximyxx/LumaGA/releases)。下方为上游 README 原文。

# LumaGA

<p align="center">
  <img src="docs/logo.png" width="128" alt="LumaGA logo">
</p>

**English** | [简体中文](README.zh-CN.md)

Android port of [MNGA](https://github.com/BugenZhao/MNGA), an NGA (bbs.nga.cn)
client. Built with Jetpack Compose, on top of the same Rust `logic` backend
MNGA uses, linked in as `liblogic.so` over JNI.

## Screenshots

Home | Topic list | Topic detail | Topic detail
--- | --- | --- | ---
![Home](docs/screenshots/mnga_home.jpg) | ![Topic list](docs/screenshots/lumaga_list.jpg) | ![Topic detail](docs/screenshots/lumaga_post.jpg) | ![Topic detail](docs/screenshots/lumaga_post_2.jpg)

## Usage

- Download the latest APK from the [Releases](https://github.com/Duzc01/LumaGA/releases) page:
  - `app-release.apk` — signed release build (recommended)
  - `app-debug.apk` — debug build (built by CI on every push)
- Install and sign in with your NGA account on first launch.
- `mnga://` deep links are supported, e.g. `mnga://forum/f/722` opens a forum
  directly; links copied to the clipboard are also detected and opened
  automatically when the app comes to the foreground.

## Layout

- `app/` — Compose UI, ported from the MNGA SwiftUI app.
- `logic/` — Android library module wrapping `liblogic.so` plus the generated
  protobuf Java/Kotlin sources.
- `rust/` — the Rust workspace `liblogic.so` is built from, vendored from MNGA.
  See [rust/README.md](rust/README.md).

## Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. JDK 17 required.

Gradle does not build the Rust side. `liblogic.so` is committed under
`logic/src/main/jniLibs/`, so a plain Gradle build needs no NDK or Rust
toolchain. After changing anything under `rust/`, rebuild and commit the
libraries:

```bash
rust/build-jni-libs.sh      # liblogic.so for arm64-v8a, x86_64, x86
rust/gen-kotlin-protos.sh   # only when rust/protos/ changed
```

Both need `protoc`; the first also needs `cargo-ndk` and an Android NDK.

## CI

- [ci.yml](.github/workflows/ci.yml) builds the debug APK on every push/PR to
  `main` and uploads it as an artifact.
- [rust.yml](.github/workflows/rust.yml) rebuilds `liblogic.so` for all three
  ABIs and runs the Rust unit tests, on changes under `rust/` or on demand.

## Attribution

MNGA ships without a LICENSE file and its README reserves all rights, so this
port — including the Rust sources vendored under `rust/` — is not
redistributable without permission from its author. Vendored sled keeps its own
MIT/Apache-2.0 license files in `rust/logic/sled/`.
