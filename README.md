<div align="center">

# ⚔️ Cancel The Damage Interval

**彻底移除无敌帧与攻击冷却 —— 回归纯手速战斗**

*Minecraft Java Edition 战斗机制重构模组*

[![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Forge%20%7C%20Fabric%20%7C%20NeoForge%20%7C%20Quilt-orange)](https://minecraft.wiki/w/Mods)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2017%20%7C%2021%20%7C%2025-red)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/license/mit/)
[![Status](https://img.shields.io/badge/Status-Long--Term%20Maintenance-brightgreen)](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval)

</div>

---

## 📖 项目简介

**Cancel The Damage Interval**（以下简称 **CTDI**）是一款面向 Minecraft Java Edition 的战斗机制重构模组。

项目的核心目标非常简单：

> **移除原版战斗系统中的无敌帧与攻击冷却，让攻击频率真正由玩家的操作决定。**

CTDI 不添加新的武器、职业、属性或独立战斗系统，而是在尽可能保留原版战斗机制的基础上，移除限制连续攻击的核心机制。

项目采用长期维护模式，并计划逐步扩展 Minecraft 版本、Mod Loader 与 Java Runtime 支持。

---

## ⚔️ 核心机制

| 限制项 | 原版行为 | CTDI 行为 |
|:---|:---|:---|
| **无敌帧（i-frames）** | 受击后存在短暂伤害免疫窗口 | 🚫 移除 |
| **攻击冷却（Attack Cooldown）** | 攻击强度受到冷却时间影响 | 🚫 移除 |

> **一句话总结：手速就是 DPS。**

在 CTDI 中，只要攻击能够有效命中目标，每一次攻击都可以正常进入伤害结算流程。

---

## ✨ 核心特性

| # | 特性 | 说明 | 状态 |
|:-:|:---|:---|:---:|
| 1 | **移除无敌帧** | 移除原版伤害免疫窗口，连续命中可以连续造成伤害 | ✅ 核心 |
| 2 | **移除攻击冷却** | 攻击不再受到原版攻击冷却限制 | ✅ 核心 |
| 3 | **手速驱动 DPS** | 攻击频率直接影响理论输出 | ✅ 核心 |
| 4 | **客户端与服务端适配** | 支持战斗逻辑在客户端与服务端环境中运行 | ✅ 核心 |
| 5 | **原版机制兼容** | 尽可能保留附魔、药水、暴击等原版战斗机制 | ✅ 核心 |
| 6 | **独立配置** | 无敌帧与攻击冷却分别控制 | 📋 规划 |
| 7 | **实体白名单** | 针对特定实体保留原版行为 | 📋 规划 |
| 8 | **延迟处理** | 优化高延迟环境下的连续攻击体验 | 📋 规划 |

---

## ⚔️ 战斗机制对比

| 维度 | Legacy Combat | 现代原版 | **CTDI** |
|:---|:---:|:---:|:---:|
| 无敌间隔 | 无 | 有 | **无** |
| 攻击冷却 | 无 | 有 | **无** |
| 战斗节奏 | 手速驱动 | 时机驱动 | **手速驱动** |
| 攻击强度 | 基本恒定 | 受到冷却影响 | **保持完整攻击强度** |
| 连续命中 | 支持 | 受到伤害免疫限制 | **支持** |

CTDI 的设计理念不是重新创造一套战斗系统，而是：

> **保留原版战斗内容，只移除限制战斗节奏的机制。**

---

## 📦 支持范围

CTDI 的支持范围会随着项目开发持续扩展。

### Minecraft

- **Minecraft Java Edition**
- 支持多个 Minecraft 版本
- 具体支持版本以对应 Release 为准

### Mod Loader

CTDI 的目标 Mod Loader 包括：

- **Forge**
- **Fabric**
- **NeoForge**
- **Quilt**

不同 Mod Loader 的实际支持情况以对应 Release、源码与构建配置为准。

### Java

CTDI 面向多个 Java Runtime 版本进行适配：

- **Java 8**
- **Java 17**
- **Java 21**
- **Java 25**

| Java | 支持 |
|:---:|:---:|
| **8** | ✅ |
| **17** | ✅ |
| **21** | ✅ |
| **25** | ✅ |

> 不同 Minecraft 版本对 Java Runtime 的要求可能不同。
>
> 请根据对应 Release 的构建配置选择正确的 Java 版本。

---

## 📥 安装

### 1. 选择 Minecraft 版本

前往 [Releases](../../releases)，根据你的 Minecraft 版本选择对应的 CTDI Release。

### 2. 安装对应 Mod Loader

根据 Release 页面要求安装对应的：

- Forge
- Fabric
- NeoForge
- Quilt

### 3. 确认 Java Runtime

根据当前 Minecraft 版本及 Release 要求选择对应的 Java Runtime：

- Java 8
- Java 17
- Java 21
- Java 25

### 4. 安装 CTDI

将对应的 `.jar` 文件放入 Minecraft 的 `mods/` 目录。

### 5. 启动游戏

启动对应的 Minecraft 实例即可。

---

## 🔧 技术实现

<details>
<summary>面向开发者的设计说明</summary>

### 伤害免疫

修改实体伤害处理流程，移除原版伤害免疫窗口，使连续攻击能够继续进入正常伤害结算。

### 攻击冷却

修改玩家攻击强度相关逻辑，使攻击不再因为攻击冷却不足而降低伤害。

### 原版机制兼容

CTDI 尽可能保留原版战斗系统中的其他机制，包括：

- 武器伤害
- 附魔
- 药水效果
- 暴击
- 属性
- 原版伤害计算
- Mod Loader 提供的事件与 API

项目不主动重新实现完整的 Minecraft 战斗系统。

### 服务端权威

最终伤害判定以服务端为准。

客户端负责输入、表现以及与服务端之间的数据同步。

### 技术方向

- Java
- Gradle
- Minecraft Modding API
- Forge
- Fabric
- NeoForge
- Quilt
- Mixin / Injection
- 跨版本适配

具体实现会根据 Minecraft 版本与 Mod Loader 的技术架构进行调整。

</details>

---

## 🗺️ 路线图

CTDI 采用**长期维护**模式。

项目不会单纯追求版本数量，而是逐步完善核心机制、兼容性与跨版本支持。

| 方向 | 内容 | 状态 |
|:---|:---|:---:|
| **核心战斗机制** | 无敌帧与攻击冷却处理 | 🚧 开发中 |
| **配置系统** | 提供可配置的战斗机制 | 📋 规划 |
| **Mod 兼容性** | 提高与其他 Mod 的兼容性 | 📋 规划 |
| **网络优化** | 改善多人环境下的战斗体验 | 📋 规划 |
| **Minecraft 多版本** | 扩展不同 Minecraft 版本 | 🔮 持续进行 |
| **Forge** | Forge 版本适配 | 🔮 持续进行 |
| **Fabric** | Fabric 版本适配 | 🔮 规划 |
| **NeoForge** | NeoForge 版本适配 | 🔮 规划 |
| **Quilt** | Quilt 版本适配 | 🔮 规划 |
| **Java 8** | Java 8 Runtime 适配 | 🔮 持续进行 |
| **Java 17** | Java 17 Runtime 适配 | 🔮 持续进行 |
| **Java 21** | Java 21 Runtime 适配 | 🔮 持续进行 |
| **Java 25** | Java 25 Runtime 适配 | 🔮 持续进行 |

---

## 🧩 长期维护

CTDI 并不是一次性的实验项目。

项目采用长期维护模式：

- 🐛 发现 Bug → 修复
- 🔧 发现兼容性问题 → 调整
- 💡 有实际需求 → 评估并实现
- ⚡ 性能问题 → 优化
- 📦 新 Minecraft 版本 → 视情况适配
- 🔌 新 Mod Loader → 视情况适配
- ☕ 新 Java Runtime → 根据 Minecraft 要求适配
- 😴 没有必要的改动 → 保持稳定

项目优先保证核心战斗机制的稳定性，而不是单纯追求版本号增长。

---

## 🤝 贡献

欢迎提交 Issue 与 Pull Request。

### 创建分支

    git checkout -b feature/amazing

### 构建项目

    ./gradlew build

### 提交修改

    git add .
    git commit -m "Add amazing feature"
    git push origin feature/amazing

随后创建 Pull Request。

提交代码前，请确保：

- 项目能够正常构建
- 核心战斗机制没有被意外破坏
- 没有引入不必要的默认行为变化
- 相关修改已经经过实际测试
- 没有破坏已有版本或 Mod Loader 的兼容性

---

## 🤖 AI 辅助开发

CTDI 的开发过程中使用 AI 作为辅助开发工具。

AI 可参与：

- 源码分析
- 技术方案分析
- 代码生成
- Debug
- 构建错误分析
- 文档编写
- 项目结构分析
- 跨版本适配分析

AI 生成的代码和内容会经过项目维护者审查、修改与测试。

项目的整体方向、功能设计、代码取舍、测试结果以及最终发布由项目维护者决定。

---

## 📊 项目状态

CTDI 目前处于持续开发与维护阶段。

核心战斗机制优先，跨版本与跨 Loader 支持逐步推进。

具体支持情况请以项目最新 Release、源码与构建配置为准。

---

## 📜 许可证

本项目基于 **MIT License** 开源。

    MIT © 2026 zzy89216-gif

---

## ❤️ 致谢

感谢：

- Minecraft 社区
- Minecraft Modding 社区
- Forge 项目及其贡献者
- Fabric 项目及其贡献者
- NeoForge 项目及其贡献者
- Quilt 项目及其贡献者
- 所有参与测试的用户
- 提交 Issue 与 Bug 反馈的用户
- 提供建议与改进意见的社区成员
- 所有为 Minecraft 开源生态做出贡献的开发者

---

<div align="center">

# ⚔️ Cancel The Damage Interval

**Remove the limits. Keep the combat.**

*Made for Minecraft combat.*

⭐ **如果这个模组对你有帮助，欢迎给项目一个 Star。**

</div>