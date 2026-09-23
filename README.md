<div align="center">

# ⚔️ Cancel The Damage Interval

**彻底移除无敌帧与攻击冷却 —— 回归纯手速战斗**

*Minecraft Java Edition 1.20.1 · Forge 战斗机制重构模组*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.1.3%2B-orange)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Java](https://img.shields.io/badge/Java-17-red)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/license/mit/)
[![Status](https://img.shields.io/badge/Status-Long--Term%20Maintenance-brightgreen)](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval)

</div>

---

## 📖 项目简介

**Cancel The Damage Interval**（以下简称 **CTDI**）是一款面向 Minecraft **Java Edition 1.20.1 + Forge** 的战斗机制重构模组。

项目的核心目标非常简单：

> **移除原版战斗系统中的无敌帧与攻击冷却，让攻击频率真正由玩家的操作决定。**

CTDI 不添加新的武器、职业、属性或独立战斗系统，而是在尽可能保留原版战斗机制的基础上，移除限制连续攻击的两个核心机制。

### 核心机制

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
| 4 | **双端支持** | 当前版本针对客户端与服务端进行适配 | ✅ 核心 |
| 5 | **保留原版机制** | 尽可能保留附魔、药水、暴击等原版战斗机制 | ✅ 核心 |
| 6 | **独立配置** | 无敌帧与攻击冷却分别控制 | 📋 规划 |
| 7 | **实体白名单** | 针对特定实体保留原版行为 | 📋 规划 |
| 8 | **延迟处理** | 优化高延迟环境下的连续攻击体验 | 📋 规划 |

---

## ⚔️ 战斗机制对比

| 维度 | 1.8.x Legacy | 现代原版 | **CTDI** |
|:---|:---:|:---:|:---:|
| 无敌间隔 | 无 | 有 | **无** |
| 攻击冷却 | 无 | 有 | **无** |
| 战斗节奏 | 手速驱动 | 时机驱动 | **手速驱动** |
| 攻击强度 | 基本恒定 | 受到冷却影响 | **保持完整攻击强度** |
| 连续命中 | 支持 | 受到伤害免疫限制 | **支持** |

CTDI 的设计理念不是重新创造一套战斗系统，而是：

> **保留原版战斗内容，只移除限制战斗节奏的机制。**

---

## 📥 快速开始

### 当前支持范围

| 项目 | 当前支持 |
|:---|:---:|
| Minecraft | **1.20.1** |
| Mod Loader | **Forge** |
| Forge | **47.1.3+** |
| Java | **17** |
| 客户端 | ✅ |
| 专用服务端 | ✅ |

> 当前版本仅针对 **Minecraft 1.20.1 + Forge** 开发与测试。
>
> 其他 Minecraft 版本、Mod Loader 和 Java 版本暂不代表已经支持。

### 安装

1. 安装 **Minecraft 1.20.1**；
2. 安装 **Forge 47.1.3 或更高版本的 1.20.1 Forge**；
3. 从 [Releases](../../releases) 下载 CTDI；
4. 将 `.jar` 文件放入 Minecraft 的 `mods/` 文件夹；
5. 服务端使用时，同样将模组安装至服务端；
6. 启动游戏即可。

### 双端要求

当前版本涉及战斗逻辑修改。

如果用于多人服务器，建议客户端与服务端使用**相同版本的 CTDI**。

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
- Forge 事件

项目不主动重新实现完整的 Minecraft 战斗系统。

### 服务端权威

最终伤害判定以服务端为准。

客户端负责输入、表现以及与服务端之间的数据同步。

### 当前技术栈

- Java 17
- Gradle
- ForgeGradle
- Mixin
- Minecraft Forge 1.20.1

</details>

---

## 🗺️ 路线图

CTDI 采用**长期维护**模式。

当前主要目标是完善 **Minecraft 1.20.1 + Forge** 版本。

| 版本 / 方向 | 内容 | 状态 |
|:---|:---|:---:|
| **v0.1** | 核心战斗机制 | 🚧 开发中 |
| **v0.2** | 配置系统 | 📋 规划 |
| **v0.3** | Mod 兼容性优化 | 📋 规划 |
| **v0.4** | 网络与延迟优化 | 📋 规划 |
| **v1.0** | 稳定版与完整文档 | 📋 规划 |
| 更多 Minecraft 版本 | 跨版本支持 | 🔮 未来考虑 |
| Fabric | Mod Loader 支持 | 🔮 未来考虑 |
| NeoForge | Mod Loader 支持 | 🔮 未来考虑 |
| Quilt | Mod Loader 支持 | 🔮 未来考虑 |

未来是否扩展其他版本和 Mod Loader，将根据实际开发情况决定。

**当前不会为了追求版本数量而强行进行跨版本移植。**

---

## 🧩 长期维护

CTDI 并不是一次性的实验项目。

项目采用长期维护模式：

- 🐛 发现 Bug → 修复
- 🔧 发现兼容性问题 → 调整
- 💡 有实际需求 → 评估并实现
- ⚡ 性能问题 → 优化
- 📦 新版本需求 → 视情况移植
- 😴 没有必要的改动 → 保持稳定

项目优先保证核心战斗机制的稳定性，而不是单纯追求版本号增长。

---

## 🤝 贡献

欢迎提交 Issue 与 Pull Request。

### 创建分支

```bash
git checkout -b feature/amazing

构建项目

./gradlew build

提交修改

git add .
git commit -m "Add amazing feature"
git push origin feature/amazing

随后创建 Pull Request。

提交代码前，请确保：

- 项目能够正常构建；
- 核心战斗机制没有被意外破坏；
- 没有引入不必要的默认行为变化；
- 相关修改已经经过实际测试。

---

🤖 AI 辅助开发

CTDI 的开发过程中使用 AI 作为辅助开发工具。

AI 可参与：

- 源码分析
- 技术方案分析
- 代码生成
- Debug
- 构建错误分析
- 文档编写
- 项目结构分析

AI 生成的代码和内容会经过项目维护者审查、修改与测试。

项目的整体方向、功能设计、代码取舍、测试结果以及最终发布由项目维护者决定。

---

📜 许可证

本项目基于 MIT License 开源。

MIT © 2026 zzy89216-gif

---

❤️ 致谢

感谢：

- Minecraft 社区
- Minecraft Modding 社区
- Forge 项目及其贡献者
- 所有参与测试的用户
- 提交 Issue 与 Bug 反馈的用户
- 提供建议与改进意见的社区成员
- 所有为 Minecraft 开源生态做出贡献的开发者

---

<div align="center">⚔️ Cancel The Damage Interval

Remove the limits. Keep the combat.

Made for Minecraft combat.

⭐ 如果这个模组对你有帮助，欢迎给项目一个 Star。

</div>
