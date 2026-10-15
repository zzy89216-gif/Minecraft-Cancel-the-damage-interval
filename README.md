<div align="center">

# ⚔️ Cancel The Damage Interval

**彻底移除无敌帧与攻击冷却 —— 回归纯手速战斗**

*Minecraft Java Edition 战斗机制重构模组*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%2026.3%20%7C%201.12.2-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Forge%20%7C%20Fabric-orange)](https://minecraft.wiki/w/Mods)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2017%20%7C%2025-red)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/license/mit/)
[![Status](https://img.shields.io/badge/Status-Archived-lightgrey)](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval)

</div>

---

> [!WARNING]
> **项目已归档，停止维护**
>
> 由于个人学业原因及个人安排，项目作者决定停止本项目的后续开发与维护。
>
> 本项目不再提供功能更新、问题修复、新版本适配或新版本发布。现有代码、文档及历史发行版本继续保留，供查阅、学习和自行使用。
>
> 项目原有路线图中的计划功能不再代表后续开发承诺。使用者请自行判断当前版本的适用性与兼容性。
>
> 感谢所有曾经关注、使用或为本项目提供帮助的人。

---

## 📖 项目简介

**Cancel The Damage Interval（CTDI）** 是一个 Minecraft Java Edition 战斗机制重构模组。

> **移除原版战斗系统中的无敌帧与攻击冷却，让攻击频率真正由玩家的操作决定。**

CTDI 不添加武器、职业或独立战斗系统，只是在尽量少改原版机制的前提下，移除限制连续攻击的两处代码。

> **Remove the limits. Keep the combat.**
>
> **手速就是 DPS。**

本项目已停止维护。以下内容用于记录现有版本的设计、实现、兼容性及测试情况，不代表项目仍在持续开发。

---

## ⚔️ 核心机制

| 限制项 | 原版行为 | CTDI |
|---|---|---|
| **无敌帧（i-frames）** | 受击后有一个短暂免疫窗口；窗口内再次挨打会被直接丢弃或只结算差额 | 🚫 移除 |
| **攻击冷却（Attack Cooldown）** | 冷却未满时攻击伤害按 `0.2 + s² × 0.8` 打折，且无法满足全力攻击的暴击条件 | 🚫 移除 |

其余原版战斗逻辑尽可能保留，包括武器伤害、附魔、药水、暴击、属性、击退、护盾、盔甲及原版伤害计算。

实际效果以对应版本的实现及测试结果为准。

---

## 📦 支持范围

以源码、构建配置和历史 Release 为准。

| Minecraft | Mod Loader | Java | 历史发布状态 | 实机验证 |
|---|---|---|---|---|
| **1.20.1** | **Forge 47.2.0** | 17 | 已发布 | 同 tick A/B 通过 |
| **1.20.1** | **Fabric Loader 0.15.11** | 17 | 已发布 | 同 tick A/B 通过 |
| **26.3** | **Fabric Loader 0.19.5** | 25 | 已发布 | 同 tick A/B 通过 |
| **1.12.2** | **Forge 14.23.5.2859** | 8 | 已发布 | 同 tick A/B 通过 |

> 「同 tick A/B 通过」指在真实专用服务端上，以同一 tick 内两次 5 点伤害进行对照：安装 CTDI 时累计损失 10 点生命值，未安装时累计损失 5 点。
>
> 具体测试方法及记录见 [HANDOVER.md](HANDOVER.md)。

以上为历史测试记录。项目归档后，不再保证新环境、新版本或其他模组组合的兼容性。

---

## 🔧 技术实现（各版本差异，别照抄）

两个机制在不同 Minecraft 版本里的位置、字段名和方法名均不相同。CTDI 的各版本实现基于对应版本的字节码、反编译源码及 Forge patch 进行确认。

| 目标 | 无敌帧：伤害入口 | 无敌帧：窗口字段 | 旁路标签 | 攻击冷却方法 | 实现方式 |
|---|---|---|---|---|---|
| 1.20.1 Forge / Fabric | `LivingEntity#hurt` | `invulnerableTime` | `BYPASSES_INVULNERABILITY` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 26.3 Fabric | `LivingEntity#hurtServer` | `damageCooldownTime` | `BYPASSES_COOLDOWN` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 1.12.2 Forge | `EntityLivingBase#attackEntityFrom` | `hurtResistantTime` | 硬编码，无标签 | `EntityPlayer#getCooledAttackStrength` | 纯 Forge 事件，无 Mixin |

1.12.2 版本使用 Forge 事件处理，无需 Mixin。`LivingAttackEvent` 用于处理伤害入口，`AttackEntityEvent` 用于处理攻击冷却相关逻辑；冷却字段 `ticksSinceLastSwing` 通过 Forge 官方 `ObfuscationReflectionHelper` 写入。

上述内容记录的是各版本原有实现，不代表这些实现已适配后续版本的 API 变化。

### 无敌帧

各版本的判定逻辑可概括为以下形式，具体字段名和标签名随版本变化：

```java
if (窗口字段 > 10.0f && !source.is(旁路标签)) {
    if (伤害 <= 上次伤害) return false;
    applyDamage(伤害 - 上次伤害);
} else {
    上次伤害 = 伤害;
    窗口字段 = 20;
    applyDamage(伤害);
}
````

CTDI 在入口方法头部清零对应窗口字段，使伤害进入完整结算分支。

* 注入点早于原版读取该字段，同一服务端 tick 内多次命中可以分别结算。

* 只修改对应的无敌帧窗口字段，不主动修改 `hurtTime`，以保留原版受击红屏及动画行为。

* 1.20.1 的 `invulnerableTime` 声明在 `Entity`；26.3 的 `damageCooldownTime` 是 `LivingEntity` 的 public 字段。

### 攻击冷却

原版攻击冷却影响攻击伤害、附魔伤害及全力攻击相关判定。其逻辑可概括为：

```java
float s = player.getAttackStrengthScale(0.5F);
伤害 = 基础伤害 * (0.2F + s * s * 0.8F);
附魔伤害 *= s;
boolean 全力 = s > 0.9F;
```

CTDI 通过对应版本的实现，使攻击冷却进度方法恒返回 `1.0F`，等价于让攻击始终处于满冷却状态。

这使每次攻击都能按满冷却伤害结算，并满足全力攻击相关判定条件；其他伤害计算尽可能沿用原版逻辑。

### 副作用与限制

由于无敌帧处理位于通用伤害入口，摔落、岩浆等环境伤害也可能失去原版的无敌帧保护。

这是当前实现范围所带来的行为变化。原有计划中提及的配置项和白名单尚不能视为已实现功能。

> 注：1.20.1 的 `BYPASSES_INVULNERABILITY` 只包含 `out_of_world`、`generic_kill` 等对应版本定义的旁路类型，不包含普通摔落或着火伤害。这些伤害在原版中的具体处理路径仍需以对应版本源码为准。

### 📥 安装

### 前置需求

以下信息依据原有发布配置整理。

| 目标                 | 需要                                               | 不需要                   |
| ------------------ | ------------------------------------------------ | --------------------- |
| ctdi-forge-1.20.1  | Minecraft 1.20.1、Forge 47.x、Java 17（Java 21 可运行） | 无其他模组前置               |
| ctdi-fabric-1.20.1 | Minecraft 1.20.1、Fabric Loader ≥ 0.15.0、Java 17  | Fabric API            |
| ctdi-fabric-26.3   | Minecraft 26.3、Fabric Loader ≥ 0.19.5、Java 25    | Fabric API            |
| ctdi-forge-1.12.2  | Minecraft 1.12.2、Forge 14.23.5.2859、Java 8       | MixinBooter / coremod |

CTDI 自身不依赖其他模组作为基础前置。Fabric 版未将 Fabric API 声明为必需依赖，1.12.2 版采用纯 Forge 事件实现。

多人游戏中，客户端与服务端均应安装对应版本的 CTDI。伤害判定以服务端为准。

### 安装步骤

* 安装对应版本的 Minecraft、Mod Loader 和 Java。

* 前往 [GitHub Releases](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases) 下载匹配的 JAR 文件。

* 将 JAR 放入游戏目录的 `mods/` 文件夹。

* 启动游戏。

* 多人游戏请确保客户端和服务端安装兼容版本的模组。

历史发布文件名称示例：

* `ctdi-forge-1.20.1-*.jar`

* `ctdi-fabric-1.20.1-*.jar`

* `ctdi-fabric-26.3-*.jar`

* `ctdi-forge-1.12.2-*.jar`

> \[!NOTE\] 项目已归档。下载前请确认文件来源、游戏版本及加载器版本。归档不代表历史文件已失效，但不再保证后续兼容性。

### 🔨 从源码构建 · Building from source

不同目标使用不同的 JDK 和 Gradle 配置。

```bash
git clone https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval.git
cd Minecraft-Cancel-the-damage-interval
```

### 1.20.1 Forge

需要 JDK 17。

```bash
cd forge/1.20.1
./gradlew build
```

### 1.20.1 Fabric

需要 JDK 17。

```bash
cd fabric/1.20.1
./gradlew build
```

### 26.3 Fabric

需要 JDK 25。

```bash
cd fabric/26.3
./gradlew build
```

### 1.12.2 Forge

需要 JDK 8。原有构建环境使用 ForgeGradle 3、Gradle 4.9 和 MCP snapshot。

```bash
cd forge/1.12.2
./gradlew build
```

构建产物位于各目标对应的 `build/libs/` 目录。

首次构建需要下载 Gradle 发行版与 Minecraft 相关依赖。实际耗时取决于网络、设备和构建环境。

> \[!NOTE\] 以上为原有构建方式。由于项目已停止维护，不保证依赖仓库、Gradle 版本或构建环境发生变化后仍可直接构建。

### 📊 已验证 / 未验证

以下为项目归档前记录的验证结果。

| 项目            | 历史验证状态                            |
| ------------- | --------------------------------- |
| 各目标 Gradle 构建 | 四个目标均曾真实执行通过，使用 JDK 8 / 17 / 25   |
| 注入点正确性        | 曾逐版本对照真实字节码、反编译源码及 Forge patch 确认 |
| 无敌帧移除效果       | 四个目标均曾在真实专用服务端进行同 tick A/B 对照     |
| 攻击冷却效果        | 注入点与运行时应用已验证；玩家挥砍手感未在图形客户端实测      |
| 客户端表现         | 红屏、准星冷却圈等表现未完成图形客户端实测             |

> 没有验证过的内容不应视为已验证。详细清单与复现方法见 [HANDOVER.md](https://HANDOVER.md)。

以上为历史记录，不构成对未来版本、其他模组组合或不同运行环境的兼容性保证。

### 🗺️ 原有路线图 · Historical roadmap

项目归档前曾规划以下方向：

| 方向                                                        | 归档前状态      |
| --------------------------------------------------------- | ---------- |
| 1.20.1 Forge / 1.20.1 Fabric / 26.3 Fabric / 1.12.2 Forge | 已完成并进行实机验证 |
| 配置系统：两项机制独立开关、白名单                                         | 规划中，未承诺完成  |
| NeoForge、Quilt、更多 Minecraft 版本                            | 规划中，未承诺完成  |
| GitHub Actions CI：push 自动构建四个目标                           | 规划中，未承诺完成  |

项目归档后，上述计划不再推进。 该表仅用于记录项目历史，不代表未来开发承诺。

### 🤝 贡献与支持

本项目已停止维护，不再接受常规功能开发、问题修复或版本适配工作。

现有 Issue、PR、代码和文档可作为历史记录与自行研究的参考，但不保证会获得回复、合并或修复。

如需自行维护或移植，可参考：

* [HANDOVER.md](https://HANDOVER.md)：开发交接、技术细节及复现方法。

* [CHANGELOG.md](https://CHANGELOG.md)：历史版本变更记录。

* [GitHub Issues](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/issues)：历史问题记录。

* [GitHub Releases](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases)：历史发行版本。

### 📜 许可证 · License

本项目采用 MIT License，详见 LICENSE。

Minecraft 本体及 Mojang / Microsoft 的相关资产不属于本仓库，仍应遵守各自适用的许可证与使用条款。

```
