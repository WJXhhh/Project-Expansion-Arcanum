# RS Integration：奥术转化桌递归合成补料

适配开发依赖：Minecraft 1.20.1 / Forge 47.3.22 / ProjectE PE1.0.1 /
RS Integration 1.5.0.1（CurseForge 文件 9065984）。RSI 仍然是可选依赖。

## 玩家行为

- 奥术转化桌放在玩家物品栏（含副手）或 Curios 饰品栏即可，无需打开转化桌。
- RSI 递归合成会把已学会、具有正 EMC 值的物品计入可用材料。
- 使用玩家自己的 ProjectE EMC；不借用其他玩家、个人 EMC 仓库或转化桌输入槽内的 EMC。
- 配方按 RSI 原有存储来源顺序取料；本联动在物理背包之后提供缺少的物品。
- 预览、模拟提取、预留和回退预留不扣 EMC。只有 RSI 提交提取时，才按实际补出数量扣 EMC 并同步客户端。
- 移走转化桌、遗忘知识或 EMC 不足时，执行阶段重新校验，不会免费生成物品。
- 完整知识使用 ProjectE 的 EMC 映射列表。物品经 ProjectE 持久 NBT 清理后才作为模板；不会凭配方要求复制任意附魔、耐久或自定义 NBT。
- 取消/失败时，已提取的补料按 RSI 原账本规则作为**实物**返还；不会同时返还 EMC。这也适用于提交中途失败。
- 不向 RS 网络或玩家背包主动批量灌入材料，不改变玩家的通用 Forge 物品 capability。

## 实现与边界

`RsiMaterialSourcesMixin.countInventory` 在服务端主线程追加虚拟材料快照，兼容未安装
Sophisticated Backpacks 的情况；安装该模组时过滤重复的虚拟来源，避免双计数。
该快照同时进入 RSI 同步规划和异步规划的不可变输入，不在异步线程读取玩家能力。

`RsiExtractionLedgerMixin.findAllBackpackInventories` 将虚拟材料追加到 RSI 自身的背包列表。
实际提取沿用 RSI 的 `PLAYER_INVENTORY` 来源、提取记录及取消返料。
预留时读取同一账本的 `pendingInv`，扣除其他物品超出物理库存的预留 EMC，
避免同一余额在多种材料间重复预留；原有撤销预留也自动释放这部分预算。

**预览限制：**RSI 的材料快照是按物品计数，不支持表达“多种物品共享一种货币”。
因此每种物品显示的是单独可转化的上限，不是所有物品可同时取得的承诺。
若整份计划的总 EMC 超过余额，实际账本预留会拒绝超额部分；不会透支。
RSI 的仅扫描主物品栏的特殊 `reserveUpToFromMainInventoryThenNetwork` 路径不扩展，
本联动针对递归配方的常规材料预留/精确预留路径。

测试：`python scripts/tests/test_rsi_arcane.py`（JDK 17+，无需启动游戏）。
测试编译并执行实际虚拟库存源，使用窄接口替身验证知识、计费、模拟、共享预留预算、
物理库存优先、重复来源过滤、大整数及能力失效。它不是游戏端到端测试。

原生账本测试：`python scripts/tests/test_rsi_arcane_native.py`，需要本机已缓存开发依赖，
并存在用户已接受的 `run/eula.txt`（也可用 `--eula PATH` 指定）。
它创建独立临时世界，仅监听回环地址/随机端口，使用 Forge FakePlayer 执行真实 RSI
材料快照、普通/精确预留、EMC 扣费、取消返料、跨材料共享预算和移走转化桌测试，
结束后自动关闭测试服务器并清理测试订阅者的编译输出。不打开已有存档。
这些测试不等价于真实玩家的 JEI 点击端到端测试；后者仍需游戏内验收。
