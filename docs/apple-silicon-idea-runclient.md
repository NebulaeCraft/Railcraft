# IntelliJ IDEA 与 Apple Silicon 开发环境修复记录

记录日期：2026-07-16

本文记录 Railcraft 1.12.2 项目在 IntelliJ IDEA 中从无法导入到能够在 Apple Silicon Mac 上启动 `runClient` 的完整排查和修复过程。

## 环境

- macOS，Apple Silicon（验证机器为 Apple M4）
- Azul Zulu OpenJDK 8，`1.8.0_492`，AArch64
- Gradle Wrapper 4.9
- ForgeGradle 2.3.4
- Minecraft 1.12.2
- Forge 14.23.5.2847

此项目使用较老的 Minecraft/ForgeGradle 工具链。构建和运行必须使用 Java 8，不能直接切换到 IDEA 默认的新版本 JDK。

## 1. 缺少 `api-railcraft/gradle.properties`

首次由 IDEA 导入项目时出现：

```text
Could not read script '.../Railcraft/api-railcraft/gradle.properties' as it does not exist.
```

`api-railcraft` 和 `lang` 是 Git 子模块。仅克隆主仓库时，子模块目录可能存在但没有实际内容，因而 `build.gradle` 第 28 行无法加载 API 版本配置。

处理方法：

```bash
git submodule update --init --recursive
```

完成后应能看到：

```text
api-railcraft/gradle.properties
lang/src/main/resources/...
```

## 2. 无法解析 `forgeBin`

随后 Gradle 报告：

```text
Could not find net.minecraftforge:forgeBin:
1.12.2-14.23.5.2847-PROJECT(railcraft)
```

这个依赖不是应该从 Maven 仓库直接下载的普通发布物。ForgeGradle 2.3 会在开发环境初始化期间生成本地 `forgeBin`。因此，添加新的 Maven 仓库并不能解决问题。

在确认子模块版本配置已经存在后，使用 Java 8 初始化开发工作区：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew setupDecompWorkspace -x getAssets --no-daemon
```

`-x getAssets` 是必要的，因为 ForgeGradle 2.3 使用的旧资源下载端点已经失效，会返回 HTTP 400，并逐个重试大量资源。

## 3. IDEA 中没有 `runClient` 和 `runServer`

项目原有的 `genIntellijRuns` 针对旧版 IDEA 编写，在当前 IDEA 项目模型下会出现空指针异常，生成的 Application 配置还可能引用错误的模块名或不完整的模块输出。

本次最终为 IDEA 创建了普通 Application 运行配置，因此 `runClient` 与 `runServer` 使用相同类型的图标：

- 主类为 IDEA 可以直接索引的 `mods.railcraft.dev.RunClient` 或 `RunServer`；它们只负责通过反射调用 ForgeGradle 2.3 生成的 `GradleStart`/`GradleStartServer`。
- classpath 仍使用 `railcraft.main` 模块提供依赖，但排除 IDEA 自己的主模块输出，改用 Gradle 处理后合并生成的 `build/ideaRuns/main`。该目录同时包含 class 与 resource，因为 Forge 1.12 会把发现 `@Mod` 类的目录本身当作 Mod 资源包；若分别加入 `build/classes/java/main` 和 `build/resources/main`，Railcraft 虽能加载，纹理、模型、语言等资源却不会归属于它。
- 启动前执行 Gradle 的 `prepareRunClient` 或 `prepareRunServer`，先编译源码、展开版本资源、合并 IDEA 开发运行输出，并为客户端生成 Apple Silicon Narrator 补丁和复制 ARM 原生库。
- JVM 参数显式包含 `-Djava.library.path=.../build/natives` 和 `-Xbootclasspath/p:.../apple-silicon-narrator.jar`。
- Gradle JVM 固定为 Java 8。
- IDEA 的“构建和运行使用”设为 IntelliJ IDEA，而不是把整个 Application 启动委托给 Gradle。

此前 Application 配置失败并不是因为 Apple Silicon 修复必须从 Gradle 配置启动，而是启动方式和 classpath 选择错误。Gradle `runClient` 任务本身的 classpath 末尾包含 `build/distributions/railcraft-12.1.1.jar`；若再叠加 IDEA 主模块输出，就会同时发现发布 JAR 和源码输出，触发重复 Mod。最终配置不使用发布 JAR，并明确排除 IDEA 的 `build/classes/production/railcraft.main`，因此 classpath 中只有一套 Gradle 主输出。

直接使用 `GradleStart` 作为主类时，IDEA 不会索引 ForgeGradle 缓存中的动态启动类，因而给配置图标加红叉。两个开发桥接类解决静态校验问题，同时从所有发布 JAR 中排除，不会进入 Mod 成品。IDEA 的 `Make` 还会把源码中的 `@VERSION@` 和资源中的 `${version}` 原样写入输出；改用 Gradle 合并后的开发运行输出后，两处都会正确展开为 `12.1.1`，Forge 也能从同一目录取得 Railcraft 的全部资源。

相邻 NebulaeCraft 项目采用相同思路，但两者入口不同：NebulaeCraft 使用 ForgeGradle 3 的 `net.minecraftforge.legacydev.MainClient` 和一组环境变量；Railcraft 使用 ForgeGradle 2.3 的 `GradleStart`。因此可以参考其 Application 配置结构，但不能直接复制主类和环境变量。

IDEA 的 `.idea/runConfigurations` 当前属于本地忽略文件，不随 Git 提交。`genIntellijRuns` 完成后会通过独立的 `writeIdeaRunConfigurations` 任务生成最终配置；独立任务由 `finalizedBy` 连接，避免 ForgeGradle 2.3 在项目评估后重建原任务 action 时清掉后处理。生成目录通过 Gradle 的 `file('.idea/runConfigurations')` 按项目根解析，不能使用依赖 daemon 当前目录的 `new File(...)`。若 IDEA 已经在当前会话中加载过旧配置，磁盘文件更新后仍可能保留旧 classpath；保存工作并重启一次 IDEA，再重新选择 `runClient` 即可清除这类会话缓存。

ForgeGradle 2.3 的原始 `genIntellijRuns` 还会向 `.idea/workspace.xml` 写入名为 `Minecraft Client` 和 `Minecraft Server` 的旧配置。它们已由上述 `runClient` 和 `runServer` 完整替代；项目的生成后处理会删除这两个旧配置及其列表项，避免以后重新生成时再次出现。

## 4. Apple Silicon 原生库崩溃

最初的 `runClient` 启动日志显示 Minecraft 1.12.2 自带的 LWJGL 2 原生库是 `x86_64`，而 JVM 是 `aarch64`。这会在加载 `liblwjgl.dylib`、OpenAL 或 JInput 时失败。

修复参考相邻的 NebulaeCraft 项目，为 Railcraft 增加了两个 ARM 原生库包：

```text
gradle/macos-arm64-natives/
├── jinput-platform-natives-osx-arm64-2.0.5.jar
└── lwjgl-platform-natives-osx-arm64-2.9.4-nightly-20150209.jar
```

SHA-256：

```text
571bdd9e576025f2a3d112da96e3152397bc6d95aa1788ea58164892010b4c6f  jinput-platform-natives-osx-arm64-2.0.5.jar
a14379458c749872a1e183e88edceaab87c762328fb031481d98d0aaed5e6311  lwjgl-platform-natives-osx-arm64-2.9.4-nightly-20150209.jar
```

`build.gradle` 在 macOS ARM 环境中执行以下处理：

1. 强制使用 LWJGL 和 `lwjgl_util` 2.9.4 nightly。
2. 使用支持 ARM 的 JNA 5.13.0。
3. 将三个所需原生文件复制到 `build/natives`：
   - `liblwjgl.dylib`
   - `openal.dylib`
   - `libjinput-osx.jnilib`
4. 通过 `-Djava.library.path=.../build/natives` 优先加载这些文件。

实际检查结果为：

```text
liblwjgl.dylib:        arm64
openal.dylib:          arm64
libjinput-osx.jnilib:  x86_64 + arm64
```

## 5. Narrator 的 `libjcocoa` 崩溃

替换 LWJGL 后，客户端继续在 Mojang Narrator 初始化时崩溃。Minecraft 1.12.2 的 macOS Narrator 通过 `java-objc-bridge` 加载 Intel-only 的 `libjcocoa`，不能在 AArch64 JVM 中运行。

项目现在包含一个仅用于 Apple Silicon 开发环境的无操作 Narrator：

```text
src/appleSiliconPatch/java/com/mojang/text2speech/Narrator.java
```

构建流程将它单独编译并生成：

```text
build/patches/apple-silicon-narrator.jar
```

然后通过 Java 8 的以下参数优先加载：

```text
-Xbootclasspath/p:.../build/patches/apple-silicon-narrator.jar
```

补丁 JAR 只包含 `Narrator.class` 和 `Narrator$1.class`，不会把 Railcraft 主模块输出加入启动 classpath，因此不会造成重复 Mod。

此补丁的唯一功能差异是：Apple Silicon 开发环境中的游戏旁白被禁用。正式 Railcraft 构建内容不包含该替代类。

## 6. 避免每次启动执行耗时的 `getAssets`

ForgeGradle 2.3 的 `runClient` 默认依赖 `getAssets`。即使本地已经有 Minecraft 1.12.2 资源，旧任务仍可能访问失效端点并检查或重试 1305 个文件。

现在 `build.gradle` 在 macOS ARM 环境中禁用该任务：

```groovy
if (isMacosArm) {
    getAssets.enabled = false
}
```

本次验证的 Gradle 输出为：

```text
> Task :getAssetIndex UP-TO-DATE
> Task :getAssets SKIPPED
```

该设置假定 `~/.gradle/caches/minecraft/assets` 中已有 Minecraft 1.12.2 的资源缓存。当前开发机器的缓存完整，可以正常启动。若在全新机器上配置，需要从可用的 Minecraft 1.12.2/Forge 开发环境复制资源缓存；旧版 ForgeGradle 的 HTTP 下载任务已不可靠。

## 最终启动方式

在 IDEA 中选择 Application 类型的 `runClient`，或者在终端执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew runClient --no-daemon
```

`runClient` 会自动执行 `prepareRunClient`，生成 Narrator 补丁并复制 ARM 原生库，不需要手工运行准备任务。

IDEA 的 Application 配置同样把 `prepareRunClient` 设为启动前任务，所以两种入口的 Apple Silicon 准备步骤一致；该任务通过 `prepareIdeaRunOutput` 依赖 Gradle 的 `classes`，负责更新带版本替换且合并了资源的主模块输出。

## 验证结果

最终实测结果：

```text
getAssets SKIPPED
Java: OpenJDK 1.8.0_492, aarch64
LWJGL Version: 2.9.4
GL Renderer: Apple M4
Forge Mod Loader has successfully loaded 5 mods
BUILD SUCCESSFUL
```

同时确认：

- 没有再出现 `NarratorOSX` 或 `libjcocoa` 异常。
- 没有 `DuplicateModsFoundException`。
- Railcraft 只加载一次。
- 没有生成新的客户端崩溃报告。
- 客户端完成 Forge 的初始化、后初始化和资源加载阶段，并正常退出。

后续又从 IDEA 实际启动最终 Application 配置：进程 classpath 包含合并的 `build/ideaRuns/main`，不包含 `build/classes/production/railcraft.main`、分离的 Gradle class/resource 目录或 `build/distributions` 中的发布 JAR；日志只识别一次 `mods.railcraft.common.core.Railcraft`，版本显示为 `12.1.1`，Forge 成功加载 7 个 Mod，且未出现 `DuplicateModsFoundException`、ARM native、Narrator 或 Railcraft 资源管理器异常。

一次实际 IDEA 启动暴露了额外问题：开启 delegated build 时，IDEA 会把 Application 启动转换成临时的 `:GradleStart.main()`。该任务的 classpath 仅包含 API 输出，没有主模块输出，最终 Forge 只加载 6 个 Mod，Railcraft 不在列表中。关闭委托后若只追加 Gradle 主输出，IDEA 原生输出又会造成重复发现；若把 Gradle 的 class/resource 两个目录分别追加，Forge 能发现 Mod 却会把仅含 class 的目录用作资源包，日志出现 `railcraft is missing a resource manager`。最终方案同时关闭委托、排除 IDEA 主输出并加入 Gradle 合并后的开发运行输出，兼顾单次加载、正确版本元数据与完整资源加载。

## 已知的非致命警告

启动日志中仍可能出现以下内容，但它们不是本次启动崩溃：

- `http://www.railcraft.info/railcraft_versions` 返回 HTTP 502：旧版 Railcraft 在线版本检查服务不可用。
- `nebulaecraft` 域缺少部分纹理：当前自定义模型引用资源的警告，不会终止客户端。
- IC2 未安装时出现 IC2 配方或货物黑名单条目无匹配项：可选集成被禁用后的警告。

## 本次涉及的项目文件

- `.gitignore`
- `build.gradle`
- `gradle/macos-arm64-natives/*.jar`
- `src/appleSiliconPatch/java/com/mojang/text2speech/Narrator.java`
- `.idea/gradle.xml`（本地忽略）
- `.idea/runConfigurations/runClient.xml`（本地忽略）
- `.idea/runConfigurations/runServer.xml`（本地忽略）
