# Project Expansion：Arcanum

**面向 Minecraft 1.20.1 的 Project Expansion 维护分支。**

Arcanum 是 [Project Expansion](https://github.com/DonovanDMC/ProjectExpansion) 的独立维护分支，原项目由 **Donovan_DMC** 创建。Arcanum 的首个版本仅专注于维护、兼容性、工程清理和已验证的错误修复，不新增方块、物品、机器、配方、机制、GUI 功能或玩法内容。

当前维护者身份：**首个正式版本发布前确认**。

## 兼容性与迁移

Arcanum 使用独立的运行时模组 ID 及注册表/资源命名空间 `projectexa`，以避免与其他翻译版和分支发生冲突。官方 Project Expansion `1.1.3` 世界使用旧的 `projectexpansion` 命名空间；Arcanum 会在世界加载期间，通过 Forge 缺失映射处理器和方块实体 NBT 迁移钩子，将匹配的方块、物品、方块实体、菜单、附魔和声音映射到新的命名空间。

迁移前请备份存档并关闭 Minecraft：

1. 移除官方的 `projectexpansion-1.20.1-1.1.3.jar`。
2. 安装 Arcanum 的 JAR 文件。
3. 不要同时安装两个 JAR 文件。
4. 使用备份后的世界启动，并检查机器、物品、EMC、玩家数据和配置。完成映射后，迁移内容将注册在 `projectexa` 命名空间下。

迁移和运行时检查属于发布门禁测试。Arcanum 与 ProjectE 没有隶属或官方支持关系；ProjectE 相关问题请提交给 ProjectE 项目，而不是本项目。

## 支持环境

- Minecraft `1.20.1`
- Forge `1.20.1`
- ProjectE `PE1.0.1`（`1.20.1`）
- Java 17

可选集成仍然保持可选，包括 Curios、JEI、Jade、WTHIT、The One Probe、AE2 和 Pipez。

## 开发与构建

请使用 Java 17，并从干净的工作区构建：

```bat
gradlew.bat clean build
```

Linux 或 macOS：

```sh
./gradlew clean build
```

发布 JAR 会生成在 `build/libs/` 目录，文件名包含 `projectexa` 和 `arcanum`。项目使用 Gradle Wrapper，并配置了最小化的 GitHub Actions 构建流程。

## 相关链接

- [English README](README.md)
- [Arcanum 仓库](https://github.com/WJXhhh/Project-Expansion-Arcanum)
- [提交 Bug](https://github.com/WJXhhh/Project-Expansion-Arcanum/issues/new/choose)
- [原 Project Expansion 仓库](https://github.com/DonovanDMC/ProjectExpansion)
- [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte)

## 许可证与致谢

项目保留原始 MIT 许可证及 Donovan_DMC 的版权声明，详见 [LICENSE](LICENSE) 和 [NOTICE](NOTICE.md)。项目也保留 ProjectE 的致谢信息；Arcanum 不是官方 ProjectE 附属项目，也不受 ProjectE 官方支持。
