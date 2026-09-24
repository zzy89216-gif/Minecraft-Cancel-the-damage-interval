# CTDI 交接日志

> 写给下一个接手本项目的开发者 / AI Agent。
> 最后更新：2026-09-24，v0.2.0（四目标：1.20.1 Forge / 1.20.1 Fabric / 26.3 Fabric / 1.12.2 Forge）

---

## 1. 当前状态（一句话）

**CTDI 的四个目标全部实现、构建通过、并在真实专用服务端上做了同 tick A/B 实测。**

- 仓库：https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval （public，MIT）
- 发布：https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases （v0.2.0 含四个 jar）
- 实测结论（同 tick 两次相同伤害，落在原版无敌帧窗口内）：

| 目标 | 有 CTDI | 无 CTDI（对照） |
|:---|:---|:---|
| 1.20.1 Forge | `19.0f -> 9.0f` | `20.0f -> 15.0f` |
| 1.20.1 Fabric | `20.0f -> 10.0f` | `20.0f -> 15.0f` |
| 26.3 Fabric | `20.0f -> 10.0f` | `20.0f -> 15.0f` |
| 1.12.2 Forge | `20.0f -> 10.0f` | `20.0f -> 15.0f` |

**未验证的部分**见 §5，尤其是"攻击冷却在图形客户端里的手感"。

---

## 2. 仓库结构与工具链

四个目标各自是**独立 Gradle 工程**（工具链互不兼容，不能合成一个多模块构建）：

| 目录 | MC | Loader | JDK | Gradle | 构建插件 | 映射 |
|:---|:---|:---|:---:|:---|:---|:---|
| `forge/1.20.1` | 1.20.1 | Forge 47.2.0 | 17 | 8.1.1 | ForgeGradle 6 + Sponge Mixin 插件 | official (Mojang) |
| `fabric/1.20.1` | 1.20.1 | Fabric loader 0.15.11 | 17 | 8.8 | fabric-loom 1.6.12 | **Mojang official**（`loom.officialMojangMappings()`） |
| `fabric/26.3` | 26.3 | Fabric loader 0.19.5 | **25** | **9.5.1** | net.fabricmc.fabric-loom 1.17.21 | **无需映射**（26.x 起官方 jar 不再混淆） |
| `forge/1.12.2` | 1.12.2 | Forge 14.23.5.2859 | **8** | **4.9** | ForgeGradle 3 | MCP `snapshot 20171003-1.12` |

根目录：`README.md`（支持矩阵）、`CHANGELOG.md`、`HANDOVER.md`、`LICENSE`、`tools/damage_test.py`（实机回归测试）。

JDK 位置（本机）：`/usr/lib/jvm/java-17-openjdk-arm64`、`/opt/jdks/jdk-25.0.4.1+1`、`/opt/jdks/jdk8u504-b01`。

---

## 3. 实现位置（各版本差异很大，**不要跨版本照抄**）

### 3.1 差异总表

| 目标 | 伤害入口 | i-frame 字段 | 旁路标签 | 冷却方法 | 实现方式 |
|:---|:---|:---|:---|:---|:---|
| 1.20.1 | `LivingEntity#hurt` | `invulnerableTime`（`Entity`，protected） | `BYPASSES_INVULNERABILITY` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 26.3 | `LivingEntity#hurtServer` | `damageCooldownTime`（`LivingEntity`，**public**） | `BYPASSES_COOLDOWN` | `Player#getAttackStrengthScale` | Mixin ×2 |
| 1.12.2 | `EntityLivingBase#attackEntityFrom` | `hurtResistantTime`（`Entity`，**public**） | 无（硬编码） | `EntityPlayer#getCooledAttackStrength` | **纯 Forge 事件** |

### 3.2 1.20.1（Forge + Fabric 共用同一份 Mixin 源码）

- `InvulnerabilityMixin`：`@Inject(method="hurt", at=@At("HEAD"))` → `invulnerableTime = 0`。
  原版 `hurt` 内：`if (invulnerableTime > 10f && !source.is(BYPASSES_INVULNERABILITY))` 分支
  会丢弃伤害或只结算差额；清零后全部走完整伤害分支。
- `AttackCooldownMixin`：`getAttackStrengthScale` HEAD 处 `setReturnValue(1.0f)`。
  该方法同时控制基础伤害乘数 `0.2+s²·0.8`、附魔乘数 `×s`、以及 `s > 0.9f` 的暴击/击退门槛。
- 只改 `invulnerableTime` 一个字段；`hurtTime`（受击红屏/动画）刻意不动。
- Fabric 侧用 Mojang 官方映射，因此**两个 Mixin 文件与 Forge 侧完全一致**，只有入口类/元数据不同。

### 3.3 26.3（Fabric）

- 26.x 起伤害流程拆成客户端/服务端，入口是 `LivingEntity#hurtServer(ServerLevel, DamageSource, float)`，
  `Player#hurtServer`、`ServerPlayer#hurtServer` 都 `super` 到它，所以注入 LivingEntity 覆盖全部玩家路径。
- 字段改名 `damageCooldownTime`（public），标签改名 `BYPASSES_COOLDOWN`。
- `Player#getAttackStrengthScale(float)` 方法名未变，冷却 Mixin 与 1.20.1 相同。

### 3.4 1.12.2（Forge，**无需 Mixin / 无需 coremod**）

依据（都已核对 Forge 1.12.2 的 patch 与反编译源码）：

1. `LivingAttackEvent` 由 `ForgeHooks.onLivingAttack` 触发，而 Forge 把它插在 `EntityLivingBase#attackEntityFrom`
   **第一行**（早于原版所有判断，包括 i-frame 检查）→ 事件里 `entity.hurtResistantTime = 0` 即可。
2. `AttackEntityEvent` 由 `ForgeHooks.onPlayerAttackTarget` 触发，位于 `attackTargetEntityWithCurrentItem`
   第一行，**早于** `getCooledAttackStrength(0.5f)` 的读取 → 在这里把冷却计时器拉满，连同一 tick 内的多次攻击也全部满伤害。
3. 冷却字段是 `EntityLivingBase.ticksSinceLastSwing`（**protected**，SRG `field_184617_aD`），
   用 Forge 官方 `ObfuscationReflectionHelper.findField(...)` 拿到 Field 后每 tick/每次攻击写入。
4. `PlayerTickEvent` 也写同一个字段，目的是让客户端的攻击指示条保持一致。

> 因为走的是事件而不是 Mixin，1.12.2 目标**没有 refmap / coremod / MixinBooter 依赖**，安装最简单。

---

## 4. 构建链与踩过的坑

### 1.20.1 Forge
- `id 'org.spongepowered.mixin'` + `mixin { add sourceSets.main, "ctdi.refmap.json"; config "ctdi.mixins.json" }`
  + `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`，jar manifest 里写 `MixinConfigs`。
- `compatibilityLevel` 用 `JAVA_13`：写 `JAVA_17` 会每次启动刷一条"Mixin 上限是 JAVA_13"的警告（功能无影响）。
- refmap 已核对：`hurt -> m_6469_`、`getAttackStrengthScale -> m_36403_`。

### 1.20.1 Fabric
- loom 1.6.12 + Gradle 8.8；`mappings loom.officialMojangMappings()`。
- mixin 配置里**不要**写 `refmap` 字段：loom 会把 refmap 名自动注入到 jar 内的 mixin 配置里。
- 产物里 refmap 是 intermediary 名（`class_1309`/`method_5643`），属正常。

### 26.3 Fabric
- **26.x 起 Mojang 不再混淆 jar**：版本 JSON 里没有 `client_mappings`，Yarn 没有 26.x 条目，
  intermediary 是 `0.0.0` 占位。所以 `dependencies` 里**不写 mappings**，类名就是官方名。
- **loom 1.17 移除了 `modImplementation`**，官方模板用普通 `implementation`（写错会报 `Could not find method modImplementation()`）。
- JDK 必须 25（Gradle 9.5.1 + MC 26.3 都要求）。

### 1.12.2 Forge
- **FG3 默认用平台编码**（容器里是 ASCII），源码含中文注释会报 `unmappable character for encoding ASCII` →
  在 build.gradle 里加 `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }`。
- FG3 不会把 MC 反编译成源码；要读源码用 FG 生成的 `build/_renameJarInPlace_*/output.jar`（**MCP 命名**），
  直接 CFR 反编译即可。Forge 的插入点则看 userdev jar 里的 `patches/*.patch`。
- 服务端启动：installer 装完后用 `java -jar forge-1.12.2-14.23.5.2859.jar nogui`（注意不是 `--nogui`）。

---

## 5. 已做过的验证 / 未验证的

| 验证项 | 方式 | 结果 |
|:---|:---|:---|
| 四目标构建 | `./gradlew build`（各自 JDK） | ✅ 全部 BUILD SUCCESSFUL |
| 注入点正确性 | 对照**真实字节码/反编译源码/Forge patch**逐一确认 | ✅ 四目标均确认 |
| Mixin 运行时应用 | 服务端 + `-Dmixin.debug.verbose=true` | ✅ `Mixing ... into LivingEntity / Player`（1.20.1、26.3） |
| 服务端加载 | 真实专用服务端（Forge 1.20.1 / Fabric 1.20.1 / Fabric 26.3 / Forge 1.12.2） | ✅ 均无崩溃，`[CTDI] loaded` |
| **无敌帧移除（功能实测）** | 同一服务端 tick 内两次相同伤害，A/B 对照 | ✅ 四目标全部通过（见 §1 表） |
| 攻击冷却（运行时） | — | ⚠️ 注入点/字段写入路径已验证；**图形客户端挥砍手感未测** |
| 客户端表现（红屏、准星冷却圈） | — | ⚠️ 未测（本机无图形客户端） |

**没验证过的东西不要写成已验证。**

---

## 6. 如何复现实机验证

### 6.1 各目标服务端搭建

```bash
# 1.20.1 Forge（JDK 17）
mkdir -p ~/srv-forge-1.20.1/mods && cd ~/srv-forge-1.20.1
curl -LO https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.2.0/forge-1.20.1-47.2.0-installer.jar
java -jar forge-1.20.1-47.2.0-installer.jar --installServer
echo "eula=true" > eula.txt
cp <repo>/forge/1.20.1/build/libs/*.jar mods/
./run.sh --nogui                     # 注意 --nogui 要传给 run.sh，不是 installer

# 1.20.1 Fabric（JDK 17）
mkdir -p ~/srv-fabric-1.20.1/mods && cd ~/srv-fabric-1.20.1
curl -LO "https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.15.11/1.0.1/server/jar"
# 若该域名超时是临时网络问题，重试即可；launcher 内部要访问 launchermeta.mojang.com
java -jar server.jar --nogui 2>/dev/null || java -jar fabric-server-launch.jar --nogui
cp <repo>/fabric/1.20.1/build/libs/*.jar mods/

# 26.3 Fabric（JDK 25）
mkdir -p ~/srv-fabric-26.3/mods && cd ~/srv-fabric-26.3
curl -LO "https://meta.fabricmc.net/v2/versions/loader/26.3/0.19.5/1.1.2/server/jar"
/opt/jdks/jdk-25*/bin/java -Xmx3G -jar fabric-server-launch.jar --nogui
cp <repo>/fabric/26.3/build/libs/*.jar mods/

# 1.12.2 Forge（JDK 8）
mkdir -p ~/srv-forge-1.12.2/mods && cd ~/srv-forge-1.12.2
curl -LO https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2859/forge-1.12.2-14.23.5.2859-installer.jar
/opt/jdks/jdk8*/bin/java -jar forge-installer.jar --installServer
/opt/jdks/jdk8*/bin/java -Xmx2G -jar forge-1.12.2-14.23.5.2859.jar nogui
cp <repo>/forge/1.12.2/build/libs/*.jar mods/
```

**每个服务端都用不同的 `server-port` 与 `rcon.port`**，否则会因端口占用启动失败（表现为
`Failed to initialize server` 崩溃，很容易误判成模组问题）。

### 6.2 同 tick 测试

1.20.1 / 26.3 用数据包函数保证"同一 tick 两次伤害"：

```
# 1.20.1：world/datapacks/ctditest/... , pack_format 15, 目录名是 functions（复数）
# 26.3  ：同上，pack_format 121, 目录名是 function（单数，1.21.2+ 改名）
data/ctditest/functions/double_hit.mcfunction:
    damage @e[type=minecraft:zombie,limit=1] 5 minecraft:generic
    damage @e[type=minecraft:zombie,limit=1] 5 minecraft:generic
```

然后：

```bash
CTDI_RCON_PORT=25575 python3 tools/damage_test.py FORGE-1.20.1        # 有 CTDI
# 把 mods/ 里的 ctdi*.jar 移走、重启服务端、再跑一次 = 对照组
```

1.12.2 **没有 `/damage`、`/data`、`/forceload` 命令**，所以验证方式是：用一个
**独立的测试驱动模组**（放在仓库外，例如 `/tmp/ctditestmod`）注册 `/ctditest <amount>`，
在同一个命令调用里对僵尸施加两次相同伤害，并**先把僵尸回满血、把 hurtResistantTime 清零**
以保证基线干净。A/B 两轮都要装这个测试驱动（否则对照组跑不了测试命令），CTDI 则只在其中一轮装。

### 6.3 踩过的坑（别重复）

- **端口冲突**：旧服务端没退干净 → 新服务端 `Failed to initialize server` 崩溃，看着像模组崩溃。
- **区块未加载**：召唤出来的实体会落在未加载区块，`@e` 找不到（报 `No entity was found`）；
  测试脚本已内置"召唤失败就重试"。
- **环境伤害污染测量**：移除无敌帧后，**窒息/岩浆等环境伤害每 tick 都生效**，僵尸如果卡在方块里会持续掉血，
  读数会变成 delta 15 之类。测试脚本现在默认召唤到 y=250 高空，并且打完立刻读数。
- **`pkill -f <关键词>` 会匹配到自己这条命令**，把自己的 shell 杀掉；用
  `ps -eo pid,comm,args | awk '$2=="java" && /关键词/ {print $1}'` 精确定位。
- **两个服务端写同一个日志文件**会混出看不懂的栈，误判成崩溃。
- **1.12.2 原版竞态**：快速 `kill`+`summon` 偶发 `ConcurrentModificationException`（`EntityTracker`），
  与 CTDI 无关；命令之间加 1-2 秒延时可避免。
- **`user_jvm_args.txt` / 配置文件里追加参数**时注意别追加到注释行里（会静默失效）。

---

## 7. 已知取舍与限制

1. **对所有 LivingEntity 生效**（玩家、怪、Boss 都失去无敌帧）。规划中的实体白名单用于此。
2. **环境伤害同样失去无敌帧窗口**（摔落、岩浆、窒息…）：这是"移除无敌帧"的定义决定的直接结果。
   1.12.2/1.20.1 的旁路标签只覆盖 `out_of_world`/`generic_kill` 之类，**不含**摔落/着火。
3. **没有配置项**：两项机制都是硬移除，没有开关。
4. **Java 21 的准确含义**：1.20.1 两个目标的字节码是 17，可在 17/21 运行；
   但 1.20.1 的构建 wrapper 是 Gradle 8.1.1（1.20.1 Forge 目标）— 要用 JDK 21 **构建**需升 wrapper 到 8.5+。
   26.3 目标必须 JDK 25，1.12.2 目标必须 JDK 8。
5. **1.12.2 用反射写 protected 字段**：SRG 名 `field_184617_aD` 是硬编码的；若换 1.12.2 的其他 Forge 版本
   或改动 MC 版本，必须重新确认 SRG 名。找不到字段时会记录错误日志并退化为原版冷却（不会崩）。

---

## 8. 接手后建议做的事（按优先级）

1. **图形客户端实测**：进单人世界连点，确认 ①连击全程满伤害 ②暴击正常 ③HUD 攻击指示条行为符合预期
   （1.20.1/26.3 恒满；1.12.2 由 PlayerTickEvent 维持）。
2. **多人实测**：服务端 + 客户端都装 CTDI，验证高延迟/高 CPS 下的表现。
3. **配置系统**：两项机制独立开关 + 实体/伤害类型白名单（能顺便解决环境伤害的副作用）。
4. **GitHub Actions CI**：四目标矩阵构建（不同 JDK），把"构建通过"变成 PR 门禁。
5. **更多版本**：1.21.x Forge/NeoForge。注意 1.20.5+ 与 26.x 的伤害流程写法差异（见 §3）。

---

## 9. GitHub 操作备忘

### 发版时改版本号的位置（别漏）

| 目标 | 改哪里 |
|:---|:---|
| forge/1.20.1 | `gradle.properties` 的 `mod_version` |
| fabric/1.20.1 | 同上 |
| fabric/26.3 | 同上 |
| forge/1.12.2 | **两处**：`build.gradle` 的 `version = '...'`，以及 `CTDI.java` 的 `VERSION = "..."` |

- 仓库：`zzy89216-gif/Minecraft-Cancel-the-damage-interval`（MIT）
- Token 只放环境变量，**任何文件/提交/日志里都不要出现 token**
  （自检：在仓库里搜 `_pat_` 与 access-token 的前缀；注意别把前缀字面量写进文档，
  否则以后每次自检都会误报一次）
- Release 命名：`v<版本号>`；资产名 `ctdi-<loader>-<mcversion>-<modversion>.jar`
- 推送用一次性 header，不要写进 `.git/config`：
  ```bash
  git -c http.extraheader="Authorization: Basic $(printf 'x-access-token:%s' "$GITHUB_TOKEN" | base64 -w0)" push origin main
  ```

### Token 权限（踩过）

只读 Token 的表现：`HTTP 403 Resource not accessible by personal access token`，
响应头 `x-accepted-github-permissions: metadata=read`，push 报 `403 denied`。
注意 `GET /repos/...` 返回的 `permissions` 是**仓库所有者本人**的权限，**不代表 Token 权限**。

要 push + 发 Release，Fine-grained PAT 需要：`Contents: Read and write`（+ 若要提交
`.github/workflows/*` 还需 `Workflows: Read and write`）。经典 Token 勾 `repo` 即可。

---

## 10. 原则（来自项目发起人）

1. 改代码前先看源码/字节码/构建配置，**不要把 README 的规划当成已实现**。
2. 找真正产生限制的代码，不要只改客户端表现。
3. 多人环境服务端权威。
4. 少改原版，不重做战斗系统，不为简单功能上复杂架构。
5. 不确定就明说；**没构建/测试过就不要说"已验证"**。
6. 回答直接，给可复制的命令与完整文件内容。
