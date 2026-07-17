# 强化轨道全场景 150% 速度调整记录

记录日期：2026-07-16

本文记录 Railcraft 1.12.2 项目中，将强化轨道速度从原版铁轨的 125% 提升到 150% 的分析、实现与验证过程，并说明该调整与连续弯轨联挂车辆减速修复之间的关系。

## 对话目标

本次工作分为三个阶段：

1. 将 `reinforced_track` 的速度由原版铁轨的 125% 调整为 150%。
2. 分析直道、弯道和坡道采用不同限速时，是否会产生显著的速度差异。
3. 根据分析结果，将强化轨道改为在直道、弯道和坡道上统一提供 150% 的速度上限。

## 原始实现

强化轨道的速度由以下枚举控制：

```text
src/main/java/mods/railcraft/common/blocks/tracks/behaivor/SpeedController.java
```

原始实现为：

```java
REINFORCED {
    public static final float MAX_SPEED = 0.499f;
    public static final float CORNER_SPEED = 0.4f;

    @Override
    public float getMaxSpeed(World world, @Nullable EntityMinecart cart, BlockPos pos) {
        BlockRailBase.EnumRailDirection dir = TrackTools.getTrackDirection(world, pos, cart);
        if (TrackShapeHelper.isTurn(dir) || TrackShapeHelper.isAscending(dir))
            return CORNER_SPEED;
        return MAX_SPEED;
    }
}
```

原版铁轨的速度上限为 `0.4`。因此：

- 强化直轨的 `0.499` 约为原版的 125%。
- 强化弯轨和强化坡道仍被限制为 `0.4`，即原版速度。

## 第一阶段：将直轨提升至 150%

最初将 `MAX_SPEED` 从 `0.499f` 调整为 `0.6f`：

```java
public static final float MAX_SPEED = 0.6f;
```

因为：

```text
0.4 × 150% = 0.6
```

此时保留了弯道和坡道的 `CORNER_SPEED = 0.4f`，随后针对这种限速差异进行了进一步分析。

## 弯道速度差异分析

Forge 1.12.2 的矿车移动逻辑会将轨道速度上限分别应用于 X、Z 运动分量，而不是直接限制水平速度向量的模长。

矿车以 `0.6` 的速度进入一个对角弯轨时，原版轨道逻辑会把速度重新投影到弯道切线。两个水平分量约为：

```text
0.6 / √2 ≈ 0.424
```

如果弯道上限仍为 `0.4`，两个分量会分别被限制到 `0.4`，对应的实际水平位移约为：

```text
√(0.4² + 0.4²) ≈ 0.566
```

这比直道的 `0.6` 低约 5.7%。该差异通常是短暂的，矿车离开弯道后能够恢复直道速度。

### 与连续弯道联挂减速修复的关系

此前的弯道修复记录位于：

```text
docs/continuous-corner-linkage-speed-fix.md
```

该修复解决的是联挂车辆在连续弯道上的额外错误减速，包括：

- 弯道弦长被误判为车钩压缩。
- 不同轨道切线上的同速车辆被误判为存在相对速度。
- 同一个双向车钩在一个 tick 内被重复处理。

修复后，弯道上的同速车辆不会因为方向不同而产生错误阻尼，连续弯道也不应再随着弯角数量持续损失额外动能。

这项修复不会覆盖或修改 `SpeedController` 返回的轨道速度上限。因此，如果弯道继续返回 `0.4`，仍然会保留上述由轨道限速造成的短暂速度差，但不会恢复旧联挂算法造成的累积性显著减速。

既有的 `LINK_DRAG = 0.95` 仍然保留。该阻力同时作用于直道和弯道，不是弯道独有的减速来源。

## 坡道速度差异分析

坡道主要沿单一水平轴推进。弯道使用两个水平分量，而坡道的水平运动分量会直接受到 `0.4` 上限限制。

因此，当直道上限为 `0.6`、坡道上限为 `0.4` 时，坡道的水平推进上限比直道低：

```text
(0.6 - 0.4) / 0.6 = 33.3%
```

该差异比弯道明显。此外，上坡还会受到 Minecraft 原生坡道物理造成的速度损失，下坡则会获得相应加速。

连续弯道联挂修复仅处理转角轨道及车辆切线差异，不会取消坡道的显式速度限制，也不会移除原版坡道物理。

## 最终方案：全轨型统一 150%

为保证强化轨道在直道、弯道和坡道上都提供原版铁轨 150% 的速度体验，移除了弯道和坡道的独立 `0.4` 限制。

最终实现为：

```java
public static final float REINFORCED_MAX_SPEED = 0.6f;

REINFORCED {
    @Override
    public float getMaxSpeed(World world, @Nullable EntityMinecart cart, BlockPos pos) {
        return REINFORCED_MAX_SPEED;
    }
}
```

现在所有强化轨道形状均返回 `0.6`：

| 轨道形状 | 调整前上限 | 最终上限 | 相对原版铁轨 |
|---|---:|---:|---:|
| 直道 | `0.499` | `0.6` | 150% |
| 弯道 | `0.4` | `0.6` | 150% |
| 坡道 | `0.4` | `0.6` | 150% |

弯道不再发生由 `0.4` 分量限制造成的短暂速度下降，坡道也不再被强化轨道自身额外限制为原版速度。

## 二次调整：上下坡固定速度

游戏内复测发现，仅把坡道上限提高到 `0.6` 仍不足以获得稳定速度。矿车通过坡道时还会依次受到以下影响：

- 原版 `slopeAdjustment`：上坡减速、下坡加速。
- 矿车或机车自身的阻力与机车推力。
- 联挂弹簧和阻尼。
- 联挂车辆的 `LINK_DRAG`。
- 列车计算得到的当前矿车软速度帽。
- 原版载人矿车的 `0.75` 移动倍率。

为取消这些因素在强化坡道上的累积结果，新增：

```text
src/main/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedSlopeSpeedHandler.java
```

处理流程如下：

1. `MinecartUpdateEvent` 记录当前位于强化坡道上的矿车及坡道方向。
2. 在 `WorldTickEvent` 的 `END` 阶段以 `LOWEST` 优先级执行，使常规矿车更新、机车推力、阻力和联挂物理先完成。
3. 清除与坡道轴线垂直的运动分量，并覆盖之前叠加得到的速度。
4. 根据坡道朝向和行驶方向，预先抵消下一 tick 原版将要施加的 `slopeAdjustment`。
5. 在坡道移动期间临时把矿车当前速度帽提高到 `0.6`，移动结束后立即恢复原值。
6. 对使用原版载人矿车移动逻辑的车辆，同时预补偿 `0.75` 载人倍率。

以上处理使矿车下一 tick 在强化坡道上的实际水平推进速度固定为 `0.6`，无论正在上坡还是下坡：

```text
上坡：预置速度 + 原版减速量 = 0.6
下坡：预置速度 - 原版加速量 = 0.6
```

速度修正在联挂物理之后执行，因此弹簧、阻尼、`LINK_DRAG` 和列车软速度帽不会继续把最终坡道速度推离目标值。完全静止且没有行驶方向的矿车不会被强制启动；一旦其获得明确运动方向，固定速度逻辑才会生效。矿车类型自身声明的硬性最高速度仍作为安全上限保留。

## 三次调整：45° 斜线按合速度固定

强化轨道上限 `0.6` 最初直接交给 Minecraft 的矿车移动逻辑。该上限实际分别限制 X、Z 两个水平分量，而不是限制水平速度向量的长度。因此，车辆在 45° 斜线上受到持续牵引时，两个分量都可能接近 `0.6`，水平合速度最高可达到：

```text
sqrt(0.6² + 0.6²) ≈ 0.849 格/tick
```

这并不符合“沿斜线实际行驶速度为 `0.6 格/tick`”的目标。为此新增：

```text
src/main/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedDiagonalSpeedHandler.java
```

该处理器识别强化轨道的四种水平转角形状，并在常规移动、机车推力、阻力和联挂物理完成后，将 X/Z 速度向量归一化到 `0.6`。在标准 45° 方向上，两个分量分别为：

```text
X = ±0.6 / sqrt(2) ≈ ±0.424264
Z = ±0.6 / sqrt(2) ≈ ±0.424264
sqrt(X² + Z²) = 0.6 格/tick
```

Minecraft 1.12.2 的单个转角轨道内部使用 45° 对角路径；由交替转角轨道组成的长斜线也始终使用这四种轨道形状。因此，单个斜线段和连续长斜线都会应用相同的合速度修正，不会再随牵引时间逐渐升到约 `0.849`。

长斜线既有的联挂修复保持不变：相邻车辆的速度切线平行时仍使用完整的直线弹簧和阻尼；只有真正跨越方向变化时才使用弯道联挂修正。新的速度处理只修正向量长度，不改变行驶方向或该联挂判定。

原版载人矿车的 `0.75` 移动倍率同样得到预补偿；完全静止的矿车不会被强制启动；矿车类型声明的硬性最高速度仍作为安全上限。

## 本地化资源

强化轨道及相关强化轨道组件的提示文字已从 125% 更新为 150%，包括：

- 强化柔性轨道。
- 强化推进轨道。
- 强化交接轨道。
- 强化转辙轨道。
- 强化 Y 型轨道。

修改同步应用到主资源目录和实际参与资源构建的 `lang` 子模块，避免打包时旧的 125% 提示覆盖新文本。

涉及的语言包括：

- 英语
- 简体中文
- 繁体中文
- 德语
- 西班牙语
- 俄语
- 捷克语
- 匈牙利语
- 南非荷兰语

## 验证结果

使用 Java 8 执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./gradlew build
```

结果：

```text
BUILD SUCCESSFUL
```

测试包含连续弯道联挂物理、强化坡道固定速度和强化斜线合速度回归测试，验证了：

- 不同切线上的同速车辆不会产生错误阻尼。
- 弯道弦长不会产生伪压缩弹簧力。
- 弯道速度差只沿各车辆自身切线修正。
- 直轨仍保留原有阻尼行为。
- 弯道上的静止车辆仍可由相邻车辆带动。
- 东西方向坡道的上坡和下坡实际推进速度均为 `0.6`。
- 南北方向坡道的上坡和下坡实际推进速度均为 `0.6`。
- 原版载人矿车的 `0.75` 移动倍率得到预补偿。
- 没有行驶方向的静止矿车不会被强制启动。
- 45° 斜线上的 X/Z 分量均为约 `0.424264`，水平合速度为 `0.6`。
- 四种转角轨道形状均被识别，因此连续长斜线使用相同修正。

资源构建结果中，英文和简体中文强化轨道提示均已确认为 150%。同时执行了主项目及 `lang` 子模块的 `git diff --check`，未发现补丁格式错误。

## 建议的游戏内复测

建议在相同车辆编组、机车、载荷和燃料条件下进行以下对比：

1. 在足够长的强化直轨上记录稳定速度。
2. 通过单个 45° 斜线段和连续长斜线，确认水平合速度稳定为 `0.6`，且没有累积加速或减速。
3. 分别测试四种朝向的强化上坡和强化下坡，确认水平推进速度稳定为 `0.6`。
4. 分别测试单辆矿车、短编组和长编组。
5. 测试机车牵引与推行两种方向。
6. 观察高速弯道和坡道上的车钩间距、车辆重叠、振荡及脱轨情况。

## 本次涉及的项目文件

- `src/main/java/mods/railcraft/common/blocks/tracks/behaivor/SpeedController.java`
- `src/main/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedDiagonalSpeedHandler.java`
- `src/main/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedSlopeSpeedHandler.java`
- `src/main/java/mods/railcraft/common/modules/ModuleCore.java`
- `src/test/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedSlopeSpeedHandlerTest.java`
- `src/test/java/mods/railcraft/common/blocks/tracks/behaivor/ReinforcedDiagonalSpeedHandlerTest.java`
- `src/main/resources/assets/railcraft/lang/*.lang`
- `lang/src/main/resources/assets/railcraft/lang/*.lang`
- `docs/reinforced-track-150-percent-speed.md`

相关既有记录：

- `docs/continuous-corner-linkage-speed-fix.md`
