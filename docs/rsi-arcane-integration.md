# RS Integration：奥术转化桌递归合成补料

开发环境使用 Minecraft 1.20.1、Forge 47.3.22 和 ProjectE PE1.0.1。
RSI 是可选依赖，安装时版本须为 1.6.1 或以上。
Gradle 从 Modrinth 获取 RSI 1.6.1 正式版（版本 ID：`FHDN9Mvs`），通过 ForgeGradle 映射后用于
`compileOnly` 和 `runtimeOnly`。

## 玩家行为

- 奥术转化桌放在玩家物品栏（含副手）或 Curios 饰品栏即可，无需打开转化桌。
- RSI 递归合成会把已学会、具有正 EMC 值的物品计入可用材料。
- 使用玩家自己的 ProjectE EMC；不借用其他玩家、个人 EMC 仓库或转化桌输入槽内的 EMC。
- 配方按 RSI 原有存储来源顺序取料。安装 RS 时，EMC 从玩家/背包来源补料，位于实物之后；仅安装超越维度时，EMC 是网络虚拟库存的一部分，遵循 RSI 网络优先的分配顺序。
- 预览、模拟提取、预留和回退预留不扣 EMC。只有 RSI 提交提取时，才按实际补出数量扣 EMC 并同步客户端。
- 合成计划的材料表及相应的物品提示显示 `（转化 N 个，X EMC）`。材料摘要下方显示预计总 EMC，余额不足时显示对应文案。
- 估算读取 RSI API 提供的外部物品需求，按启用的材料来源和取料顺序分配库存。报价在计划变化时重新请求，页面打开期间每秒刷新，执行时按实际提取结算。
- RSI 未提供完整外部需求时，界面提示计划材料信息不完整。界面也区分网络读取、网络权限和背包读取失败。
- 仅安装超越维度时，也支持混用玩家背包和精妙背包，包含主物品栏及 Curios 中背包的可用内容。先计算网络实物与网络 EMC 分配，再用玩家/背包实物满足余量；不会错误地把这些实物从网络优先使用的 EMC 数量中再次扣除。因此，有玩家实物不代表网络虚拟 EMC 一定不会被使用。
- RS 与超越维度同时安装时，按计划记录的后端与网络标识读取唯一的选中网络；EMC 只走既有背包补料路径，不同时叠加超越维度的网络虚拟 EMC，不合并两个网络的库存或费用。
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
- 携带奥术转化桌并启用对应功能时，与 RSI 冲突的 JEI 中键及世界选块各发一次请求。服务端先尝试 RSI 网络取物。明确未取得物品时（包括没有可用网络），按 ProjectExA 原有规则转化。JEI 沿用取物模式和数量配置，世界选块保留普通单取、Shift 整组和补齐。两边都取不到时保留 RSI 的失败提示或收藏。
- 由 RSI 单独发起的取物仍可通过 `RSIItemRetrievalFallback` 在确认缺货后请求 EMC 补料，交付位置和数量由 RSI 请求指定。

接口见 [RSI 1.6.1 扩展提交](https://github.com/Elten-huanghuang/rs_integration/commit/6dbfe6f765)。

### 仅安装超越维度、不安装 Refined Storage

RSI 的 `ExtractionLedger` 仍包含旧 RS API 字节码。无 RS 时不对这个类应用本模组的
Mixin，避免 ModLauncher 在重算栈帧时解析不存在的 `IStorageCache`。
改在 RSI 的 `BeyondDimensionsSession` 合成调用中，将玩家可用的知识/EMC 加入网络材料快照，
实际从超越维度网络提取不足时再用玩家 EMC 补足并同步。非合成调用不暴露这份虚拟库存。
取消或失败后的已提取材料由 RSI 按网络来源作为实物返还，不同时返还 EMC。
无 RS 且没有超越维度网络时，本路径不提供 EMC 补料。

费用估算不通过 `ExtractionLedger` 的访问器扫描精妙背包，而是独立读取其
`CapabilityBackpackWrapper` 与 `getInventoryForUpgradeProcessing`，避免无 RS 时加载账本中的旧 RS API。
只读取主物品栏及装备的 Curios 中的背包内容，沿用 RSI 的受保护物品规则，不把普通 Curios
物品、盔甲、副手或嵌套背包额外当作材料来源。每个来源只复制并统计一次，后续分配仅修改内存中的物品副本。
BD 虚拟库存报价使用与会话 Mixin 相同的知识模板、单项可转化上限和规范化物品标识；
同一 BD 标识不会在多种材料需求间重复分配。余额变化导致的未满足部分仍显示所需 EMC，避免误报零花费。

### 安装 Refined Storage 时

`RsiTypedMaterialSourcesMixin` 在服务端主线程为 `countInventoryForTypes` 追加虚拟材料快照。
当背包来源启用时，即使未安装精妙背包也可使用虚拟材料。物品类型筛选与来源开关沿用 RSI。

`RsiExtractionLedgerMixin` 在 `selectedBackpackInventories` 返回时追加虚拟材料。
任务的初始材料准入也走该账本。预留时读取 `pendingInv`，按已启用的实物来源计算仍需占用的 EMC，
避免跨材料重复预留同一笔余额。实际提取沿用 RSI 的 `PLAYER_INVENTORY` 来源、提取记录及取消返料。

Mixin 选择与报价共同使用 `RsiStorageCompat` 判定启用的补料路径。报价独立读取实物，
根据计划的库存、背包和网络开关计算费用。

**预览限制：**RSI 的材料快照是按物品计数，不支持表达“多种物品共享一种货币”。
因此每种物品显示的是单独可转化的上限，不是所有物品可同时取得的承诺。
若整份计划的总 EMC 超过余额，实际账本预留会拒绝超额部分；不会透支。
RSI 的仅扫描主物品栏的特殊 `reserveUpToFromMainInventoryThenNetwork` 路径不扩展，
本联动针对递归配方的常规材料预留/精确预留路径。
