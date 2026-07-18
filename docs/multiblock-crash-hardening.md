# 多方块结构与跑图崩溃加固记录

记录日期：2026-07-16

本文记录本次对话中对 Railcraft 1.12.2 全部多方块结构的审计，以及针对专用服务端和 CleanroomMC 客户端跑图崩溃风险完成的修复。

## 对话目标与约束

本次工作分为两个阶段：

1. 用户首先要求分析项目中的所有多方块结构，以及可能导致服务端或 CleanroomMC 客户端崩溃的危险。
2. 用户随后说明这些方块只用于装饰，无需确保多方块功能正常，并要求解决所有已发现漏洞，尤其关注蓄水器/储罐和跑图加载场景；必要时允许删除多方块功能。

最终没有直接删除多方块功能。修复策略是保留已有方块、存档格式和基本结构行为，同时把损坏、越界或两端不一致的结构状态安全降级为普通未形成结构。功能兼容性低于稳定性优先级。

## 多方块结构清单

本项目共识别出 9 类多方块结构：

1. 金属储罐：铁制和钢制共用结构逻辑，底面宽度为 3、5、7、9，高度为 4 至 8。
2. 蓄水器：3×3、4×4、5×5 三组底面与对应高度组合。
3. 锅炉：低压/高压锅炉罐体，以及固体/液体燃料火箱。
4. 焦炉：包括 3×3×3、横向长炉体、4×4×4 和 5×5×5 等模式。
5. 高炉：3×3 底面、4 格有效高度。
6. 岩石粉碎机：2×2×3 主体，两种水平朝向。
7. 蒸汽烤炉：2×2×2 主体。
8. 蒸汽轮机：2×2×3 主体，两种水平朝向。
9. 通量变压器：2×2×2 主体。

结构注册入口位于：

```text
src/main/java/mods/railcraft/common/blocks/RailcraftBlocks.java
```

核心结构实现位于：

```text
src/main/java/mods/railcraft/common/blocks/logic/StructureLogic.java
src/main/java/mods/railcraft/common/blocks/structures/StructurePattern.java
```

## 原始高危问题

### 1. 金属储罐阀门和 TESR 直接数组越界

`StructureLogic.getMarker(EnumFacing)` 原本直接使用相邻坐标读取三维模式数组，没有检查相邻位置是否仍在模式范围内。

默认配置允许储罐堆叠，此时金属储罐模式没有上下边框。位于最底层的阀门读取 `DOWN` 时会得到 `y = -1`，从而触发 `ArrayIndexOutOfBoundsException`。

该路径会被以下代码触发：

- `TileTankValve.getActualState()` 的六方向模型状态检测。
- `TESRHollowTank` 的阀门填充动画。

因此玩家只要跑图加载并渲染对应区块，就可能发生客户端崩溃。

### 2. 客户端与服务端储罐配置不一致

金属储罐模式原本在静态初始化时根据本地配置和 `getEffectiveSide()` 生成。

如果服务端和客户端的 `allow.stacking` 或最大储罐尺寸配置不同，服务端同步的模式索引、模式高度和结构内坐标可能无法在客户端本地模式数组中表示。客户端在读取同步包、计算主方块或渲染模型时可能数组越界，并在每次加载该区块时重复崩溃。

### 3. 损坏或旧版结构 NBT 导致区块加载崩溃

原结构 NBT 存在多个未经验证的读取点：

- 空 `marker` 字符串直接调用 `charAt(0)`。
- 长度不足 3 的坐标整型数组直接读取 `c[0]`、`c[1]`、`c[2]`。
- NBT 中的模式索引和结构内坐标未经完整范围验证。
- 非法枚举序号、枚举名称和所有者 UUID 会抛出运行时异常。

这些问题可能使旧存档、第三方写入或经过 NBT 编辑的区块在服务端和客户端加载时崩溃。

### 4. 递归传播可能耗尽线程栈

结构变化原本通过递归 DFS 在相邻多方块组件之间传播。

大型金属储罐模式可接近或超过一千个模式位置。由储罐方块组成的细长或异常连通区域，加上较小的 JVM 线程栈，可能导致服务端 `StackOverflowError`。

### 5. 结构检测的池化坐标未释放

`StructurePattern.testPattern()` 使用 `PooledMutableBlockPos`，但遇到区块未加载或模式不匹配时会提前返回，绕过 `release()`。

该问题不会永久泄漏普通 Java 对象，但会使对象池失效，在区块边界、大量装饰方块和多模式储罐反复检测时增加内存分配与 GC 压力。

### 6. 通用方块实体直接引用客户端类

`TileRockCrusher` 和 `TileSteamOven` 位于通用代码包中，但其构造的粒子回调直接引用 `ClientEffects`。

常规 Java 虚拟机通常会延迟解析该引用，但更严格的类验证、字节码转换器或 CleanroomMC 使用的现代 JVM 环境可能在专用服务端加载类时触发 `NoClassDefFoundError`。

### 7. 网络数据缺少范围验证

`RailcraftInputStream.readEnum()` 原本直接使用有符号字节作为数组下标。版本错配、截断数据或非法数据可产生负下标或超范围下标，导致客户端运行时异常。

### 8. 蒸汽轮机与通量变压器结构键重复

两者原本都使用结构键 `flux`。`StructureLogic.canMatch()` 只比较结构键，因此错误主方块坐标、损坏 NBT 或未来模式改动可能使两种结构逻辑互相匹配。

### 9. 公开结构放置接口缺少防御

公开放置接口存在以下问题：

- 金属储罐与岩石粉碎机直接使用未经验证的模式索引。
- 金属储罐没有映射主方块标记 `M`。
- 高炉和焦炉没有映射角块标记 `C`。
- 岩石粉碎机第二种朝向缺少 `g` 标记映射。

第三方模组或世界生成器传入非法索引时，可能直接使服务端崩溃。

## 已完成的修复

### 安全的模式坐标读取

`StructurePattern` 新增统一的范围判断。所有超出三维模式范围的坐标都返回 `EMPTY_MARKER`，不再直接访问数组。

`StructureLogic.setPatternState()` 同样验证模式和结构内坐标。无效数据会清除当前模式、主方块坐标和标记，并把结构置为无效状态。

这项修复直接覆盖金属储罐阀门、锅炉连接模型、蒸汽轮机模型和储罐 TESR 等所有相邻标记读取路径。

### 稳定的储罐模式列表

金属储罐现在始终建立 3、5、7、9 四组尺寸的模式列表，不再使用线程组相关的 `getEffectiveSide()` 决定客户端模式数量。

服务端仍可根据结构配置生成对应形状，但同步索引不会再因最大尺寸配置不同而整体错位。即使堆叠配置导致形状不同，结构坐标验证也会把无法表示的数据安全降级。

同时修复了非堆叠模式中 `AxisAlignedBB.offset()` 返回值被忽略的问题。

### NBT 与网络防御

完成以下加固：

- 空结构标记不再调用 `charAt(0)`。
- 截断坐标数组返回 `null`。
- 未知 NBT 类型不再被当作方块坐标。
- 模式索引和结构内坐标均验证范围。
- 非法枚举序号和名称使用默认值。
- 非法所有者 UUID 被忽略。
- 网络枚举使用无符号字节读取，并把非法值转换为可由现有包处理逻辑捕获的 `IOException`。
- 位集合长度改用无符号字节，避免负数组长度。

### 有界迭代结构传播

结构变化传播由递归 DFS 改为使用 `Deque` 的迭代遍历，并继续受最大模式体积限制。异常连通结构不再消耗 Java 调用栈。

### 确保池化坐标释放

结构模式检测现在使用 `try/finally`，无论正常完成、区块未加载还是模式不匹配，都会释放 `PooledMutableBlockPos`。

### 服务端/客户端代码隔离

岩石粉碎机和蒸汽烤炉通过 `CommonProxy` 请求粒子效果：

- 专用服务端代理为空实现。
- 客户端代理再调用 `ClientEffects`。

编译后的两个通用方块实体类已检查，不再包含对 `ClientEffects` 的常量池引用。

### 结构键与放置接口

- 蒸汽轮机结构键改为 `steam_turbine`。
- 非法金属储罐和岩石粉碎机模式索引直接忽略，不修改世界。
- 补齐金属储罐 `M`、焦炉/高炉 `C` 和岩石粉碎机 `g` 的放置映射。

## 新增测试

新增以下回归测试：

```text
src/test/java/mods/railcraft/common/blocks/structures/StructurePatternTest.java
src/test/java/mods/railcraft/common/plugins/forge/NBTPluginTest.java
src/test/java/mods/railcraft/common/util/network/RailcraftInputStreamTest.java
```

测试覆盖：

- 六方向和任意越界模式坐标返回空标记。
- 模式范围判断。
- 截断与完整方块坐标 NBT。
- 非法枚举序号和名称回退。
- 有效网络枚举读取。
- 非法网络枚举转换为 `IOException`。

## 验证结果

使用 JDK 8 执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew compileJava

JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew test

JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew build
```

结果：

```text
BUILD SUCCESSFUL
```

原有与新增测试共 30 项，全部通过。完整构建、重混淆和 `git diff --check` 均成功。

生成的发布 JAR：

```text
build/distributions/railcraft-12.1.1.jar
```

工作区原有的 `lang` 子模块状态没有被修改。

## CleanroomMC 验证范围

本次完成了静态源码审计、JDK 8 编译测试、完整 ForgeGradle 构建，以及通用结构类的编译后字节码引用检查。

尚未在真实 CleanroomMC 客户端与专用服务端组合中进行长时间跑图集成测试。因此不能证明不存在来自其他模组、核心修改或显卡驱动的独立问题，但本次确认的结构坐标越界、损坏 NBT、网络枚举、递归栈溢出、池化对象和客户端类加载路径均已处理。

## GUI 高危标识

根据后续要求，所有仍可作为物品获得的多方块组件会在容器、背包和创造模式物品槽中显示警告角标。角标使用用户提供的 `警告.png`，绘制在 16×16 物品槽的右下 8×8 区域，因此只覆盖 GUI 物品图标，不修改世界中的方块贴图或模型。

组件的本地化名称末尾统一追加红色加粗的 `§c§l(高危!)`。后缀由物品显示逻辑追加，避免构建时只读 `lang` 子模块覆盖主资源目录中的同名语言文件。旧的简体中文“危险！可能导致崩服”静态后缀已移除，避免重复显示。

铁制和钢制储罐量计是旧存档迁移用的无物品替换方块，注册时没有对应 `ItemBlock`；它们的名称仍在高危清单中，但正常 GUI 不会出现可叠加角标的物品。

GUI 标识改动完成后再次使用 JDK 8 执行完整 `test build`，共 37 项测试通过，重混淆发布 JAR 构建成功；JAR 内的警告 PNG 与用户提供的源图片 SHA-256 完全一致。

## 本次涉及的主要项目文件

- `src/main/java/mods/railcraft/common/blocks/logic/StructureLogic.java`
- `src/main/java/mods/railcraft/common/blocks/structures/StructurePattern.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileTank.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileRockCrusher.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileSteamOven.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileSteamTurbine.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileCokeOven.java`
- `src/main/java/mods/railcraft/common/blocks/structures/TileBlastFurnace.java`
- `src/main/java/mods/railcraft/common/blocks/ItemBlockRailcraft.java`
- `src/main/java/mods/railcraft/common/blocks/RailcraftBlocks.java`
- `src/main/java/mods/railcraft/common/core/CommonProxy.java`
- `src/main/java/mods/railcraft/client/core/ClientProxy.java`
- `src/main/java/mods/railcraft/client/gui/HighRiskItemOverlay.java`
- `src/main/java/mods/railcraft/common/plugins/forge/NBTPlugin.java`
- `src/main/java/mods/railcraft/common/plugins/forge/PlayerPlugin.java`
- `src/main/java/mods/railcraft/common/util/network/RailcraftInputStream.java`
- `src/main/resources/assets/railcraft/lang/zh_cn.lang`
- `src/main/resources/assets/railcraft/textures/gui/multiblock_high_risk.png`
- `docs/multiblock-crash-hardening.md`
