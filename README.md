<div align="center">

# ⚔️ Cancel The Damage Interval

**彻底移除无敌帧与攻击冷却 —— 回归纯手速战斗**

*Minecraft Java Edition 战斗机制重构模组*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%2026.3%20%7C%201.12.2-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Forge%20%7C%20Fabric-orange)](https://minecraft.wiki/w/Mods)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2017%20%7C%2025-red)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/license/mit/)
[![Status](https://img.shields.io/badge/Status-Long--Term%20Maintenance-brightgreen)](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval)

</div>

---

## 📖 项目简介

**Cancel The Damage Interval**（**CTDI**）是一个 Minecraft Java Edition 战斗机制重构模组：

> **移除原版战斗系统中的无敌帧与攻击冷却，让攻击频率真正由玩家的操作决定。**

CTDI 不添加武器、职业或独立战斗系统，只是在**尽量少改原版机制**的前提下，移除限制连续攻击的两处代码。

> **Remove the limits. Keep the combat.**
> **手速就是 DPS。**

---

## ⚔️ 核心机制

| 限制项 | 原版行为 | CTDI |
|:---|:---|:---|
| **无敌帧（i-frames）** | 受击后有一个短暂免疫窗口；窗口内再次挨打会被**直接丢弃**或**只结算差额** | 🚫 移除 |
| **攻击冷却（Attack Cooldown）** | 冷却未满时攻击伤害按 `0.2 + s²·0.8` 打折，且无法暴击 | 🚫 移除 |

其余原版战斗逻辑**全部保留**：武器伤害、附魔、药水、暴击、属性、击退、护盾、盔甲、原版伤害计算。

---

## 📦 支持范围

以**源码、构建配置和 Release** 为准。

| Minecraft | Mod Loader | Java | 状态 | 实机验证 |
|:---|:---|:---:|:---|:---|
| **1.20.1** | **Forge 47.2.0** | 17 | ✅ 已发布 | ✅ 同 tick A/B 通过 |
| **1.20.1** | **Fabric**（loader 0.15.11） | 17 | ✅ 已发布 | ✅ 同 tick A/B 通过 |
| **26.3** | **Fabric**（loader 0.19.5） | 25 | ✅ 已发布 | ✅ 同 tick A/B 通过 |
| **1.12.2** | **Forge 14.23.5.2859** | 8 | ✅ 已发布 | ✅ 同 tick A/B 通过 |

> "同 tick A/B 通过"= 在真实专用服务端上，用同一 tick 内两次 5 点伤害做对照：
> 装 CTDI 掉 10 点，不装 CTDI 只掉 5 点。详见 [HANDOVER.md](HANDOVER.md)。

---

## 🔧 技术实现（各版本差异，别照抄）

两个机制在不同 Minecraft 版本里的**位置、字段名、方法名都不同**。CTDI 每个版本都按该版本的真实字节码重新确认过：

| 目标 | 无敌帧：伤害入口 | 无敌帧：窗口字段 | 旁路标签 | 攻击冷却方法 | 实现方式 |
|:---|:---|:---|:---|:---|:---|
| 1.20.1 Forge / Fabric | `LivingEntity#hurt` | `invulnerableTime` | `BYPASSES_INVULNERABILITY` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 26.3 Fabric | `LivingEntity#hurtServer`（已拆客户端/服务端） | `damageCooldownTime` | `BYPASSES_COOLDOWN` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 1.12.2 Forge | `EntityLivingBase#attackEntityFrom` | `hurtResistantTime` | 硬编码，无标签 | `EntityPlayer#getCooledAttackStrength` | **纯 Forge 事件**（无 Mixin） |

> 1.12.2 不需要 Mixin：Forge 把 `LivingAttackEvent` 插在伤害入口的**第一行**、把 `AttackEntityEvent`
> 插在冷却值被读取**之前**，事件里就能干净地清掉无敌帧窗口、把冷却计时器拉满。
> 冷却字段 `ticksSinceLastSwing` 是 protected，用 Forge 官方 `ObfuscationReflectionHelper` 写入。

### 无敌帧

所有版本的判定都长这样（字段名/标签名随版本变化）：

```java
if (窗口字段 > 10.0f && !source.is(旁路标签)) {
    if (伤害 <= 上次伤害) return false;        // 这次攻击被完全丢弃
    applyDamage(伤害 - 上次伤害);              // 否则只结算差额
} else {
    上次伤害 = 伤害;
    窗口字段 = 20;                            // 重置窗口
    applyDamage(伤害);                        // 完整伤害
}
```

CTDI 在**入口方法头部**把窗口字段清零，于是所有伤害都走完整结算分支：

- 注入点早于原版读取该字段，**同一服务端 tick 内多次命中同样全部生效**（不是靠每 tick 清零）
- 只改这一个字段，`hurtTime`（受击红屏/动画）不碰，原版表现不变
- 1.20.1 的 `invulnerableTime` 声明在 `Entity`，26.3 的 `damageCooldownTime` 是 `LivingEntity` 的 public 字段

### 攻击冷却

```java
float s = player.getAttackStrengthScale(0.5F);   // 0..1 的冷却进度
伤害 = 基础伤害 * (0.2F + s * s * 0.8F);         // 冷却不满就掉伤害
附魔伤害 *= s;                                    // 附魔也被打折
boolean 全力 = s > 0.9F;                          // 暴击/击退/冲刺加成门槛
```

CTDI 让该方法恒返回 `1.0F`，等价于"攻击永远满冷却"：每次挥砍都是满伤害且可暴击，其余计算保持原版。

### 副作用（务必知道）

注入的是**通用伤害入口**，所以**摔落、岩浆等环境伤害也失去了无敌帧窗口**（原版短时间内连摔两次，第二次会被差额结算；现在是两次全额）。这是"移除无敌帧"的定义决定的，未来的配置项/白名单会用来自定义。

> 注：`BYPASSES_INVULNERABILITY`（1.20.1）只含 `out_of_world`、`generic_kill`，**不包含**摔落/着火，这两类本来就走完整分支。

---

## 📥 安装

### 前置需求（已按发布产的依赖声明核对）

| 目标 | 需要 | **不需要** |
|:---|:---|:---|
| `ctdi-forge-1.20.1` | Minecraft 1.20.1、Forge **47.x**（`mods.toml`: forge `[47,)`、minecraft `[1.20.1,1.21)`）、Java 17（21 可运行） | 无其它前置 |
| `ctdi-fabric-1.20.1` | Minecraft 1.20.1、Fabric Loader **≥0.15.0**（`fabric.mod.json`: minecraft `~1.20.1`、java `>=17`） | **不需要 Fabric API** |
| `ctdi-fabric-26.3` | Minecraft 26.3、Fabric Loader **≥0.19.5**、**Java 25** | **不需要 Fabric API** |
| `ctdi-forge-1.12.2` | Minecraft 1.12.2、Forge **14.23.5.2859**、**Java 8** | **不需要 MixinBooter / coremod**（纯事件实现，jar 里没有 mixin 类） |

> CTDI 自身不依赖任何其它模组：Fabric 版没把 fabric-api 写进 `depends`，
> 1.12.2 版完全不用 Mixin。只有"多人游戏时客户端与服务端都装 CTDI"这一条额外要求。

### 安装步骤

1. 按上表装好对应 Minecraft + Loader + Java。
2. 到 [Releases](https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases) 下载匹配的 jar：
   - `ctdi-forge-1.20.1-*.jar`
   - `ctdi-fabric-1.20.1-*.jar`
   - `ctdi-fabric-26.3-*.jar`
   - `ctdi-forge-1.12.2-*.jar`
3. 放进 `mods/` 目录，启动游戏。
4. **多人游戏请客户端和服务端都装**：伤害判定以服务端为准。

---

## 🔨 构建（各目标 JDK 要求不同）

```bash
git clone https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval.git
cd Minecraft-Cancel-the-damage-interval

# 1.20.1 Forge —— JDK 17
cd forge/1.20.1 && ./gradlew build

# 1.20.1 Fabric —— JDK 17
cd fabric/1.20.1 && ./gradlew build

# 26.3 Fabric —— JDK 25（26.x 起 Mojang 不再混淆，loom 无需 mappings 依赖）
cd fabric/26.3 && ./gradlew build

# 1.12.2 Forge —— JDK 8（ForgeGradle 3 + Gradle 4.9 + MCP snapshot）
cd forge/1.12.2 && ./gradlew build
```

产物在各自 `build/libs/` 下。首次构建需要下载 Gradle 发行版与 Minecraft 工件，
在 ARM 手机/容器上实测约 6–12 分钟（1.12.2 首次要反编译整个 Minecraft，更久）。

---

## 📊 已验证 / 未验证

| 项目 | 状态 |
|:---|:---|
| 各目标 Gradle 构建 | ✅ 四个目标全部真实执行通过（JDK 8 / 17 / 25） |
| 注入点正确性 | ✅ 逐版本对照**真实字节码 / 反编译源码 / Forge patch** 确认（不靠记忆） |
| 无敌帧移除效果 | ✅ **四个目标全部**在真实专用服务端做了**同 tick A/B 对照** |
| 攻击冷却效果 | ⚠️ 注入点与运行时应用已验证；**玩家挥砍手感未在图形客户端实测** |
| 客户端表现（红屏、准星冷却圈） | ⚠️ 未实测（需要图形客户端） |

> 没验证过的东西不写成已验证。详细清单与复现方法见 [HANDOVER.md](HANDOVER.md)。

---

## 🗺️ 路线图

| 方向 | 状态 |
|:---|:---|
| 1.20.1 Forge / 1.20.1 Fabric / 26.3 Fabric / 1.12.2 Forge | ✅ 已完成并实测 |
| 配置系统（两项机制独立开关、白名单） | 📋 规划 |
| NeoForge、Quilt、更多 MC 版本 | 📋 规划 |
| GitHub Actions CI（push 自动构建四目标） | 📋 规划 |

---

## 🤝 贡献

欢迎 Issue / PR。提交前请确保：`./gradlew build` 通过、核心机制没被破坏、改动经过实际测试。
开发与交接细节见 [HANDOVER.md](HANDOVER.md)，版本历史见 [CHANGELOG.md](CHANGELOG.md)。

---

## 📜 许可证

**MIT License**，MIT © 2026 zzy89216-gif。Minecraft 本体及 Mojang/微软资产不属于本仓库。

---

<div align="center">

# ⚔️ Remove the limits. Keep the combat.

⭐ 如果这个项目对你有帮助，欢迎 Star。

</div>
