# 版本元数据与开发版提示修改记录

记录日期：2026-07-16

本文记录本次对话中对 Railcraft 项目版本信息、Mod 元数据以及进入游戏时开发版提示的修改。

## 修改目标

1. 将项目版本号修改为 `12.1.1`。
2. 将 Mod 描述修改为 `Modified for NebulaeCraft 5th Server.`。
3. 在作者列表中增加 `Kuina_20`。
4. 删除每次进入游戏时显示的开发版风险提示。

## 版本号与 Mod 元数据

项目版本号的唯一配置入口是：

```text
gradle.properties
```

修改为：

```properties
version=12.1.1
```

Mod 元数据位于：

```text
src/main/resources/mcmod.info
```

对应修改为：

```json
"description": "Modified for NebulaeCraft 5th Server.",
"authorList": [ "CovertJaguar", "Kuina_20" ]
```

该文件中的版本字段仍使用 `${version}`，构建时由 `build.gradle` 使用项目版本号替换。

## 删除开发版提示

提示文本定义在：

```text
src/main/java/mods/railcraft/common/core/BetaMessageTickHandler.java
```

它通过 `LivingEvent.LivingUpdateEvent` 向客户端玩家逐行发送红色聊天消息。

原本在 `Railcraft.init()` 中注册：

```java
MinecraftForge.EVENT_BUS.register(BetaMessageTickHandler.INSTANCE);
```

本次从以下文件移除了该注册：

```text
src/main/java/mods/railcraft/common/core/Railcraft.java
```

这样 `BetaMessageTickHandler` 不再接收 Forge 事件，因此进入游戏时不会再弹出这段说明；其他使用 `Game.DEVELOPMENT_VERSION` 的开发辅助逻辑保持不变。

## 验证

- `mcmod.info` JSON 格式验证通过。
- `git diff --check` 通过。
- 已确认项目中不再注册 `BetaMessageTickHandler.INSTANCE`。
