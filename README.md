<div align="center">

# ⚔️ Cancel The Damage Interval

**彻底移除无敌帧与攻击冷却 —— 回归纯手速战斗**

*Minecraft Java Edition 战斗机制重构模组*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Forge%2047-orange)](https://www.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-red)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/license/mit/)
[![Status](https://img.shields.io/badge/Status-Long--Term%20Maintenance-brightgreen)](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval)

</div>

---

## 📖 项目简介

**Cancel The Damage Interval**（以下简称 **CTDI**）是一款面向 Minecraft Java Edition 的战斗机制重构模组。

项目的核心目标非常简单：

> **移除原版战斗系统中的无敌帧与攻击冷却，让攻击频率真正由玩家的操作决定。**

CTDI 不添加新的武器、职业、属性或独立战斗系统，而是在尽可能保持原版战斗内容的基础上，针对限制连续攻击的核心机制进行修改。

项目采用长期维护模式，并计划逐步扩展 Minecraft 版本、Mod Loader 与 Java Runtime 支持。

---

## ⚔️ 核心机制

| 限制项 | 原版行为 | CTDI 行为 |
|:---|:---|:---|
| **无敌帧（i-frames）** | 受击后存在短暂伤害免疫/差额结算窗口 | 🚫 移除 |
| **攻击冷却（Attack Cooldown）** | 攻击强度受到冷却时间影响 | 🚫 移除 |

> **一句话总结：手速就是 DPS。**

---

## ✨ 当前特性

| # | 特性 | 说明 | 状态 |
|:-:|:---|:---|:---:|
| 1 | **移除无敌帧** | 移除原版伤害免疫窗口，允许连续攻击进入伤害结算 | ✅ 已实现 |
| 2 | **移除攻击冷却** | 移除原版攻击冷却对攻击强度的限制 | ✅ 已实现 |
| 3 | **手速驱动 DPS** | 攻击频率直接影响理论输出 | ✅ 已实现 |
| 4 | **原版机制保留** | 不主动重写完整战斗系统 | ✅ 当前实现 |
| 5 | **独立配置** | 分别控制无敌帧与攻击冷却 | 📋 规划 |
| 6 | **实体白名单** | 针对特定实体恢复原版行为 | 📋 规划 |
| 7 | **延迟处理** | 优化高延迟环境下的连续攻击体验 | 📋 规划 |

---

## 📦 支持范围

> **当前实际支持以源码、构建配置和 Release 为准。下表中"长期目标"只是规划，尚未实现。**

### 当前已实现

| Minecraft | Mod Loader | Java Runtime |
|:---|:---|:---|
| **1.20.1** | **Forge 47.2.0** | **17**（字节码 17，Java 21 亦可运行） |

### 长期目标（规划中，尚未实现）

- **Minecraft**：多版本
- **Mod Loader**：Fabric、NeoForge、Quilt
- **Java Runtime**：8、25 等（以对应 Minecraft 版本的要求为准）

> 注意：Minecraft 1.20.1 本身要求 Java 17+，Java 8 只可能对应更老的 Minecraft 版本（如 1.12.2），不会用于 1.20.1。

---

## 📥 安装

### 1. 前置要求

- **Minecraft Java Edition 1.20.1**
- **Forge 47.2.0**（或 ≥ 47 的 1.20.1 系列）：https://files.minecraftforge.net/
- **Java 17** 或 **Java 21**

### 2. 安装

前往 [Releases](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases) 下载 `ctdi-<version>.jar`，放入 `mods/` 目录，启动游戏。

### 3. 多人游戏

伤害判定以**服务端**为准。建议客户端和服务端都装 CTDI：

| 服务端 | 客户端 | 效果 |
|:---|:---|:---|
| CTDI | CTDI | 手速 DPS 完全生效 |
| CTDI | 原版 | 原版玩家正常游戏，攻击节奏不受影响 |
| 原版 | CTDI | 客户端手感不变，但服务端仍按原版无敌帧结算 |

---

## 🔧 技术实现

<details>
<summary>面向开发者的设计说明（按 1.20.1 实际字节码验证过）</summary>

### 伤害免疫（i-frames）

1.20.1 `LivingEntity#hurt` 中的原版逻辑：

```java
if (this.invulnerableTime > 10.0f && !source.is(BYPASSES_INVULNERABILITY)) {
    if (amount <= this.lastHurt) return false;          // i-frames 窗口内：伤害被丢弃
    this.damageEntity(source, amount - this.lastHurt);  // 否则只结算与上次伤害的差额
    ...
} else {
    this.lastHurt = amount;
    this.invulnerableTime = 20;
    this.damageEntity(source, amount);                  // 完整伤害
    ...
}
```

`invulnerableTime` 每次受击设为 20（1 秒），每 tick 递减。`InvulnerabilityMixin`
在 `hurt` 方法头部（任何判断之前）把 `invulnerableTime` 与 `hurtTime` 清零，
使**每一次**伤害都走完整结算分支。

关键点：

- 注入在方法 HEAD，早于原版判断，同一服务端 tick 内处理多次攻击也全部生效
- 客户端实体同样执行 `hurt`，所以客户端表现（红屏/受击动画）与服务端一致
- `invulnerableTime > 0` 的差额结算分支也被一并绕过，没有残留行为
- `BYPASSES_INVULNERABILITY` 标签（如摔落、着火）本身不受影响，因为它们走的是完整分支

### 攻击冷却

1.20.1 `Player#getAttackStrengthScale`：

```java
public float getAttackStrengthScale(float partialTicks) {
    return Mth.clamp((attackStrengthTicker + partialTicks) / getAttackSpeedModifier(), 0.0f, 1.0f);
}
```

`Player#attack` 用该强度 `s` 计算：

- 基础武器伤害：`s * (0.2 + s*s*0.8)` —— 冷却未满时威力骤降
- 附魔伤害（锋利等）：`附魔伤害 * s`
- 暴击 / 击退 / 冲刺加成门槛：`s > 0.9`

`AttackCooldownMixin` 使该方法恒返回 `1.0`，等价于"攻击永远满冷却"：

- 每次攻击都是满强度（满基础伤害 + 满附魔缩放）
- 暴击、击退、冲刺加成永远可用
- 攻击速度属性只影响冷却回满速度，不再影响伤害
- 附魔、药水、属性、护盾、盔甲、原版伤害计算全部保持原版逻辑

### 服务端权威

Mixin 在客户端与服务端同时加载，伤害判定发生在服务端实体上，因此行为以上表（安装一节）为准。

### 技术栈

- Java 17（字节码级；运行于 Java 17 / 21）
- Forge 47.2.0 + ForgeGradle 6 + Gradle 8.1.1
- Mixin 0.8.5：annotation processor 生成 refmap，`reobfJar` 任务把引用重映射为
  SRG 名，产物 jar 可直接用于正式环境（无需 deobf）

</details>

---

## 🔨 构建

环境要求：JDK 17（JDK 21 亦可）。

```bash
git clone https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval.git
cd Minecraft-Cancel-the-damage-interval
./gradlew build
```

产物：`build/libs/ctdi-0.1.0.jar`（已 reobf，可直接投放）。

首次构建会下载 Gradle 发行版、Forge 与 Minecraft 工件，移动端/弱网环境请预留时间与流量（本次实测约 11 分钟）。

---

## 🗺️ 路线图

| 方向 | 内容 | 状态 |
|:---|:---|:---:|
| **核心战斗机制** | 无敌帧与攻击冷却处理（1.20.1 Forge） | ✅ 已实现 |
| **配置系统** | 分别控制两项机制、恢复原版行为 | 📋 规划 |
| **实体白名单** | 针对特定实体恢复原版行为 | 📋 规划 |
| **网络优化** | 改善多人环境下的战斗体验 | 📋 规划 |
| **Minecraft 多版本** | 扩展不同 Minecraft 版本 | 🔮 持续适配 |
| **Forge 其他版本** | 1.21.x 等 | 🔮 持续适配 |
| **Fabric / NeoForge / Quilt** | 其他 Loader 适配 | 📋 规划 |
| **Java 8** | 仅对应旧 Minecraft 版本 | 🔮 |
| **Java 21** | 已可用（字节码 17） | ✅ |
| **Java 25** | 视新版本 Minecraft 要求 | 🔮 |

---

## 🤝 贡献

欢迎提交 Issue 与 Pull Request。

```bash
git checkout -b feature/amazing
./gradlew build
git add .
git commit -m "Add amazing feature"
git push origin feature/amazing
```

提交代码前请确保：

- 项目能够正常构建（`./gradlew build` 通过）
- 核心战斗机制没有被意外破坏
- 没有引入不必要的默认行为变化
- 相关修改已经经过实际测试
- 没有破坏已有版本或 Mod Loader 的兼容性

---

## 🤖 AI 辅助开发

CTDI 的开发过程中使用 AI 作为辅助开发工具（源码分析、实现、Debug、构建、文档）。

AI 生成的代码和内容会经过项目维护者审查、修改与测试。项目的整体方向、功能设计、
代码取舍、测试结果以及最终发布由项目维护者决定。

开发环境与操作说明见 [HANDOVER.md](HANDOVER.md)，版本历史见 [CHANGELOG.md](CHANGELOG.md)。

---

## 📜 许可证

本项目基于 **MIT License** 开源。

MIT © 2026 zzy89216-gif

> Minecraft 本体及 Mojang/微软资产不属于本仓库。

---

<div align="center">

# ⚔️ Remove the limits. Keep the combat.

*Made for Minecraft combat.*

⭐ **如果这个模组对你有帮助，欢迎给项目一个 Star。**

</div>
