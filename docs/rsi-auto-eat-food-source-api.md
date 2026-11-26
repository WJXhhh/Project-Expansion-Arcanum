# RSI 一键进食的外部食物来源 API 制作提案

适用环境为 Minecraft 1.20.1 / Forge。本文供 RSI fork 和 ProjectExA 的实现者使用。核对基线为 RSI fork 的 `master@57cdd072`，其进食模块及公开 API 与此前调研的 `codex/rsi-native-extension-api@28e03db2` 相同。基线的原生扩展 API 版本为 1.3。下文的接口名、签名和结算规则均为提案。[RSI 基线源码](https://github.com/WJXhhh/rs_integration_forked/tree/57cdd072ae64e835e84dfc29798be9e07c643445)

## 玩家行为

玩家携带奥术转化桌，在已连接且有查看、提取权限的终端点击 RSI 原有的进食按钮。RSI 根据当前进食模式，同时考虑网络里的食物和这名玩家已学会、能用个人 EMC 转化的食物。对于选中的同一食物变体，优先消耗网络实物，缺货时才转化一份。每次点击的食用数量仍由 RSI 的模式和批次限制决定。

例如，玩家选中蘑菇煲，网络里还剩一份，个人 EMC 足以转化两份。若饥饿度等条件允许连续吃三份，第一份从网络提取，后两份各在进食前扣一次 EMC。若服务器配置了每份进食另收费用物品，三份各自都要缴费。吃完产生的碗沿用 RSI 的容器返还流程。

## 为什么需要扩展

三个模式先从 `AutoEatStorage.items()` 枚举网络快照，再从同一存储提取选中的食物。零库存食物在枚举阶段就会被排除，仅添加缺货回调覆盖不到这一情形。现有 `RSICraftingMaterialSources` 由递归合成调用，`RSIItemRetrievalFallback` 的场景仅有 JEI 和世界选块。建议给进食服务单独提供**候选枚举**和**取得一份**两个操作，继续使用现有按钮、偏好及请求包。[进食引擎](https://github.com/WJXhhh/rs_integration_forked/blob/57cdd072ae64e835e84dfc29798be9e07c643445/src/main/java/com/huanghuang/rsintegration/feature/autoeat/AutoEatEngine.java#L306) · [存储适配](https://github.com/WJXhhh/rs_integration_forked/blob/57cdd072ae64e835e84dfc29798be9e07c643445/src/main/java/com/huanghuang/rsintegration/feature/autoeat/AutoEatStorage.java) · [取物兜底的场景定义](https://github.com/WJXhhh/rs_integration_forked/blob/57cdd072ae64e835e84dfc29798be9e07c643445/src/main/java/com/huanghuang/rsintegration/api/RSIItemRetrievalFallback.java#L32)

## RSI 公开接口草案

建议在 `com.huanghuang.rsintegration.api` 新增 `RSIAutoEatFoodSources`。沿用 fork 的原生材料源注册风格，在通用初始化阶段按唯一来源 ID 注册，重复 ID 返回 `false`。以下为公开签名草案，省略实现和导入。

```java
public final class RSIAutoEatFoodSources {
    public static boolean register(ResourceLocation id, Source source) { /* ... */ }

    public interface Source {
        List<ItemStack> candidates(Context context);
        TakeResult takeOne(Context context, ItemStack exactTemplate);
    }

    public record Context(ServerPlayer player, Set<Item> wantedItems) {}

    public sealed interface TakeResult {
        record Taken(ItemStack stack) implements TakeResult {}
        record Unavailable() implements TakeResult {}
        record Indeterminate() implements TakeResult {}
    }
}
```

| 接口 | 调用与返回约定 |
| --- | --- |
| `Context` | RSI 完成请求、网络权限和模式前置校验后，在服务端主线程创建。堆叠暴食传入玩家选定的物品类型，多样饮食和膳食均衡传空集合，表示查询全部可供食物。集合取不可变副本。玩家的选择顺序由 RSI 另行保留。 |
| `candidates` | 返回此刻至少可供应一份的精确食物模板，每个模板数量为 1，保留影响物品身份的 NBT。枚举只读，可重复调用。每个模板的可用性独立估算，多个候选可以共享同一份 EMC。RSI 收到后复制模板，仅用于当前服务端批次。 |
| `takeOne` | RSI 传回曾由该来源提供的精确模板。来源按实时状态复核，在成功时交付数量恰为 1、身份匹配的独立栈，并完成该份的来源扣费。返回的栈归 RSI 持有，来源不得同时放入背包或网络。 |
| `TakeResult` | `Taken` 表示已明确交付；`Unavailable` 表示本次无扣款、无交付，可以换来源；`Indeterminate` 表示来源无法确认扣款或交付结果，RSI 应终止本次请求。提交抛出异常或返回违约结果按结果不明处理，记录来源 ID，避免重复转化。 |

## RSI 进食引擎接入

**候选集合。** 保留原生快照顺序，随后按来源 ID 的稳定顺序加入外部模板。用网络端点的精确物品身份合并相同模板，同时保留能供应它的来源。相同物品 ID 的不同 NBT 变体可以分别入选。一次服务端批次只枚举一次，跨 tick 执行的批次重新读取玩家状态。尤其在玩家拥有完整知识时，避免每吃一口都遍历全部 EMC 映射。

| 模式 | 修改位置及应保留的选食行为 |
| --- | --- |
| 多样饮食 `DIVERSITY` | 将联合候选交给 `executeDiversity`。继续通过 Spice of Life: Carrot Edition 检查食物是否计数、是否吃过，并应用该模式的物品及效果黑名单。 |
| 堆叠暴食 `STACK` | 在 `executeStack` 中按已选物品 ID 对联合候选分组，保留多选轮转、效果黑名单、饥饿度判断和请求上限。即使选中的某种食物在网络里从未出现，也可以由外部来源单独构成一组。 |
| 膳食均衡 `DIET` | 将联合候选交给 `executeDiet`，继续按 Diet 当前最低营养组和该食物的实际分组选择。某个候选因余额变化失效时，继续查找同组其余候选；本轮排除已确认耗尽的来源，避免反复选中同一份无法取得的食物。 |

**每份的结算顺序。** 建议在三种模式中共用下面的取食流程，进食判定、各模式的模组回调和批次计数继续由 RSI 持有。

1. 按模式选中精确模板，通过该模式的食用条件。沿用已认证的网络端点优先提取一份。原生提取成功时按既有路径缴费、进食和返还。原生端点不可用、权限失效或转移结果不明时结束请求。外部路径只在已确认网络缺货时启用。现有 `AutoEatStorage.extract` 将结果简化成物品栈，接入处应保留 `StorageRestockSupport.extractOutcome` 的 `confirmedMissing`，以区分缺货和后端异常。[存储提取结果](https://github.com/WJXhhh/rs_integration_forked/blob/57cdd072ae64e835e84dfc29798be9e07c643445/src/main/java/com/huanghuang/rsintegration/storage/StorageRestockSupport.java)
2. 外部来源对该模板有报价时，先从原生网络收取本份的 RSI 费用物品，由 RSI 暂存实际取出的栈。费用为零直接进入下一步；费用不足则返还已取部分并结束。本步只访问原生网络，即使玩家学过费用物品，也不能通过食物来源支付。当前 `payCost` 只返回布尔值，外部路径需要保留实际取出的费用栈，以便后续归还。[现有进食扣费代码](https://github.com/WJXhhh/rs_integration_forked/blob/57cdd072ae64e835e84dfc29798be9e07c643445/src/main/java/com/huanghuang/rsintegration/feature/autoeat/AutoEatEngine.java#L441)
3. 对选中模板调用 `takeOne`。`Unavailable` 时可在本份已暂存费用的前提下尝试下一来源。所有来源都不可用时，返还费用，再按模式查找其他候选。`Indeterminate` 时结束请求，返还 RSI 明确持有的费用，并停止重试该次转化。返还实物时采用网络、玩家背包、掉落的顺序。
4. 收到 `Taken` 后，按原模式执行提取后的食用校验。如果此时无法进食，返还已取得的食物实物和暂存费用各一次。外部来源已完成该食物的转化，退回实物即已结清，不能再给玩家退同一份 EMC。
5. 一切就绪后确认本份 RSI 费用，调用原有 `finishUsingItem`、完成事件和模式回调。碗、瓶等按实际返回栈处理。若食用回调已开始后抛出异常，停止该请求并记录错误；回调可能已产生效果，此时重试或补发原食物会重复结算。

## ProjectExA 适配

建议注册来源 ID `projectexa:arcane_emc`。ProjectExA 只负责判断能否提供某个精确变体，以及为一次成功交付扣除多少 EMC。

- **候选枚举。** 用现有 `hasTablet(player)` 检查玩家的物品栏、副手和 Curios。读取玩家自己的 `IKnowledgeProvider`；普通知识遍历 `getKnowledge()`，完整知识遍历 `EMCMappingHandler.getMappedItems()`。尽早按 `wantedItems` 和可食用性过滤，再按现有持久 NBT 规则清理、去重。只返回玩家已学会、单价大于零且当前余额足以支付一份的模板。[现有 EMC 材料源](../src/main/java/com/wjx/forge/projectexa/integrations/rsi/RsiEmcMaterialSource.java) · [转化桌携带判定](../src/main/java/com/wjx/forge/projectexa/net/packets/to_server/PacketOpenArcaneTransmutationTablet.java)
- **单份交付。** 在服务端主线程复核携带状态、知识、模板身份和当前单价，再用当前余额扣除一份，返回该精确食物的独立栈并同步 EMC。移走转化桌、遗忘、不可转化或余额不足时返回 `Unavailable`。若扣款状态不明，返回 `Indeterminate`；若扣款已确认、仅客户端同步失败，则仍交付已付款的一份，并记录同步失败。现有 `provider`、`unitValue` 和扣款实现可作为复用起点。[EMC 校验与扣款](../src/main/java/com/wjx/forge/projectexa/integrations/rsi/RsiEmcMaterialSource.java)
- **版本兼容。** RSI 发布时应声明新增公开接口的版本。ProjectExA 将新接口的注册放在独立兼容类，先检查其是否存在，再加载注册类。只提供 API 1.3 的 fork 仍可走原有合成和取物联动。整个功能可沿用现有进食客户端和同步包，两端继续遵守各模组既有的版本匹配要求。[现有兼容入口](../src/main/java/com/wjx/forge/projectexa/integrations/rsi/RsiCompatibility.java)
