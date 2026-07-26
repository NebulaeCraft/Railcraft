# 原版、镀铁与废弃轨道官方 3D 模型恢复记录

记录日期：2026-07-26

本文记录 Railcraft 1.12.2 项目中，将部分已经替换为客制模型的轨道恢复为 Railcraft-3D 官方 3D 模型的完整过程。工作范围包括废弃轨道转角、原版普通轨道、镀铁轨道，以及原版、镀铁和废弃轨道的全部统一道岔。

## 对话目标

本次工作分为四个阶段：

1. 将废弃轨道的四种水平转角恢复为官方 3D 模型，同时保留其直线和坡道模型。
2. 在误改现有官方转角模型后，将该文件精确恢复到 Git 基线版本。
3. 将原版普通轨道和镀铁轨道的直线、坡道、转角恢复为官方 3D 模型。
4. 将原版、镀铁和废弃轨道的 `junction`、`turnout`、`wye` 全部恢复为官方 3D 模型。
5. 修复原版和镀铁轨道安装组合式轨道套件后重新显示客制基底、且套件显示不完整的问题。

用户提供的官方材质包为：

```text
/Volumes/Shared/Railcraft-3D-1.0.5-vanilla.zip
```

该压缩包来自 Railcraft-3D 1.0.5，包含 Minecraft 1.12 和 Railcraft 使用的官方 3D 轨道模型。

## 修改原则

项目中的全局 Minecraft 轨道模型已经被客制版本覆盖：

```text
src/main/resources/assets/minecraft/models/block/rail_flat.json
src/main/resources/assets/minecraft/models/block/rail_curved.json
src/main/resources/assets/minecraft/models/block/rail_raised_ne.json
src/main/resources/assets/minecraft/models/block/rail_raised_sw.json
```

Railcraft 的高速、强化、镀铁等轨道原本会共享这些全局模型。直接把全局文件替换回官方版本，会连带取消其他轨道的客制外观。

因此本次采用隔离方案：

- 不覆盖现有全局客制模型。
- 把附件中的官方模型复制到 `railcraft:official_3d` 专用路径。
- 只修改目标轨道的模型引用。
- 高速、强化、电气、高速电气等未指定轨道继续使用客制模型。
- 不修改 Java 逻辑、碰撞、速度、轨道状态或贴图内容。

## 第一阶段：废弃轨道转角

### 原因定位

废弃轨道的 blockstate 位于：

```text
src/main/resources/assets/railcraft/blockstates/track_flex_abandoned.json
```

四种转角原先引用未限定命名空间的模型：

```json
"submodel": "rail_curved"
```

该引用最终解析到已经被客制化的：

```text
assets/minecraft/models/block/rail_curved.json
```

所以废弃轨道直线和坡道虽然仍显示官方 3D 外观，转角却显示客制模型。

### 复用项目内闲置模型

项目中存在一份没有被任何 blockstate 引用的官方同系转角模型：

```text
src/main/resources/assets/rccosmetic/models/block/track_curved.json
```

该模型包含 42 个 3D 元素，面纹理使用 `#rail_curved`，粒子纹理仍通过 `#rail` 解析。废弃轨道的四种转角因此改为：

```json
"textures": {
  "rail": "railcraft:blocks/tracks/flex/abandoned_turned",
  "rail_curved": "railcraft:blocks/tracks/flex/abandoned_turned"
},
"submodel": "rccosmetic:track_curved"
```

四种方向继续使用原来的旋转角度：

| 轨道形状 | 模型旋转 |
| --- | ---: |
| `south_east` | 0° |
| `south_west` | 90° |
| `north_west` | 180° |
| `north_east` | 270° |

这项修改只影响废弃轨道转角，不影响其他引用全局 `rail_curved` 的轨道。

## 第二阶段：恢复误改的转角模型

对话过程中，以下文件被意外修改：

```text
src/main/resources/assets/rccosmetic/models/block/track_curved.json
```

误改破坏了一个面的 JSON 字符串和闭合括号。由于沙箱不允许 Git 创建 `.git/index.lock`，无法直接执行 `git checkout --`，所以根据 `git diff` 精确恢复了受影响的两行。

恢复后完成以下验证：

- `jq empty` JSON 语法检查通过。
- 文件相对 Git 基线没有差异。
- 工作区文件与 `HEAD` 版本的 SHA-256 完全一致：

```text
9ad6645811140e0e49b582e1afdde0d3b1f7ef6344892430468b24759a40a3ef
```

此前对 `track_flex_abandoned.json` 的引用修改保持不变。

## 第三阶段：原版普通轨道和镀铁轨道

### 官方专用基础模型

项目中没有闲置的官方直线和坡道模型，因此从附件提取以下四份模型：

```text
src/main/resources/assets/railcraft/models/block/official_3d/rail_flat.json
src/main/resources/assets/railcraft/models/block/official_3d/rail_curved.json
src/main/resources/assets/railcraft/models/block/official_3d/rail_raised_ne.json
src/main/resources/assets/railcraft/models/block/official_3d/rail_raised_sw.json
```

提取后的文件与附件逐一核对，SHA-256 如下：

| 模型 | SHA-256 |
| --- | --- |
| `rail_flat.json` | `33003b82bf6bf29ea12fb9c7bbaf0d42b00eabe52f98d943feac407d33f39945` |
| `rail_curved.json` | `937d293f79c15fb466b23de64d84ff0ad85e99d2d7a785739b8202dcee1095ab` |
| `rail_raised_ne.json` | `e7158fef7edff8bdace76d25550493433b3289701eb0840640b7df884fa11727` |
| `rail_raised_sw.json` | `c06a4289f92ad5003357ee63dac021dc69f7764834502dc74d7195fddbb1cab1` |

### 原版普通轨道

Minecraft 1.12.2 的 `rail` blockstate 不直接引用 `rail_flat`，而是引用以下包装模型：

```text
normal_rail_flat
normal_rail_curved
normal_rail_raised_ne
normal_rail_raised_sw
```

为只恢复原版普通轨道而不影响其他共享全局模型的轨道，新增了四个同名资源覆盖：

```text
src/main/resources/assets/minecraft/models/block/normal_rail_flat.json
src/main/resources/assets/minecraft/models/block/normal_rail_curved.json
src/main/resources/assets/minecraft/models/block/normal_rail_raised_ne.json
src/main/resources/assets/minecraft/models/block/normal_rail_raised_sw.json
```

这些包装模型分别继承 `railcraft:block/official_3d/...`，并保留原版纹理：

- 直线和坡道：`minecraft:blocks/rail_normal`
- 转角：`minecraft:blocks/rail_normal_turned`

### 镀铁轨道

镀铁轨道 blockstate 位于：

```text
src/main/resources/assets/railcraft/blockstates/track_flex_strap_iron.json
```

其直线、两个坡道和四种转角全部改为显式引用：

```text
railcraft:official_3d/rail_flat
railcraft:official_3d/rail_curved
railcraft:official_3d/rail_raised_ne
railcraft:official_3d/rail_raised_sw
```

模型几何恢复为官方 3D，贴图仍使用镀铁轨道原有的：

```text
railcraft:blocks/tracks/flex/strap_iron_flat
railcraft:blocks/tracks/flex/strap_iron_turned
```

## 第四阶段：三种轨道的全部道岔

### 范围

恢复的轨道类型为：

- 原版轨道：`iron`
- 镀铁轨道：`strap_iron`
- 废弃轨道：`abandoned`

每种轨道包含三类统一道岔：

- `junction`：交叉道岔
- `turnout`：转辙道岔，共 8 种模型状态
- `wye`：Y 型道岔，包含普通和镜像模型及全部旋转状态

### 原因定位

上述轨道原先与高速、强化、电气等道岔共享：

```text
src/main/resources/assets/railcraft/models/block/outfitted/unified/
```

该目录下的 13 份共享模型均已被客制化。将这些共享文件直接替换回官方版本会影响所有道岔类型，因此继续采用专用路径隔离。

### 官方专用道岔模型

从附件提取 13 份模型到：

```text
src/main/resources/assets/railcraft/models/block/official_3d/outfitted/unified/
```

模型组成如下：

```text
track_junction.json
track_turnout_0.json
track_turnout_1.json
track_turnout_2.json
track_turnout_3.json
track_turnout_4.json
track_turnout_5.json
track_turnout_6.json
track_turnout_7.json
track_wye_0.json
track_wye_0_mirrored.json
track_wye_1.json
track_wye_1_mirrored.json
```

13 份文件均与附件中的对应文件逐一进行 SHA-256 比较，结果全部一致。

### Blockstate 引用调整

修改了以下九份文件：

```text
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/iron/junction.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/iron/turnout.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/iron/wye.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/strap_iron/junction.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/strap_iron/turnout.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/strap_iron/wye.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/abandoned/junction.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/abandoned/turnout.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/unified/abandoned/wye.json
```

共 57 个模型引用由：

```text
railcraft:outfitted/unified/...
```

改为：

```text
railcraft:official_3d/outfitted/unified/...
```

三份目标 `wye.json` 中原有的异常默认模型路径：

```text
railcraft:outfitted/unified/railcraft:track_wye
```

同时规范为有效的官方默认模型：

```text
railcraft:official_3d/outfitted/unified/track_wye_0
```

各 blockstate 原有贴图引用保持不变，本次只更换模型几何和模型路径。

## 第五阶段：组合式轨道套件基底

### 后续问题

完成普通轨道和统一道岔恢复后，游戏内发现原版轨道和镀铁轨道安装组合式轨道套件时仍会显示客制基底，部分套件几何还会被遮挡或显示不完整；废弃轨道没有该问题。

用户提供了完整和不完整状态的俯视对比截图：

```text
/Users/kuina/Desktop/截屏2026-07-26 10.32.34.png
/Users/kuina/Desktop/截屏2026-07-26 10.32.56.png
```

对比中可以看到，完整状态下横向枕木和套件组件会连续显示在两条钢轨之间；异常状态下只剩两侧或局部组件，中央区域出现明显缺失，并同时混入客制轨道基底。该视觉差异与组合模型中基底额外几何覆盖套件层的分析一致。

原因是普通 `track_flex` 和安装套件后的 `track_outfitted` 使用两套独立资源。`OutfittedTrackModel` 对 `COMPOSITE` 类型套件采用以下组合方式：

```text
最终模型 = 轨道类型基底模型 + 轨道套件模型
```

原版和镀铁轨道共同引用：

```text
railcraft:outfitted/type/normal
railcraft:outfitted/type/normal_raised_ne
railcraft:outfitted/type/normal_raised_sw
```

这三份模型已经被客制化，分别包含 22、93、93 个元素。附件中的官方对应模型均只有 7 个元素。客制基底中的额外几何与套件层叠加后会遮挡套件，同时让安装套件的轨道重新呈现客制外观。

废弃轨道使用独立的 `abandoned` 基底，没有引用上述客制 `normal` 模型，因此表现正常。

### 隔离修复

从附件提取三份官方套件基底：

```text
src/main/resources/assets/railcraft/models/block/official_3d/outfitted/type/normal.json
src/main/resources/assets/railcraft/models/block/official_3d/outfitted/type/normal_raised_ne.json
src/main/resources/assets/railcraft/models/block/official_3d/outfitted/type/normal_raised_sw.json
```

对应 SHA-256 为：

| 模型 | SHA-256 |
| --- | --- |
| `normal.json` | `18fce61764cbe58b60f5030c620f58b40613b04b355758c7604028c0db2fd103` |
| `normal_raised_ne.json` | `0aaf39516a13f71db44c3fc4f9051adcb2f3a888c30c5851214fc60a6490f252` |
| `normal_raised_sw.json` | `4ff2f7dcc0005ab67cdc621ca9640b7d07ee0dd848914a4b0c3cb15402df96c6` |

随后只修改：

```text
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/type/iron.json
src/main/resources/assets/railcraft/blockstates/tracks/outfitted/type/strap_iron.json
```

两类轨道的平直和坡道基底全部改为 `railcraft:official_3d/outfitted/type/normal...`。所有轨道套件自身的 blockstate、模型、状态和贴图均保持不变。

高速和强化轨道继续引用原客制 `railcraft:outfitted/type/normal...`，废弃轨道继续使用其原有独立官方基底。

## 最终影响范围

| 轨道类型 | 普通直线/坡道/转角 | `junction` | `turnout` | `wye` |
| --- | --- | --- | --- | --- |
| 原版普通轨道 / `iron` | 官方 3D；组合式套件使用官方基底 | 官方 3D | 官方 3D | 官方 3D |
| 镀铁轨道 / `strap_iron` | 官方 3D；组合式套件使用官方基底 | 官方 3D | 官方 3D | 官方 3D |
| 废弃轨道 / `abandoned` | 转角恢复官方 3D；既有直线和坡道保留 | 官方 3D | 官方 3D | 官方 3D |
| 高速轨道 | 保留客制模型 | 保留客制模型 | 保留客制模型 | 保留客制模型 |
| 强化轨道 | 保留客制模型 | 保留客制模型 | 保留客制模型 | 保留客制模型 |
| 电气及高速电气轨道 | 保留客制模型 | 保留客制模型 | 保留客制模型 | 保留客制模型 |

## 验证

本次完成了以下静态和构建验证：

1. 所有新增和修改的 JSON 均通过 `jq empty`。
2. 所有改动均通过 `git diff --check`。
3. 四份基础官方模型与附件逐一进行 SHA-256 比较，全部一致。
4. 十三份官方道岔模型与附件逐一进行 SHA-256 比较，全部一致。
5. 原版、镀铁和废弃道岔的 57 个模型引用均指向 `railcraft:official_3d`。
6. 高速、强化、电气和高速电气道岔仍指向原客制模型目录。
7. 三份官方组合式套件基底与附件逐一进行 SHA-256 比较，全部一致。
8. 原版和镀铁的套件基底指向 `railcraft:official_3d`，高速和强化仍指向原客制目录。
9. 多次执行以下资源构建均成功：

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./gradlew processResources
```

最终结果均为：

```text
BUILD SUCCESSFUL
```

构建输出中确认存在全部包装模型、四份基础官方模型、十三份官方道岔模型及修改后的 blockstate。

## 建议的游戏内复测

资源构建不会渲染 Forge 1.12.2 的组合模型，发布前建议在客户端完成以下检查：

1. 放置原版普通轨道的南北、东西、四种坡道和四种转角，确认均为官方 3D。
2. 对镀铁轨道重复相同方向检查，并确认贴图仍为镀铁版本。
3. 检查废弃轨道四种转角与直线、坡道的视觉连接。
4. 分别放置原版、镀铁和废弃轨道的 `junction`、`turnout`、`wye`。
5. 切换全部道岔状态并从两个基础朝向观察，确认模型旋转、镜像和纹理匹配。
6. 放置高速、强化和电气道岔，确认它们仍保持客制外观。
7. 在原版和镀铁轨道上逐一安装所有组合式轨道套件，检查平直和允许使用的坡道形状，确认套件完整且基底为官方 3D。
8. 在高速和强化轨道上安装同样的套件，确认它们仍保留客制基底。

## 未触碰内容

- 未修改 `api-railcraft`。
- 未修改 `lang`。
- 未修改轨道 Java 行为、矿车物理、碰撞箱、配方或方块状态定义。
- 工作区中原有的 `build.gradle` 和 `docs/apple-silicon-idea-runclient.md` 改动属于其他任务，本次未修改或撤销。
