# RS Integration：奥术转化桌递归合成补料

开发环境使用 Minecraft 1.20.1、Forge 47.3.22 和 ProjectE PE1.0.1。
RSI 是可选依赖，安装时版本须为 1.6.1 或以上。
Gradle 从 Modrinth 获取 RSI 1.6.1 正式版（版本 ID：`FHDN9Mvs`），通过 ForgeGradle 映射后用于
`compileOnly` 和 `runtimeOnly`。

## 玩家行为

- 奥术转化桌放在玩家物品栏（含副手）或 Curios 饰品栏即可，无需打开转化桌。
- RSI 递归合成会把已学会、具有正 EMC 值的物品计入可用材料。
- 使用玩家自己的 ProjectE EMC；不借用其他玩家、个人 EMC 仓库或转化桌输入槽内的 EMC。
- Refined Storage 和超越维度共用 RSI 取料账本。递归合成依次使用已启用的网络实物、玩家主物品栏和背包实物，剩余需求由 EMC 补足。EMC 补料跟随 RSI 的背包来源开关，即使未安装精妙背包也可启用。
- 预览、模拟提取、预留和回退预留不扣 EMC。只有 RSI 提交提取时，才按实际补出数量扣 EMC 并同步客户端。
- 合成计划的材料表及相应的物品提示显示 `（转化 N 个，X EMC）`。材料摘要下方显示预计总 EMC，余额不足时显示对应文案。
- 估算读取 RSI API 提供的外部物品需求，按启用的材料来源和取料顺序分配库存。报价在计划变化时重新请求，页面打开期间每秒刷新，执行时按实际提取结算。
- RSI 未提供完整外部需求时，界面提示计划材料信息不完整。界面也区分网络读取、网络权限和背包读取失败。
- 费用估算和取料使用计划选中的单个网络。背包来源包括玩家主物品栏和 Curios 饰品栏中的精妙背包。
- 移走转化桌、遗忘知识或 EMC 不足时，执行阶段重新校验，不会免费生成物品。
- 携带奥术转化桌时，整条 RSI 递归合成成功结算后，自动学习本次实际合成的全部中间产物和最终产物中具有正 EMC 值的物品，并同步客户端；已被后续步骤消耗的中间产物也会学习。失败、取消、仅预览计划不会学习，成功结算时仍须携带转化桌。
- 配置文件 `config/project-expansion.toml` 中的 `rsiCraftingAutoLearn` 控制上述自动学习功能，默认 `true`；关闭它不影响现有 EMC 补料与报价。
- 完整知识使用 ProjectE 的 EMC 映射列表。物品经 ProjectE 持久 NBT 清理后才作为模板；不会凭配方要求复制任意附魔、耐久或自定义 NBT。
- 取消/失败时，已提取的补料按 RSI 原账本规则作为**实物**返还；不会同时返还 EMC。这也适用于提交中途失败。
- 不向 RS 网络或玩家背包主动批量灌入材料，不改变玩家的通用 Forge 物品 capability。

## 实现与边界

### RSI 1.6.1 扩展接口

- `RSIPlanExtensions` 提供计划快照、外部材料需求、网络引用和来源开关。ProjectExA 从服务端读取 EMC 报价，RSI 负责展示并过滤过期计划的响应。
- `RecursiveCraftSucceededEvent` 提供成功结算后实际产出的物品，包含已被后续步骤消耗的中间产物。自动学习通过 ProjectE 的 `PlayerAttemptLearnEvent`，并同步新增知识。
- 携带奥术转化桌并启用对应功能时，与 RSI 冲突的 JEI 中键及世界选块各发一次请求。服务端先按请求的数量和交付位置尝试 RSI 网络取物。JEI 使用设置中的单取或整组、背包或光标模式。世界选块在安装 Refined Storage 时会尝试网络取物，普通选块取一个，Shift 取整组或补齐手中的同种物品。
- 确定没有交付且符合兜底条件时，按同一次请求尝试 EMC 转化。网络已经部分交付时以实际数量结束，转移结果不明时提示失败。两边都取不到时按场景显示失败提示或尝试收藏。
- 由 RSI 单独发起的取物仍可通过 `RSIItemRetrievalFallback` 在确认缺货后请求 EMC 补料，交付位置和数量由 RSI 请求指定。

接口见 [RSI 1.6.1 扩展提交](https://github.com/Elten-huanghuang/rs_integration/commit/6dbfe6f765)。

### 共用取料账本

RSI 1.6.1 的 `ExtractionLedger` 已移除旧 RS 类型引用。只安装超越维度时也启用以下补料接入。
`RsiTypedMaterialSourcesMixin` 在服务端主线程为 `countInventoryForTypes` 追加虚拟材料快照。
物品类型筛选与来源开关沿用 RSI。RSI 支持无网络的递归合成场景也可通过背包来源使用 EMC 补料。

`RsiExtractionLedgerMixin` 在 `selectedBackpackInventories` 返回时追加虚拟材料。
任务的初始材料准入也走该账本。预留时读取 `pendingInv`，按已启用的实物来源计算仍需占用的 EMC，
避免跨材料重复预留同一笔余额。实际提取沿用 RSI 的 `PLAYER_INVENTORY` 来源、提取记录及取消返料。

报价独立读取选中网络和启用的实物来源。精妙背包库存通过 `CapabilityBackpackWrapper` 与
`getInventoryForUpgradeProcessing` 读取，沿用 RSI 的受保护物品规则。每个实物来源只复制并统计一次，
报价分配使用物品副本，按实物满足后的可转化缺口计算 EMC 费用。

**预览限制：**RSI 的材料快照是按物品计数，不支持表达“多种物品共享一种货币”。
因此每种物品显示的是单独可转化的上限，不是所有物品可同时取得的承诺。
若整份计划的总 EMC 超过余额，实际账本预留会拒绝超额部分；不会透支。
