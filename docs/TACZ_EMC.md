# TACZ 枪械 EMC 支持

Project Expansion: Arcanum 让 ProjectE 自动为 TACZ（Timeless and Classics Zero）的
**枪械、子弹、配件**计算 EMC。整套实现只使用 ProjectE 自己的扩展点，因此计算发生在
原版 ProjectE 的 EMC 映射时机（数据包加载 / `/reload` 时的 `TagsUpdatedEvent`），
数值同样由 ProjectE 的图求解器从配方材料推导，而不是写死的表。

## 组成

| 文件 | 作用 |
| --- | --- |
| `integrations/tacz/TaczGunSmithTableMapper` | `@RecipeTypeMapper`：把枪械工作台配方（`tacz:gun_smith_table_crafting`）交给 ProjectE 的 `CraftingMapper`，与原版合成配方走同一条流水线。 |
| `integrations/tacz/TaczNBTProcessor` | `@NBTProcessor`：把枪械/子弹/配件归约成"刚合成出来"的身份 NBT，供学习（知识）、凝聚器和本模组的转化逻辑使用。 |
| `integrations/tacz/TaczEmcHelper` | 生成上述规范形态，镜像 TACZ 的 `RawGunTableResult`。 |
| `mixin/NBTManagerPersistentEMCMixin` | 让"精确 NBT 查不到就回落到裸物品"的查询多一步：先查该物品的持久化 NBT 形态。 |

## 为什么需要规范化 NBT

TACZ 把上百把枪塞进同一个物品（`tacz:modern_kinetic_gun`），真正的身份在 NBT 里；
ProjectE 的 EMC 表以 物品 + NBT 为键。玩家手里的枪 NBT 一直在变（弹药数、开火模式、
枪管余弹、配件、熟练度、皮肤、过热值），直接拿整份 NBT 当键必然查不到。

所以枪械/子弹/配件统一归约为：

- 枪：`{GunId, GunFireMode(索引里的第一个开火模式), GunCurrentAmmoCount:0, HasBulletInBarrel:0b}`
  —— 与枪械工作台产出的物品逐字节一致，因此转化出来的枪和合成出来的枪完全相同。
- 子弹：`{AmmoId}`
- 配件：`{AttachmentId}`（忽略皮肤、倍镜档位、激光颜色）

裸物品（没有任何 NBT 的 `tacz:modern_kinetic_gun`）**不会**获得 EMC，避免所有枪共享一个价格。

## 配置开关

沿用 ProjectE 自己的配置，不额外发明开关：

- `config/ProjectE/mapping.toml` → `[CraftingMapper.TACZGunSmithTable] enabled`
- `config/ProjectE/processing.toml` → `[processors.TACZItemProcessor] enabled / persistent`

本模组只新增一个开关（`config/project-expansion.toml`）：

- `persistentNbtEmcFallback`（默认 `true`）：精确 NBT 无价时，先查持久化 NBT 形态再回落到裸物品。
  关掉它，只有全新未使用的枪才能查到 EMC。

## 已知取舍

- 配方自带的赠品（例如 `tacz:ai_awp` 配方直接附带一个瞄具、或 `ammoCount > 0`）不会被扣除，
  整份材料价都算在裸枪上。结果是裸枪略贵，属于安全方向的偏差，不会产生刷 EMC 的漏洞。
- 弹药箱（`tacz:ammo_box`）不参与身份归约：它的 NBT 里带着数量，归约会丢失信息。它仍然通过
  普通合成配方获得 EMC。
- 枪身上装着的配件、枪里的子弹不会额外加价，卖出时按裸枪计价。

## 验证记录（开发端专用服务器，TACZ 默认枪包）

```text
Found and loaded RecipeType Mapper: TACZGunSmithTable, with priority 1
Found and loaded NBT Processor: TACZItemProcessor, with priority 0
Registered 1766 EMC values.
```

其中 TACZ 条目 180 条：53 把枪、24 种子弹、94 个配件（其余为方块等原版配方产物）。

运行期查询（同一把 `tacz:ump45`）：

```text
全新枪  {GunCurrentAmmoCount:0,GunFireMode:"AUTO",GunId:"tacz:ump45",HasBulletInBarrel:0b}  emc=26880
使用过  {GunCurrentAmmoCount:12,GunDisplayId:...,GunLevelExp:500,HasBulletInBarrel:1b}       emc=26880
子弹    {AmmoId:"tacz:792x57"}                                                               emc=77
配件    {AttachmentId:"tacz:sniper_extended_mag_1"}（带皮肤/激光颜色时同价）                  emc=16896
裸枪物品（无 NBT）                                                                            emc=0
```
