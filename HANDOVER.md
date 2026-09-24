# CTDI 交接日志

> 写给下一个接手本项目的开发者 / AI Agent。
> 最后更新：2026-09-24，v0.1.0（首个可构建、可运行、已发布的版本）

---

## 1. 当前状态（一句话）

**Minecraft 1.20.1 + Forge 47.2.0 的 CTDI 已经写完、构建通过、实机验证并发布**：
移除无敌帧 + 移除攻击冷却，两个 Mixin 共约 40 行核心代码，
`./gradlew build` 产出可直接投放的 `build/libs/ctdi-0.1.0.jar`。

- 仓库：https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval （public，MIT）
- 发布：https://github.com/zzy89216-gif/Minecraft-Cancel-the-damage-interval/releases/tag/v0.1.0
  （资产 `ctdi-0.1.0.jar`，8223 字节，
  sha256 `fda6eb8d64c7c60c6c187a80eaa79a32f24d0c0e927d862500b6466af9976152`）
- 已实测验证的范围（含对照实验）见 §5，**未验证的部分写得同样明确**。

---

## 2. 仓库里有什么

```
├── build.gradle            # ForgeGradle 6 + Sponge mixin 插件 + reobf
├── settings.gradle         # 插件仓库（含 spongepowered maven）
├── gradle.properties       # 版本矩阵：MC 1.20.1 / Forge 47.2.0 / mod ctdi 0.1.0
├── src/main/java/com/zzy89216/ctdi/
│   ├── CTDIMod.java        # @Mod 入口（无注册内容，纯加载标记）
│   └── mixin/
│       ├── InvulnerabilityMixin.java   # 移除无敌帧
│       └── AttackCooldownMixin.java    # 移除攻击冷却
├── src/main/resources/
│   ├── META-INF/mods.toml
│   ├── ctdi.mixins.json    # mixin 配置（refmap: ctdi.refmap.json）
│   ├── pack.mcmeta
│   └── assets/ctdi/icon.png
├── CHANGELOG.md            # 版本历史
└── HANDOVER.md             # 本文件
```

## 3. 核心机制的实现位置（改代码前先读这里）

### 3.1 无敌帧 —— `InvulnerabilityMixin`

**注入点**：`LivingEntity#hurt(DamageSource, float)` 方法 **HEAD**。
**做法**：进入方法时把 `invulnerableTime = 0`、`hurtTime = 0`。

**为什么这样是对的（1.20.1 字节码已验证）**：原版 `hurt` 内部有
`if (invulnerableTime > 10f && !source.is(BYPASSES_INVULNERABILITY))` 分支，
该分支内要么丢弃伤害（`amount <= lastHurt`）要么只结算差额。
清零后所有伤害走完整结算分支（`damageEntity(source, amount)`）。

**验证过的事实**：
- `invulnerableTime` 字段声明在 `Entity`（protected int），`hurtTime` 声明在 `LivingEntity`（public int）。Mixin 类混入 `LivingEntity`，直接写字段合法，**不需要 AT（access transformer）**。
- 同一服务端 tick 内处理多次攻击（高 CPS / 多个玩家同时打一个怪）也全部生效，因为注入点早于原判断，而不是靠每 tick 清零。
- 客户端实体同样跑 `hurt`，所以客户端表现与服务端一致；无网络同步改动。

**注意**：换 Minecraft 版本时必须重新确认 `hurt` 里 i-frames 判断的写法
（1.20.5+ 重写过伤害流程，字段/分支位置可能变化，不能直接照抄本实现）。

### 3.2 攻击冷却 —— `AttackCooldownMixin`

**注入点**：`Player#getAttackStrengthScale(float)` HEAD，`cancellable=true`，
直接 `setReturnValue(1.0f)`。

**为什么这样是对的（1.20.1 字节码已验证）**：原版 `Player#attack` 中
`s = getAttackStrengthScale(0.5f)` 同时控制：
- 基础伤害乘数 `0.2 + s*s*0.8`
- 附魔伤害乘数 `附魔 * s`
- 暴击/击退/冲刺加成门槛 `s > 0.9`

恒返回 1.0 等价于"攻击永远满冷却"。攻击速度属性（ATTACK_SPEED）
只影响 `attackStrengthTicker` 回满速度，不再影响伤害。

**客户端一致性**：准星处的冷却圆圈（攻击指示器）读的是同一个方法，
所以 HUD 显示与实际伤害行为一致，不会"看着满、打半伤"。

### 3.3 构建链（Mixin refmap / reobf，踩过的坑都在这里）

- `build.gradle` 里 `id 'org.spongepowered.mixin'` 插件（来自 spongepowered maven，
  仓库已写进 `settings.gradle` 的 pluginManagement）。
- `mixin { add sourceSets.main, "ctdi.refmap.json"; config "ctdi.mixins.json" }`
  让 annotation processor 在编译期生成 refmap。
- `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'` 必须有，否则 refmap 不生成。
- jar manifest 里写了 `MixinConfigs: ctdi.mixins.json`（Forge 靠它发现 mixin 配置）。
- `jar` 任务 `finalizedBy 'reobfJar'`（MDK 自带）把 jar 里对 MC 的引用从
  Mojang 名重映射为 SRG 名。**refmap 已验证**：`hurt -> m_6469_`、
  `getAttackStrengthScale -> m_36403_`，与 1.20.1 SRG 一致，产物 jar 可直接投放。
- 开发环境（eclipse/idea run config）用 Mojang 名，不需要 refmap，两条路径都通。

## 4. 开发环境（Android 手机 / Ubuntu 24.04 容器）

实测环境：

| 项目 | 值 |
|:---|:---|
| 系统 | Ubuntu 24.04（aarch64，ARM64 容器） |
| JDK | OpenJDK 17.0.20（`apt install openjdk-17-jdk-headless`） |
| Gradle | 项目自带 wrapper 8.1.1（首次构建自动下载发行版） |
| 内存 | 11 GB（构建峰值约 3-4 GB，`org.gradle.jvmargs=-Xmx3G`） |
| 构建耗时 | 首次约 11 分钟（主要是下载）；增量构建秒级到分钟级 |

关键命令（在仓库根目录执行）：

```bash
./gradlew build          # 完整构建（产物 build/libs/ctdi-0.1.0.jar）
./gradlew clean build    # 干净重建
./gradlew genIntellijRuns # 可选：生成 IDE 运行配置
```

## 5. 已做过的验证

| 验证项 | 方式 | 结果 |
|:---|:---|:---|
| 构建 | `./gradlew build` 真实执行 | ✅ BUILD SUCCESSFUL |
| refmap/reobf | 解包产物 jar 检查 `ctdi.refmap.json` | ✅ `hurt -> m_6469_`、`getAttackStrengthScale -> m_36403_` |
| 原版源码核对 | 反编译 1.20.1 官方 client.jar（CFR）+ javap 字节码，确认注入点 | ✅ 两个注入点均命中 |
| 服务端加载 | 真实 Forge 1.20.1 专用服务端 + 产物 jar 启动 | ✅ `[CTDI] loaded`，无崩溃 |
| Mixin 应用 | 服务端加 `-Dmixin.debug.verbose=true` | ✅ `Mixing InvulnerabilityMixin ... into LivingEntity`、`Mixing AttackCooldownMixin ... into Player` |
| 无敌帧移除（**功能实测**） | RCON `/damage` 对同一僵尸连打两次 5 点（约 1 tick 间隔，落在原版 10 tick 窗口内） | ✅ 有 CTDI：20.0f → **10.0f**（两次全生效）<br>对照（无 CTDI）：20.0f → **15.0f**（第二次被原版丢弃） |
| Mixin 兼容性告警 | 检查服务端日志 | ✅ 0 条（`compatibilityLevel` 用 JAVA_13，与 Mixin 0.8.5 上限一致） |
| 攻击冷却（玩家侧） | 玩家真实挥砍 | ⚠️ **未做**：无图形客户端可连。代码/refmap/注入点已验证，但"手速=DPS"体感未实测 |
| 客户端表现 | 受击红屏、准星冷却圈 | ⚠️ 未做（需要图形客户端） |

测试脚本已入库：`tools/damage_test.py`（纯标准库 RCON 客户端，改完代码后可复跑回归）。

**没有验证过的东西不要当作已验证**（尤其是玩家挥砍体感与客户端表现，见 §7）。


## 6. 已知设计取舍与限制

1. **对所有 LivingEntity 生效**（玩家、怪、 Boss、铁傀儡……都会失去无敌帧）。
   规划中的"实体白名单"就是为这个服务的。
2. **没有配置项**。两项机制都是无开关硬移除。加配置 = `@Config`/Forge Config
   或 GSON json，改动量不大（规划功能 5）。
3. **Java 21 "适配"的准确含义**：字节码是 Java 17 级别，Java 17/21 都能跑这个
   jar；但**构建**本仓库用的 wrapper 是 Gradle 8.1.1（只支持 Java ≤19 的
   toolchain 自动下载没问题，用 JDK 21 直接跑 Gradle 需要 Gradle ≥8.5）。
   如果以后要在 JDK 21 上构建，把 `gradle/wrapper/gradle-wrapper.properties`
   升到 8.8+ 即可（需回归测试 FG6 兼容性）。
4. **没动摔落/着火/虚空伤害**：这些走 `BYPASSES_INVULNERABILITY` 或独立逻辑，
   与 CTDI 的目标无关，保持原版。
5. **`hurtTime` 清零只影响受击动画/红屏计时**，不影响伤害数值。

## 5.1 如何复现"实机验证"（含踩过的坑）

本机实测用的是一台真实 Forge 1.20.1 专用服务端（ARM64 容器上跑得动，约 2 分钟启动）：

```bash
# 1) 装服务端（官方 installer 一条命令）
mkdir -p ~/ctdi-server/mods && cd ~/ctdi-server
curl -LO https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.2.0/forge-1.20.1-47.2.0-installer.jar
java -jar forge-1.20.1-47.2.0-installer.jar --installServer
echo "eula=true" > eula.txt

# 2) 放入模组
cp <仓库>/build/libs/ctdi-0.1.0.jar mods/

# 3) 开启 RCON（给 tools/damage_test.py 用）
cat >> server.properties <<'EOF'
enable-rcon=true
rcon.port=25575
rcon.password=ctditest
broadcast-rcon-to-ops=false
EOF

# 4) 启动（注意用 installer 生成的 run.sh，不要手写 java -jar）
./run.sh --nogui

# 5) 等日志出现 "Done ("，然后跑测试
python3 <仓库>/tools/damage_test.py WITH-CTDI
```

踩过的坑（别再重复）：

- **`java -jar forge-universal.jar` 不行**：universal jar 是给客户端/类路径用的，
  专用服务端要用 installer 的 `--installServer` 生成启动配置。
- **installer jar 安装后不要删**：它同时充当启动器的一部分（`run.sh` 会引用
  `libraries/.../unix_args.txt`）。
- **`--nogui` 要传给 `run.sh`，不是传给 installer**：installer 的
  `SimpleInstaller` 只认安装参数，传 `--nogui` 会报 `UnrecognizedOptionException`。
- **两个进程不能同时写同一个日志文件**：会看到混在一起的栈，误判成崩溃。
- **Mixin 调试**：`user_jvm_args.txt` 里加 `-Dmixin.debug.verbose=true`
  （不是 `-Dmixin.debug=true`），日志才会打印 "Mixing X into Y"。

## 7. 接手后建议做的事（按优先级）

1. **游戏内实测**：本地 Forge 客户端进单人生存，打猪/牛验证
   a) 连击全程满伤害（无半伤） b) 暴击正常触发 c) 其他原版机制无异常
   （比如盾、盔甲、药水、附魔伤害数值对照原版）。
2. **多人实测**：Forge 服务端 + CTDI，客户端连击，确认服务端放行高速攻击。
3. **加配置**（规划 5）：两项机制各自可开关，默认开启。
4. **实体白名单**（规划 6）：配置里列实体类型，命中者保留原版无敌帧。
5. **多版本/多 Loader**：优先 1.21.x Forge/NeoForge（注意 1.20.5+ 伤害流程
   重写过，§3.1 的实现方式需要按新版本源码重新推导，**不要照抄**）。
6. **GitHub Actions CI**：push 触发 `./gradlew build`，把"构建通过"变成 PR 门禁。

## 8. GitHub 操作备忘

- 仓库：`zzy89216-gif/Minecraft-Cancel-the-damage-interval`（MIT）
- Token 只放环境变量，**任何文件/提交/日志里都不要出现 token**
- Release 命名：`v<版本号>`，说明用 CHANGELOG 对应段落
- 构建产物文件名规律：`ctdi-<版本号>.jar`（版本号改 `gradle.properties` 的
  `mod_version`）

### Token 权限（2026-09-24 踩过）

只读 Token 的表现：

```
HTTP 403 Resource not accessible by personal access token
x-accepted-github-permissions: metadata=read
git push → 403 denied to <user>
```

`GET /repos/...` 返回的 `permissions: {admin: true, push: true, ...}` 是**仓库所有者
本人**的权限，**不代表 Token 的权限**，不能用它判断能否 push。

要完成"push 代码 + 发 Release"，Fine-grained PAT 需要：

- Repository access：选中 `Minecraft-Cancel-the-damage-interval`
- **Contents: Read and write**（push 提交、创建 tag、发 Release 都靠它）
- Metadata: Read（自动带）
- 如果以后要提交 `.github/workflows/*`：还需要 **Workflows: Read and write**

改权限位置：GitHub → 右上角头像 → Settings → Developer settings →
Personal access tokens → Fine-grained tokens → 选中该 Token → Edit → 勾权限 → Save。

**推送凭据不要写进仓库**，用一次性 header：

```bash
git -c http.extraheader="Authorization: Basic $(printf 'x-access-token:%s' "$GITHUB_TOKEN" | base64 -w0)" push origin main
```

或用 `git remote set-url` 临时带 Token 后再改回来（不要 commit `.git/config`）。


## 9. 原则（来自项目发起人，务必遵守）

1. 修改前先看源码/构建配置，**不要把 README 的规划当成已实现**。
2. 找真正产生限制的代码，不要只做客户端表现。
3. 多人环境服务端权威。
4. 少改原版，不重做战斗系统，不为简单功能上复杂架构。
5. 不确定就明说；没构建/测试过就不要说"已验证"。
6. 回答直接、给可复制的命令和完整文件内容。
