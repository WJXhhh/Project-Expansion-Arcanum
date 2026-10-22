# 等价扩展：奥秘（Project Expansion: Arcanum）

Project Expansion 的独立维护分支，原作者 Donovan_DMC，维护者 JDJades。并非 ProjectE 官方附属项目。保留 MIT 许可及原作者署名，详见 NOTICE.md。

## 运行环境
Minecraft 1.21.1、NeoForge 21.1.148、Java 21、ProjectE PE1.1.0。运行命名空间为 projectexa。

## 本轮迁移
- 石质转化桌：学习、遗忘、物品转化、分页搜索及白名单。
- 终焉之星：无限 EMC 供应与基座复制；复制间隔及数据组件策略可配置。
- JEI：保留玩家 2×2 合成，较大配方转交奥术终端；其他菜单及非容器界面提供回退入口。服务端按配方 ID 重新查找配方并检查终端持有状态。
- 个人 EMC 仓库：绑定账户、27 个虚拟过滤槽、能力查询、弓弩弹药供给；非玩家生物背包及 Curios 可使用仓库与修复护符扩展。

仓库绑定后可交给他人或女仆使用，消耗绑定玩家的 EMC；绑定玩家必须在线。Shift 右键重新绑定；GUI 过滤槽不存放或消耗实体物品。

本轮不包含 Ars Nouveau、Create、Goety、Botania、TACZ、RSI 的专门配方或枪械联动。已有 EMI 奥术终端填充保持原状，新增的跨界面入口属于 JEI 功能。

## 开发与限制
使用 Java 21 执行 gradlew.bat compileJava。完整资源生成还需要脚本运行环境。编译通过不代表游戏内功能或旧存档迁移已验收；不要用未备份的重要旧存档进行跨版本升级。
