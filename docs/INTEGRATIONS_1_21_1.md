# 1.21.1 可选联动接口核对与迁移

核对日期：2026-10-22。基线：`cf1a123`，保留此前石桌、仓库及奥术终端修正。

## 对照的实际发行包

| 模组 | 发行版本 | 官方发布文件 | SHA-256 |
|---|---|---|---|
| Ars Nouveau | 1.21.1-5.13.3 | [9052154](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau/files/9052154) | `45c1b6022d8e63464177c1f822bda22c3d6fb66970857fd7677e4e3ccffd259f` |
| Create | 1.21.1-6.0.10 | [7963363](https://www.curseforge.com/minecraft/mc-mods/create/files/7963363) | `ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a` |
| Goety | 1.21.1-3.2.1 | [9207885](https://www.curseforge.com/minecraft/mc-mods/goety/files/9207885) | `71026c15919792ac9e7a89cedbbfc408386e62f138857c914818ba338c5cc153` |

以实际 JAR 的 `javap` 方法签名及必要的反编译结果为准，不只看发布页游戏版本标签。

## 确认的接口变化

- Ars 的 `EnchantingApparatusRecipe`、`EnchantmentRecipe`、`ArmorUpgradeRecipe`、`BuddingConversionRecipe`、`ScryRitualRecipe` 已移到 `common.crafting.recipes`。
- Ars 配方改用 `reagent()`、`pedestalItems()`、`CrushRecipe.input()` 等访问器。JEI 收到的是 `RecipeHolder<T>`。
- Ars JEI 类别不再使用旧版的 `glyph_recipe`、`enchantment_apparatus`：本版实际标识为 `glyph`、`enchantment`。
- Create 的 `ProcessingRecipe` 有两个泛型参数；序列组装仍提供 `getLoops()`、`getSequence()` 和 `IAssemblyRecipe.addAssemblyIngredients()`。
- Create 的 JEI 配方使用 `RecipeHolder`。自动盆地配方保留原版配方 ID；砂纸部署变体添加 `_using_deployer` 后缀。药水混合、容器处理、原木去皮及两种神秘转化含有 RecipeManager 以外生成的展示配方，必须分别验证，不能信任客户端材料列表。
- Goety 仍向 JEI 传递带 `getId()` 的原始配方对象。`WitchBrewJeiRecipe` 是单独的展示对象；其材料由服务端酿造表或内建催化物表确认。
- Goety 坩埚的 `getTakeWith()` 以及仪式的 `getActivationItem()` 必须计入材料；附魔／升级类 Ars 配方的动态中心装备则不能当成固定材料生成。
- ProjectE PE1.1.0 不再提供旧的 `CustomEMCRegistration` / `IMCMethods.REGISTER_CUSTOM_EMC`。92 条 Goety 基础值改用 `@EMCMapper` 及 `setValueBefore`。
- ProjectE 配方映射器接收 `RecipeHolder<?>`；收集器参数为 fastutil `Object2IntMap`，假分组结果使用 `dummy()` / `created()`。所有映射器还必须实现 `getTranslationKey()`。
- Goety 的纯净不洁之血以 `DataComponents.CUSTOM_DATA` 内的 `Pure` 标记识别。为其新增持久组件处理器，避免学习后丢失纯净身份；不保留任意其他自定义数据。

## 已接入的行为

- Ars：符文、粉碎、魔源灌注、灌注装置、附魔、盔甲升级、萌芽转化及探知仪式的缺料补齐。
- Create：已登记处理／机械合成配方；序列组装累计全部循环的物品材料；自动盆地、药水混合、砂纸部署、去皮、容器处理及神秘转化的单独来源验证。
- Goety：仪式及其激活物，火盆、诅咒注入、坩埚与取出材料、粉碎／吸魂类登记配方、酿造催化材料；基础 EMC、仪式／图腾特殊消耗映射及纯净血组件。
- JEI：保留原生 transfer handler 的优先权，无原生 handler 时按实际类别安装补料入口；保留绿／黄／红的材料状态提示。
- 服务端：检查模组是否加载、终端持有状态、配方及类别；优先保留现有实体材料，只为欠缺材料支付 EMC 并生成物品。一次请求最多处理 4096 个物品材料。

**这不是直接合成产物的功能。** 世界装置、灵魂、魔源、流体、热量、时间及仪式环境等条件仍需原模组正常满足。展示配方无法从已核对的规则恢复时拒绝请求，不采纳客户端提供的配料表。

## 环境与可选性

- Create 6.0.10 的包内元数据要求 NeoForge **21.1.219 或以上**，工程编译目标与最低版本一并提升。
- Curios 更新为 **9.5.1+1.21.1**，满足 Goety 3.2.1 声明的 Curios 下限。
- 三个联动模组均为 `compileOnly`，且在本模组元数据中声明为可选，不默认加入开发运行时，也不打包到本模组 JAR。安装时仍须满足各自发行要求。
- 本轮没有新增 EMI 专门的跨模组补料入口；此前 EMI 奥术终端填充保持原状。
- Botania、TACZ、RSI 不在本轮改动范围内。

## 发行包检查发现

- Ars 5.13.3 的 `META-INF/LICENSE.txt` 有 ZIP CRC 不一致；全部 `.class` 条目可正常读取。下载文件 SHA-1 与作者 Modrinth 发布记录一致：`541b3286d187deeb9a1297a509d0d3539d8af0f6`。未修改或重新打包第三方 JAR。
- Goety 3.2.1 的 `META-INF/neoforge.mods.toml` 保留了 `${loader_version_range}`、`${neo_version_range}`、`${minecraft_version_range}` 占位符。这是观察到的包内内容，不据此臆断启动结果；正式运行前必须确认所用发行包和加载器处理结果。

## 验证边界

执行接口核对、Java 编译、资源处理、JAR 内容检查，以及独立源码副本上的回滚／补丁重建。未启动 Minecraft 客户端或服务端，未进行模组组合运行矩阵或旧存档迁移；不能把这些静态及构建结果当成游戏内验收。
