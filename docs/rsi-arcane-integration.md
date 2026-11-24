# RS Integration 与奥术转化桌

开发环境使用 Minecraft 1.20.1、Forge 47.3.22 和 ProjectE PE1.0.1。RSI 为可选运行时依赖，联动需要原生扩展 API 1.3。

Gradle 默认从相邻的 `../rs_integration_forked/build/libs/rs_integration-1.6.1-apiTest2.jar` 读取 Fork 的发布包，并通过 ForgeGradle 映射。对应 Fork 分支为 `codex/rsi-native-extension-api`，该 JAR 的模组元数据版本为 `1.6.1`。可用 `-PrsiForkDir` 指定其他 Fork 工作目录，用 `-PrsiForkVersion` 指定发布包文件名中的版本。联动时客户端和服务端使用匹配的 ProjectExA 与 RSI Fork 构建。

## 玩家行为

- 奥术转化桌放在玩家物品栏（含副手）或 Curios 饰品栏即可，无需打开转化桌。
- RSI 递归合成可用玩家已学会、具有正 EMC 值、符合持久 NBT 策略的物品补料。
- 使用玩家自己的 ProjectE EMC；不借用其他玩家、个人 EMC 仓库或转化桌输入槽内的 EMC。
- Refined Storage 和超越维度共用 RSI 取料账本。RSI 优先分配已启用的原生库存，再用 EMC 补足材料。EMC 补料跟随 RSI 的背包来源开关。
- 预留时校验同一批次共享的 EMC 余额。提交取料时按实际补出数量扣 EMC 并同步客户端。
- 合成计划的材料表及相应的物品提示显示 `（转化 N 个，X EMC）`。材料摘要下方显示预计总 EMC，余额不足时显示对应文案。
- 报价基于当前计划分配给 EMC 材料源的物品和数量，由服务端按当前 ProjectE 单价计算。在计划变化时重新请求，页面打开期间每秒刷新。计划材料信息不完整时，界面会提示暂时无法报价。
- 移走转化桌、遗忘知识或 EMC 不足时，执行阶段重新校验，不会免费生成物品。
- 携带奥术转化桌时，整条 RSI 递归合成成功结算后，自动学习本次实际合成的全部中间产物和最终产物中具有正 EMC 值的物品，并同步客户端；已被后续步骤消耗的中间产物也会学习。失败、取消、仅预览计划不会学习，成功结算时仍须携带转化桌。
- 配置文件 `config/project-expansion.toml` 中的 `rsiCraftingAutoLearn` 控制上述自动学习功能，默认 `true`；关闭它不影响现有 EMC 补料与报价。
- 完整知识使用 ProjectE 的 EMC 映射列表。物品经 ProjectE 持久 NBT 清理后才作为模板；不会凭配方要求复制任意附魔、耐久或自定义 NBT。
- 取消/失败时，已提取的补料按 RSI 原账本规则作为**实物**返还；不会同时返还 EMC。这也适用于提交中途失败。
- 不向 RS 网络或玩家背包主动批量灌入材料，不改变玩家的通用 Forge 物品 capability。

## 实现与边界

- `RSICraftingMaterialSources` 注册 EMC 材料源。所有可转化物品共享同一个额度池，RSI 负责确认分配、提交预留并结算实物。
- `RSIPlanExtensions` 提供按物品和来源汇总的计划分配，负责展示报价并过滤过期响应。
- `RecursiveCraftSucceededEvent` 提供成功结算后实际产出的物品，包含已被后续步骤消耗的中间产物。自动学习通过 ProjectE 的 `PlayerAttemptLearnEvent`，并同步新增知识。
- `RSIItemRetrievalInput` 协调 JEI 中键和世界选块，由接管输入的一侧发送请求。服务端用 `RSIItemRetrieval` 从 Refined Storage 或超越维度网络取物。世界选块默认取一个，Shift 取整组或补齐手中的同种物品。
- 缺货、连接不可用、权限不足或交付空间不足时，ProjectExA 可尝试 EMC 转化。确认有交付时按实际数量结束。转移结果不明且未确认交付时显示失败提示。两边都取不到时按场景显示失败提示或尝试收藏。
- 由 RSI 单独发起的取物仍可通过 `RSIItemRetrievalFallback` 在确认缺货后请求 EMC 补料，交付位置和数量由 RSI 请求指定。
- 创造模式物品栏的光标由客户端持有，网络取物沿用专用适配和 ProjectExA 的光标回执。
