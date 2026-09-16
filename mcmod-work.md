# MC 百科工作记录

## ProjectExA / Goety

- 模组：诡厄巫法（Goety），MC 百科模组 ID：7635，页面：https://www.mcmod.cn/class/7635.html
- 本地开发依赖：Goety 2.5.57.3（Forge 1.20.1），核对日期：2026-09-16。
- `goety:ominous_shard`：不祥碎片，MC 百科资料 ID：823427，页面：https://www.mcmod.cn/item/823427.html
  - 灾厄教父的魔杖被摧毁后击杀教父固定掉落；可用于合成不祥核心和恶意炸弹。
- `goety:void_shard`：虚空碎片，MC 百科资料 ID：904809，页面：https://www.mcmod.cn/item/904809.html
  - 附魔的末影遣使固定掉落；用于给虚空神龛充能。
- `goety:withered_manuscript`：凋零手稿，MC 百科资料 ID：823179，页面：https://www.mcmod.cn/item/823179.html
  - 凋灵死灵法师必然掉落；用于专家下界仪式制作过往卷轴。
- `goety:shrouded_blueprint`：被隐匿的蓝图，MC 百科资料 ID：823177，页面：https://www.mcmod.cn/item/823177.html
  - 在当前 Goety 2.5.57.3 中由末影守望者击败后获得；用于锻造仪式制作终结卷轴。
- `goety:feet_of_frog`：青蛙脚，MC 百科资料 ID：751589，页面：https://www.mcmod.cn/item/751589.html
  - 青蛙概率掉落，也可通过巫婆交易获得；可烧制为熟青蛙脚，并用于青蛙腿药酿。
- `goety:void_bottle`：液态虚空瓶，MC 百科资料 ID：882154，页面：https://www.mcmod.cn/item/882154.html
  - 在维度最低高度处用玻璃瓶接触虚空获得；官方物品定义返还 `minecraft:glass_bottle`。
  - 当前 Goety 配方中用于虚空方块、末影之眼、末影木树苗、末地泥土等配方。
- `goety:void_bucket`：液态虚空桶，MC 百科资料 ID：882159，页面：https://www.mcmod.cn/item/882159.html
  - 可从装满液态虚空的炼药锅或流体源获取；官方物品定义返还 `minecraft:bucket`。
  - 当前 Goety 配方中用于虚空木桶和虚空闪现焦点仪式。
- `goety:end_mud_bottle`：末地泥浆瓶，MC 百科资料 ID：893694，页面：https://www.mcmod.cn/item/893694.html
  - 对末地泥浆方块使用玻璃瓶获得，方块转为末地土；官方物品定义返还 `minecraft:glass_bottle`。
  - 当前 Goety 的普通配方数据中没有引用该物品。
- `goety:end_mud_bucket`：末地泥浆桶，MC 百科资料 ID：893706，页面：https://www.mcmod.cn/item/893706.html
  - 可从装满末地泥浆的炼药锅或流体源获取；官方物品定义返还 `minecraft:bucket`。
  - 当前 Goety 的普通配方数据中没有引用该物品。
- 涉及容器的 EMC 计算：ProjectE 的基础配方映射会识别合成剩余物，因此使用上述瓶子/桶时分别扣除空玻璃瓶/空桶；Goety 仪式映射也沿用该逻辑，灵魂消耗不计入 EMC。
- 本轮已录入 EMC：液态虚空瓶 `1025`、液态虚空桶 `3840`、末地泥浆瓶 `65`、末地泥浆桶 `960`。
  - 按空玻璃瓶 `1`、空桶 `768` 计算；液态虚空内容物按每瓶 `1024`、末地泥浆内容物按每瓶 `64`，满炼药锅按三瓶内容物折算。
  - 虚空方块、末地泥土、末影木树苗、虚空之眼、虚空木桶等普通配方，以及虚空闪现焦点仪式，均交由 ProjectE 配方映射递归计算，不额外硬编码产物 EMC；仪式灵魂消耗不计入。
- `goety:unholy_blood`：不洁之血，MC 百科资料 ID：751595，页面：https://www.mcmod.cn/item/751595.html
  - 使徒的固定掉落，不受抢夺影响；用于万灵药、死神之镰、凋灵聚晶、贤者之石、击杀焦点等终期配方/仪式。
  - 官方 Goety 2.5.57.3 配方数据中共有 6 个引用：`unholy_fabric` 的至纯血启动物品，以及 `undeath_potion`、`death_scythe`、`wither_skull_focus`、`killing_focus`、`philosophers_stone` 的材料或启动物品。
- `goety:unholy_blood{Pure:1b}`：至纯之血，MC 百科资料 ID：844123，页面：https://www.mcmod.cn/item/844123.html
  - 是不洁之血的 NBT 变体，只由下界使徒掉落，并额外用于不洁布料仪式；录入 EMC 时必须单独注册该 NBT 变体。
- 本轮已录入 EMC：基础不洁之血 `32768`，至纯变体 `goety:unholy_blood{Pure:1b}` 为 `65536`；产物继续交由现有 Goety 仪式映射递归计算，灵魂消耗不计入。
- `goety:snap_fungus`：砰砰菌团（用户称“啪啪菌团”），MC 百科资料 ID：751598，页面：https://www.mcmod.cn/item/751598.html
  - 成长至第三阶段的砰砰疣可掉落，术士也有概率掉落；可投掷造成小范围爆炸和中毒药水云，并可合成为爆裂菌团、狂暴菌团。
  - `snap_warts` 配方消耗 4 个绯红菌、4 个诡异菌和 1 个下界疣，基础 EMC 为 `280`；1 个砰砰菌团可合成 4 个砰砰疣，因此本轮录入 `1120 EMC`，与配方链保持等价。

- `goety:refuse_bottle`：拒绝之瓶，MC 百科资料 ID：751602，页面：https://www.mcmod.cn/item/751602.html
  - 归类为药酿；失败药酿装瓶时生成，也可由女巫交易获得。物品本身设置了 `minecraft:glass_bottle` 作为合成剩余物，饮用后返还空玻璃瓶。
  - 本轮录入 `1 EMC`：按空玻璃瓶价值计算，拒绝内容物不额外计价。
- `goety:soul_jar`：灵魂罐，MC 百科资料 ID：751604，页面：https://www.mcmod.cn/item/751604.html
  - 当前版本由空灵魂罐装入死灵法师类仆从的灵魂获得；灵魂罐的实体、主人名和类型 NBT 不作为 EMC 内容计价。
- `goety:howling_soul`：咆哮之魂，MC 百科资料 ID：823429，页面：https://www.mcmod.cn/item/823429.html
  - 无合成表；拥有主人的暗兽死亡时必定掉落，用于复活暗兽。本轮录入 `8192 EMC`。
- `goety:blazing_helm`：燃烧之盔，MC 百科资料 ID：861920，页面：https://www.mcmod.cn/item/861920.html
  - 无合成表；拥有主人的野火死亡时掉落，用于复活野火。本轮录入 `16384 EMC`。
- 本轮灵魂罐基础值为 `1698 EMC`，等于 6 个阴影石头（6 × 283）；咆哮之魂与燃烧之盔的数值为按复活物品稀有度设定的项目定价。

- 本轮核对了 Goety 的研究卷轴标签 `goety:research_scrolls`：其中 9 个卷轴里，过往卷轴和终结卷轴已有 Goety 仪式配方，剩余 7 个没有可记录的合成表；MC 百科的配方读取结果也均为 0 条。
- 无配方卷轴的获取来源均为结构战利品：劫掠卷轴（林地府邸）、战争卷轴（要塞图书馆或远古城市）、埋葬卷轴（沙漠神殿）、作祟卷轴（墓穴）、先锋卷轴（沙漠神殿或堡垒遗迹）、狂风卷轴（风之神殿顶层）、花纹卷轴（丛林神殿）。对应资料页：
  - https://www.mcmod.cn/item/751606.html
  - https://www.mcmod.cn/item/751607.html
  - https://www.mcmod.cn/item/751608.html
  - https://www.mcmod.cn/item/751609.html
  - https://www.mcmod.cn/item/751610.html
  - https://www.mcmod.cn/item/792828.html
  - https://www.mcmod.cn/item/798921.html
- 本轮已录入无配方卷轴 EMC：`ravaging_scroll` `4096`、`warred_scroll` `8192`、`buried_scroll` `2048`、`haunting_scroll` `8192`、`front_scroll` `4096`、`mistral_scroll` `8192`、`floral_scroll` `2048`。按结构探索难度和研究解锁层级分为 2048/4096/8192 三档；有现成普通配方或仪式配方的卷轴继续由配方映射递归计算。

- 本轮核对了诅咒骑士套装和诅咒圣骑士套装的 8 个部件：当前版本没有可读取的工作台、诅咒注入器或其他普通合成表。诅咒骑士装备由缠魂甲胄装备掉落；诅咒圣骑士装备可通过对骨头领主使用除你铠甲药剂获得，也可能在陶瓷坛中发现。资料页：
  - https://www.mcmod.cn/item/751679.html
  - https://www.mcmod.cn/item/751680.html
  - https://www.mcmod.cn/item/751681.html
  - https://www.mcmod.cn/item/751682.html
  - https://www.mcmod.cn/item/751683.html
  - https://www.mcmod.cn/item/751684.html
  - https://www.mcmod.cn/item/751685.html
  - https://www.mcmod.cn/item/751686.html
- 本轮已录入装备 EMC：诅咒骑士头盔 `8192`、胸甲 `16384`、护腿 `12288`、靴子 `8192`；诅咒圣骑士头盔 `16384`、胸甲 `32768`、护腿 `24576`、靴子 `16384`。按装备部位权重定价，圣骑士套的整体档位约为骑士套的两倍；装备耐久损耗不作为额外 NBT EMC 变体。

## Goety 基础材料审计（2026-09-16，待确认）

- 当前 ProjectE 兼容层已覆盖 Goety 仪式、火盆和带根之图腾的普通工作台配方；灵魂消耗不计入 EMC，配方标签中的多种候选由 ProjectE 取最低值。
- 本轮已录入默认 EMC 的三项：
  - `goety:empty_focus`（空法术聚晶）：`256`。诅咒注入器可由 1 个紫水晶碎片直接转化；低于带根之图腾的普通配方。
  - `goety:savage_tooth`（野蛮之牙）：`1296`。诅咒注入器可由 1 个骨块直接转化；低于 5 个骨块加根之图腾的普通配方。
  - `goety:magic_emerald`（觉醒绿宝石）：`16384`。诅咒注入器可由 1 个原版绿宝石直接转化；低于 4 绿宝石、4 青金石加根之图腾的普通配方。
- `goety:resilience_lotion`、`goety:flying_ointment`、`goety:aging_cream` 是 Goety 自定义坩埚产物，目前没有坩埚配方映射；它们属于后续应单独接入的特殊产物，不建议先用硬编码 EMC 掩盖配方缺口。启用 Goety Revelation 时，前两者存在条件式普通合成替代。
- 已添加 `CursedInfuserRecipeMapper`：接管 Goety 的 `goety:cursed_infuser_recipes` 配方类型。该机器每个配方只有一个输入和一个输出，不消耗作为物品的燃料、催化剂或灵魂，因此按输入总 EMC 守恒映射输出；普通注入器和 Grim Infuser 共用该配方类型，`grim` 配方也一并覆盖。
- 当前 Goety 2.5.57.3 的注入器配方包括：铁锭标签→诅咒金属锭、铁块标签→诅咒金属块、紫水晶标签→空法术聚晶、绿宝石块→觉醒绿宝石块、孢子花→坟墓花，以及 Grim 配方中的黑曜石标签→坟石。输入标签由 ProjectE 的配方映射按最低 EMC 候选处理。
- `goety:void_key` 是战利品/功能钥匙，没有合成来源，不纳入基础材料自动定价；如需让钥匙进入 EMC 系统，应另行指定规则。
- 苍白金属锭、黑暗金属锭、普通布料、诡异富疣卵、浓缩绿宝石、禁书碎片/片段、四种核心、不祥核心、暗夜之心、红苔藓丛和紫颂植株均已有普通、火盆或仪式来源，可由现有映射递归计算，不应重复硬编码。
- 已添加 `CauldronRecipeMapper`：接管 `goety:cauldron`（同时覆盖 Goety 的 `cauldron_sus` 配方类型实例），把 `take_with` 作为消耗材料纳入计算，灵魂消耗仍忽略。

## 雷霆之锤 EMC（2026-09-16）

- `goety:stormlander`（雷霆之锤，MC百科物品资料页：https://www.mcmod.cn/item/792836.html）在当前 Goety 2.5.57.3 数据中没有工作台、仪式或其他可读取的合成配方；黑暗之书将其描述为灾厄村民的宝藏。资料页记录其由闪电强化的粉碎者 2.5% 掉落，或固定生成于不详铁匠铺二楼祭坛基座，并具有落雷和连锁闪电效果，最大耐久为 2500。
- 已录入默认 EMC：`stormlander` `65536`。该值按无配方挑战级特殊武器定为纯不洁之血同档；这是基于获取难度、2500 耐久和雷电能力的定档推断，不是由配方递归计算。其修复材料苍白金属锭仍由已有普通合成配方映射计算。

## 堕落之刃与霜冻之刃 EMC（2026-09-16）

- `goety:fell_blade`（堕落之刃，MC 百科资料 ID：751709，页面：https://www.mcmod.cn/item/751709.html）没有可读取的合成表；它是普通及以上难度生成的缠魂甲胄所装备的武器，攻击力 7、最大耐久 256，有概率施加穿孔，并可用诅咒金属锭修复。
- `goety:frozen_blade`（霜冻之刃，MC 百科资料 ID：751710，页面：https://www.mcmod.cn/item/751710.html）没有可读取的合成表；它是普通及以上难度生成的骨头领主自带武器，也可能在陶瓷坛中获得，攻击力 8、最大耐久 1280，攻击必定施加短暂冰冻，对弱冰冻实体有额外伤害。
- 已录入默认 EMC：堕落之刃 `4096`、霜冻之刃 `8192`。按获取层级、耐久和特殊效果分为两档；两者都没有配方递归依据，因此属于基于装备强度与稀有度的定档推断。

## 贤者之锤 EMC（2026-09-16）

- `goety:philosophers_mace`（贤者之锤，MC 百科资料 ID：751703，页面：https://www.mcmod.cn/item/751703.html）有 1 条专家下界仪式配方：启动物品为贤者之石，材料为黑暗铲、黑暗镐、掘墓者之铲、诡异之镐和 2 个下界合金锭；配方 `soulCost` 为 5。
- 修正 `RitualRecipeMapper`：Goety 仪式完成时会整件消耗启动物品，不能沿用 ProjectE 对普通工作台材料的容器返还处理。此前贤者之石被额外生成 `Damage:1` 的负数材料，而该损耗态没有 EMC，导致贤者之锤配方无法计算。
- 现在启动物品按完整消耗计入，普通仪式材料仍保留容器返还、多候选 fake group 和最低 EMC 选择逻辑；灵魂消耗仍不计入 EMC。不新增贤者之锤默认 EMC。

## 原版龙首与鞘翅 EMC（2026-09-16）

- 已录入 `minecraft:dragon_head`（龙首）默认 EMC：`2048`。
- 已录入 `minecraft:elytra`（鞘翅）默认 EMC：`8192`。
- 两者都没有原版合成来源，按末地船可重复探索获取定价；龙首按稀有收藏品处理，鞘翅因具备飞行能力保留功能溢价。鞘翅的耐久损耗不单独生成 NBT EMC 变体。

## Goety 方块与植物 EMC 审计（2026-09-16，植物批次已录入）

- 资料来源：MC 百科 Goety 物品列表（https://www.mcmod.cn/item/list/7635-1.html）、Goety Wiki（https://goety.wiki.gg/），以及当前工程依赖的 Goety `2.5.57.3` Forge `1.20.1` 数据包、方块注册和掉落表。
- 本轮核对了当前版本的方块物品模型、`goety:end_plants` / `goety:windswept_plants` 标签、普通配方、注入器/仪式配方和方块掉落。旧的 `pregenerated_emc.json` 早于本工程最近几轮默认 EMC 录入，里面显示“缺失”的方块不能直接视为最终缺口。
- 已有或应由配方映射递归计算的植物/方块：四种 Goety 树苗按树苗规则已有 `32`；`chorus_sapling` 有配方；`chorus_blossom_leaves` 已按树叶规则得到 `1`；`sienna_vine` 由 8 个原版藤蔓和 1 个红苔藓丛合成 8 个，计算值为 `11`；`corpse_blossom` 当前源码已录入 `64`。这些不重复硬编码。
- 第一批建议录入的无配方植物方块：
  - `1 EMC`：`sienna_grass`、`tall_sienna_grass`、`sienna_fern`、`large_sienna_fern`、`windswept_dead_bush`；`end_grass_sprout`、`end_grass`、`tall_end_grass`、`chorus_tall_grass`、`chorus_fern_sprout`、`chorus_fern`、`large_chorus_fern`；`chorus_sprout`、`chorus_stalk`、`large_chorus_stalk`。这一档对应原版草、蕨类、枯灌木和低成本可再生植物。
  - `8 EMC`：`chorus_vine`、`chorus_blossom_vines`、`chorus_blossom_vines_pruned`、`end_growth_vines`。这一档对应原版藤蔓/缠绕藤蔓；`end_growth_vines_plant` 只是生长体方块，没有独立物品，不注册。
- 本轮已录入上述 19 个植物方块的默认 EMC：草、蕨、芽/茎类为 `1`，藤蔓类为 `8`；未构建或测试。
- `goety:night_beacon`（暗夜信标，MC 百科资料 ID：751738，页面：https://www.mcmod.cn/item/751738.html）没有普通合成表，当前版本由下界使徒被击败后掉落；方块被破坏时必定掉落自身，并作为腐化聚晶仪式的启动物品。
- 本轮已补录暗夜信标默认 EMC：`32768`，与同为下界使徒掉落的不洁之血处于同一稀有度档位；不把其永夜、常加载区块等功能额外折算为 EMC。
- `henbane`、`deadly_nightshade`、`firethorn` 是作物生长方块，不是正常物品注册；不应给这三个方块 ID 直接注册 EMC。它们掉落的 `henbane_flower`、`nightshade_blossom` 已有 `64`，种子和 `firethorn_berries` 属于后续物品材料审计。
- 第二批应单独定价的世界生成基础方块：`end_rock`、`end_basalt`、`end_soil`。它们没有普通产出配方，却是末地岩石、末地玄武岩、末地土衍生配方的根节点；建议参考原版 `end_stone=1`、`basalt=4`、`dirt=1`，确认后再录入。`end_growth_block`、`end_soil_debris`、末地岩石/玄武岩建筑变体交给配方链递归计算。
- `black_crystal`、`grim_infuser`、`shriek_obelisk`、`resonance_crystal`、`ominous_idol`、`soul_mender` 等有仪式来源；`dark_metal_block` / `dark_anvil` 依赖黑暗金属锭；虚空、墓石、翡翠和建筑系列也有配方或依赖链。它们不在本轮植物默认值中硬编码，先补齐根材料和特殊配方映射，避免遮盖真实 EMC 链。

## Goety 特殊方块补漏审计（2026-09-16，待录入）

- 对当前 Goety 方块物品、普通配方、方块掉落表和方块类行为做了交叉筛选；暗夜信标不是唯一一个未显式注册默认 EMC 的特殊方块。
- 明确进入下一批候选的无普通配方方块：`crystal_ball`、`lofty_chest`、`spider_sac`、`tall_skull`、`redstone_golem_skull`、`grave_golem_skull`、`redstone_monstrosity_head`、`crypt_bookshelf`、`end_lamp`、`jade_ore`。其中水晶球、华丽宝箱、蜘蛛囊、墓穴书架依靠结构生成和精准采集获得；三种傀儡/巨兽头颅和高头骨属于生物掉落；玉矿石属于世界生成矿石；末地灯有自身掉落表。资料来源：Goety 物品列表 https://www.mcmod.cn/item/list/7635-1.html、玉矿石 https://www.mcmod.cn/item/751749.html、墓穴书架 https://www.mcmod.cn/item/751803.html、红石傀儡头骨 https://www.mcmod.cn/item/751831.html、红石巨兽头颅 https://www.mcmod.cn/item/823268.html。
- 末地系的根材料 `end_rock`、`end_basalt`、`end_soil`、`end_mud` 也仍需单独注册；它们没有普通产出配方，却是大量末地装饰方块配方或液体转换的根节点。暂定参考原版 `end_stone=1`、`basalt=4`、`dirt/mud=1`，具体录入与下一批一起处理。末地岩和末地玄武岩的资料页说明它们通过液态虚空与水/熔岩接触生成，而不是自然生成：https://www.mcmod.cn/item/882233.html、https://www.mcmod.cn/item/882280.html。
- `pithos`、`crypt_chest`、`crypt_urn` 是带战利品或方块实体库存的容器；若录入，只计算空容器本体，不能把内部战利品、库存 NBT 或解锁状态计入 EMC。`spider_mother_den` 以及 `chipped_dark_anvil` / `damaged_dark_anvil` 的掉落/状态语义还需单独确认，暂列待确认。
- 不纳入默认 EMC 的假阳性：`magic_thorn`、`glow_light`、`soul_light` 以及 `void_frame`、`void_spawner`、`void_vault`、`void_shrine` 等没有正常生存掉落或属于临时/技术方块；普通木材、树叶、建筑变体则继续交给标签和配方链递归计算。`plushie*` 与赞助雕像属于奖励/装饰物，除非后续决定给所有创意或奖励物品定价，否则不作为基础材料补录。

- 本轮已录入上述明确候选：水晶球 `8192`、华丽宝箱 `64`、蜘蛛囊 `2048`、末地灯 `1920`、高头骨 `256`、红石傀儡头骨 `2048`、墓灵巨像头颅 `2048`、红石巨兽头颅 `4096`、墓穴书架 `736`；玉矿石 `512`；`end_rock` `1`、`end_basalt` `4`、`end_soil` `1`、`end_mud` `1`。
- 容器方块 `pithos`、`crypt_chest`、`crypt_urn` 均按空容器本体录入 `64`，不计战利品、库存 NBT 或解锁状态。开裂/损坏的黑暗铁砧是黑暗铁砧的耐损状态，按当前黑暗铁砧配方链计算出的本体 EMC `198524` 同值录入：`chipped_dark_anvil`、`damaged_dark_anvil` 各 `198524`。
