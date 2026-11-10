# IntelliJ IDEA 与 Apple Silicon 开发启动

更新日期：2026-10-29

## 当前方案

项目使用 Java 8、Gradle Wrapper 4.9、ForgeGradle 2.3.4、Minecraft 1.12.2 和 Forge 14.23.5.2847。

IDEA 中的 `runClient`、`runServer` 是标准 Gradle 运行配置，直接执行 ForgeGradle 自带的同名任务。类路径、编译、打包和资源加载由 ForgeGradle 管理；客户端从标准 `build/distributions/railcraft-<version>.jar` 加载 Railcraft。

ForgeGradle 2.3 原有的 Application 配置生成器使用旧版 IDEA 的模块名（如 `railcraft_main`），不能直接适配当前 IDEA 的模块模型。因此 `genIntellijRuns` 仅保留一段配置生成适配：写出调用官方任务的标准 Gradle 配置。无需指定主模块、修改运行类路径或增加 Java 启动桥接类。

## 使用方式

在项目根目录使用 Java 8 生成运行配置：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew genIntellijRuns
```

完成后在 IDEA 中选择 `runClient`，直接运行即可。此配置通过项目的 Gradle JVM 执行，当前机器已设为注册的 Java 8 SDK（`1.8`）。再次执行 `genIntellijRuns` 会生成相同的标准配置，无需手动修复。

终端也可以直接运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew runClient
```

`genIntellijRuns` 和 `runClient` 均会自动准备所需的 ARM 原生库与 Narrator 补丁。

## 保留的 ARM 兼容支持

Minecraft 1.12.2 原始的 macOS 原生库针对 Intel JVM，当前机器使用 AArch64 Java 8，因此保留以下独立的兼容处理：

- `gradle/macos-arm64-natives/*.jar`：提供 ARM 版本的 LWJGL、OpenAL 和 JInput 原生库，复制至 `build/natives`。
- macOS ARM 下使用 LWJGL 2.9.4 nightly 与 JNA 5.13.0。
- `src/appleSiliconPatch/java/com/mojang/text2speech/Narrator.java`：生成 `build/patches/apple-silicon-narrator.jar`，通过 Java 8 的 `-Xbootclasspath/p` 加载。此开发环境补丁禁用依赖 Intel-only `libjcocoa` 的游戏旁白，不进入 Railcraft 发布包。
- macOS ARM 下跳过 ForgeGradle 2.3 的旧 `getAssets` 下载任务。当前机器已有完整的 Minecraft 1.12.2 资源缓存；全新环境仍需要准备这些资源。

ARM 补丁不会改变 Railcraft 的源码集、Mod 类路径或资源布局。

## 移除的旧方案

2026-10-29 清理了以下内容：

- `prepareIdeaRunOutput`、`prepareRunClient`、`prepareRunServer`、`writeIdeaRunConfigurations`。
- `src/main/java/mods/railcraft/dev/RunClient.java` 和 `RunServer.java`。
- 手工组合 classpath 的 Application 运行配置及旧 `Minecraft Client`、`Minecraft Server` 配置。
- `build/ideaRuns` 和桥接类的旧生成源码、编译产物；通过 `clean` 清理后重新构建。
- 为已删除桥接类设置的 JAR 排除规则。

当时的重复加载日志明确列出 `build/ideaRuns/main` 与 `build/classes/java/main` 两个来源。现在仅使用 ForgeGradle 官方任务选定的 Mod JAR，不再复制额外的开发运行输出。

## 验证

使用 Java 8 执行：

```bash
./gradlew clean test build genIntellijRuns --offline --no-daemon
```

构建通过，49 项测试全部通过。随后在 ARM Java 8 上运行官方 `runClient`，日志确认：

- `Java ... 1.8.0_492 ... aarch64`。
- `LWJGL Version: 2.9.4`。
- Railcraft 12.1.2 从 `build/distributions/railcraft-12.1.2.jar` 识别一次。
- `Forge Mod Loader has successfully loaded 5 mods`。
- 启动类路径不含 `build/ideaRuns`，没有 `DuplicateModsFoundException`。

实际从 IDEA 选择新生成的 `runClient` 并直接运行，也在 ARM Java 8 上完成 Forge 初始化，Railcraft 仅识别一次。再次执行 `genIntellijRuns` 后，两个运行配置的 SHA-256 均与上次生成结果一致。旧合并目录、桥接源码及编译产物均已不存在，两个子模块保持干净。验证后已停止测试客户端。

运行过程中仍有历史记录中的 NebulaeCraft 缺失纹理及旧在线版本检查服务警告，与启动配置清理无关。

## 初次导入

`api-railcraft` 和 `lang` 是独立 Git 子模块。如果未初始化，构建会因缺少 `api-railcraft/gradle.properties` 失败。初始化需要遵守项目对子模块目录的约束，并仅检出主仓库锁定的版本。

若报缺少 `forgeBin`，应使用 Java 8 执行 `./gradlew setupDecompWorkspace -x getAssets` 初始化 ForgeGradle 开发依赖。

## 参考

- [Forge 1.12.x 官方开发文档](https://docs.minecraftforge.net/en/1.12.x/gettingstarted/)
- [ForgeGradle 2.3 官方启动任务实现](https://github.com/MinecraftForge/ForgeGradle/blob/FG_2.3/src/main/java/net/minecraftforge/gradle/user/UserBasePlugin.java)
