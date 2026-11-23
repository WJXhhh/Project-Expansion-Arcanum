# RSI 第三方集成 API 建议

适用环境为 Minecraft 1.20.1 / Forge。2026-11-23 复审以 RSI 1.6.1 正式版（Modrinth 版本 ID `FHDN9Mvs`）和上游 `master` 的 `00e5332` 为基准。以下接口名称和注册方式可沿用 RSI 现有风格。 [RSI 上游提交](https://github.com/Elten-huanghuang/rs_integration/commit/00e533253e6a200b63eeefe3ea1660e3ab901480) · [Modrinth 版本记录](https://api.modrinth.com/v2/version/FHDN9Mvs)

## RSI 1.6.1 已提供的接口

这三个接口都已被 ProjectExA 使用。当前仍有 [9 个直接针对 RSI 的 Mixin](../src/main/resources/projectexa.mixins.json)，对应下文的三个缺口。

| 接口 | 已提供的行为与边界 |
| --- | --- |
| `RSIPlanExtensions` | 提供计划标识、库存修订号、合成次数、所选网络、来源开关和外部材料需求。RSI 排版摘要、材料备注与悬浮提示，并丢弃过期装饰。ProjectExA 用它展示 EMC 报价，当前仍需自行估算各材料来源承担的数量。 |
| `RecursiveCraftSucceededEvent` | 原生递归任务成功结算后在服务端主线程通知一次。事件记录实际合成的新产物，包括之后被消耗的中间产物，并排除已识别的退料、容器和催化剂返还。ProjectExA 据此学习产物。 |
| `RSIItemRetrievalFallback` | 原生 JEI 取物或世界选块通过权限和交付目标检查、确认缺货且实际交付为零时，调用服务端兜底处理器。`PASS` 无副作用，`Delivered(actualCount)` 表示已实际交付并跳过原失败提示或收藏。无网络、无权限、无空间、后端异常和部分交付仍走 RSI 原流程。 |

[RSI 原生交互扩展文档](https://github.com/Elten-huanghuang/rs_integration/blob/00e533253e6a200b63eeefe3ea1660e3ab901480/docs/api/README.md#18-原生交互扩展)

## 1. 注册递归合成材料源

允许第三方注册材料源，参与原生递归合成的规划、执行前校验、预留和实际取料。RSI 提供玩家、所选网络（如有）、已启用的材料来源、待查询的物品类型以及计划或任务上下文。Refined Storage 和 Beyond Dimensions 共用契约。RSI 按来源开关和取料顺序分配，同一个需求可由多个来源共同满足，物品变体沿用 RSI 的匹配规则。

| 阶段 | 需要的行为 |
| --- | --- |
| 规划 | 在服务端主线程读取玩家与实时库存，异步规划使用快照。材料源能累计同一次试分配已占用的额度，并在回退候选方案时撤销相应占用。 |
| 校验与预留 | 执行前按当前状态重新校验。为任务内指定的物品变体和数量返回预留句柄或明确的不足结果。预留只占额度，实际扣费留到提交。 |
| 释放 | 放弃分配、任务取消或执行失败时释放尚未提交的预留。 |
| 提交 | 重新校验来源与预留，在服务端主线程实际提取。报告实际取得的物品、数量和部分提取结果，RSI 按实际来源与数量记账。 |
| 返还 | 已取得的物品随 RSI 任务账本结算。任务失败或取消时，ProjectExA 按现有规则返还仍可退回的实物，避免同一份补料同时返还 EMC。 |

同一来源的不同物品可能共享额度。例如余额只够转化 10 个同价物品，试分配了 6 个 A 后，同一计划里最多还能分配 4 个 B。材料源在规划和预留时都需要拿到累计分配信息。可把规划确认的外部材料分配按物品标识、来源 ID 和数量附到现有计划快照上，供 EMC 报价直接使用。任务内部的产物仍由任务账本处理。

| 可替换的 Mixin | 由 API 接管的行为 |
| --- | --- |
| `RsiTypedMaterialSourcesMixin` | 在合成规划和执行前校验的材料快照中计入虚拟库存。 |
| `RsiExtractionLedgerAccessor` | RSI 负责读取精妙背包等原生来源并分配实物，扩展可直接获得分配给自身的数量。 |
| `RsiExtractionLedgerMixin` | 管理跨材料的共享预留、实物与 EMC 混合取料，以及提交和返还。 |
| `RsiBeyondDimensionsSessionMixin` | 只装超越维度时也使用相同的合成材料源，现有会话 Mixin 可退役，无需从调用栈推断是不是合成取料。 |

## 2. 协调 JEI 取物和世界选块的客户端输入

RSI 在识别取物动作后、发送请求前调用可注册的客户端处理器。上下文提供触发场景、目标物品、按键和修饰键，以及该场景相关的界面或选块信息。处理器可以放行原动作，也可以接管这次输入。接管后由处理器发送唯一的请求，RSI 跳过自己的发包。多个处理器应有确定的优先顺序，所有处理器都放行时执行 RSI 原流程。

| 可替换的 Mixin | 由 API 接管的行为 |
| --- | --- |
| `RsiJeiMiddleClickMixin` | 协调 JEI 中键取物，ProjectExA 可在满足触发条件时接管。 |
| `RsiWorldPickClientMixin` | 协调世界选块，ProjectExA 可按普通单取、Shift 整组或补齐识别本次意图。 |

## 3. 公开服务端取物操作和结构化结果

提供可在服务端主线程直接调用的原生取物入口，覆盖 JEI 和世界选块两种场景，并支持 RSI 已识别的 RS 与超越维度网络。请求携带玩家、目标物品、数量和交付目标，能表达背包、光标和当前快捷栏等场景允许的目标。世界选块应能表达单取、整组和向同种物品补齐。RSI 侧继续校验网络权限、物品身份及目标槽位和容量，按请求意图给出实际交付结果。

- 返回实际交付数量和明确状态，至少区分成功、部分交付、缺货、无网络、无权限、空间不足和后端失败。若后端失败后无法确认是否已提取或交付，应能区分这种情况与确定交付为零。
- 允许调用方选择是否调用已注册的缺货兜底，并在结果确定后统一决定提示或收藏。确认交付为零且符合自身规则时，ProjectExA 可以用玩家自己的 EMC 补料，例如没有可用网络时。已经部分交付或转移结果不明时按原取物结果处理，避免重复取物。RSI 自己发起的取物仍可使用现有缺货兜底。

现有 `RSIStorageApi` 提供存储访问和精确提取。这里需要的是包含交付校验、交付结果和反馈时机的完整取物操作。当前协同流程里，ProjectExA 的数量和交付选项用于 EMC 兜底，RSI 原生取物成功时仍使用自身的数量和目标规则。 [RSI 存储 API](https://github.com/Elten-huanghuang/rs_integration/blob/00e533253e6a200b63eeefe3ea1660e3ab901480/docs/api/README.md#172-存储访问入口) · [ProjectExA 取物协调](../src/main/java/com/wjx/forge/projectexa/integrations/rsi/RsiItemRetrieval.java)

| 可替换的 Mixin | 由 API 接管的行为 |
| --- | --- |
| `RsiJeiStoragePullInvoker` | 通过公开方法调用服务端 JEI 取物。 |
| `RsiJeiStoragePullMixin` | 读取结构化结果并延后反馈，取代对失败提示及其翻译键的拦截。 |
| `RsiWorldPickInvoker` | 通过公开方法调用服务端世界选块，并按结果决定是否兜底或提示收藏。 |

## 1.6.1 兼容性观察

正式版 JAR 的 `ExtractionLedger` 字节码已无旧 RS 类型引用，可以重新评估只装超越维度时的旧账本开关。世界选块 Invoker 的目标类 `RSSidePanelClickPacket` 仍引用旧 RS 类型，应独立评估。该结论来自静态检查。 [ProjectExA 的 Mixin 开关](../src/main/java/com/wjx/forge/projectexa/mixin/ProjectExaMixinConfigPlugin.java) · [RSI 的取料账本](https://github.com/Elten-huanghuang/rs_integration/blob/00e533253e6a200b63eeefe3ea1660e3ab901480/src/main/java/com/huanghuang/rsintegration/crafting/ExtractionLedger.java) · [RSI 的世界选块入口](https://github.com/Elten-huanghuang/rs_integration/blob/00e533253e6a200b63eeefe3ea1660e3ab901480/src/main/java/com/huanghuang/rsintegration/feature/sidepanel/RSSidePanelClickPacket.java)
