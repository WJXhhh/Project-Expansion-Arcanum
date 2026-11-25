# TaCZ EMC 联动

适配 Minecraft 1.21.1 NeoForge 的官方 TaCZ 1.2.0-hotfix。车万女仆供弹以 Touhou Little Maid 1.5.3 及 Curios 9.5.1 为测试组合。TaCZ 和车万女仆均为可选模组。[TaCZ 发布页](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files/9623169) · [车万女仆发布页](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid/files/8061852)

- 携带或装备奥术转化桌后，枪械工作台的转化按钮会按当前配方的材料和数量购买已学习的物品。服务器会校验工作台的配方筛选条件、材料和账户余额。余额不足时按可支付数量购买。
- ProjectE 根据枪械工作台的配方计算枪械、弹药和配件的 EMC。TaCZ 通过 `minecraft:custom_data` 存储型号和状态。项目使用型号对应的出厂状态进行定价及学习，保留枪包的默认虚拟配件。换弹、切换射击模式、加装配件等状态变化可通过出厂状态查询 EMC。弹药箱保留自身数据。
- 把已学习的对应弹药设为个人 EMC 仓库的过滤项后，TaCZ 玩家及车万女仆可从仓库换弹，费用由仓库绑定账户承担。女仆可把仓库放在背包或 Curios 栏中。玩家 HUD 的备弹数量包含仓库弹药，同一账户的多个仓库共享余额。

ProjectE 的 `mapping.toml` 中可配置 `mappers.Crafting-Mapper.TACZGunSmithTable` 与 `processors.TACZItemProcessor`。Arcanum 的 `server.toml` 中可配置 `persistentNbtEmcFallback`。配方额外赠送的可拆卸配件或预装弹药随状态清理移除，计算成本仍使用完整配方。

开发测试可运行 `gradlew runGameTestServer -PtaczGameTests`。

使用默认枪包验证时，ProjectE 接收了全部 200 条工作台配方。GameTest 覆盖改动状态后的枪弹配件 EMC、补料校验和扣费、玩家及女仆背包和 Curios 仓库供弹。单人客户端验证了按钮补料后材料数量即时更新，UMP45 从仓库装入 11 发并扣除 605 EMC。
