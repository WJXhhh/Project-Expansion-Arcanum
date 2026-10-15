# 通用虚拟供弹 API（Forge 1.20.1）

不生成背包补货，不自动把任意第三方武器变成兼容武器。模板仍须有有效 EMC、已学习且绑定账户在线；特殊 NBT 仍须被该物品的 ProjectE 身份/EMC 处理正确识别。

## 自己开发的新武器：推荐直接取弹事务

```java
// 接在服务端的射击/装填确认点；先完成冷却、蓄力、取消事件等检查。
Optional<AmmoSources.Selection> selection = AmmoSources.find(
        player, stack -> stack.is(MY_AMMO.get()), 1);
if (selection.isEmpty()) return;
ItemStack prediction = selection.get().simulate(); // 不扣费；可用于可用性检查
ItemStack paidAmmo = selection.get().take();       // 重验并扣费/取走实体弹药
if (paidAmmo.isEmpty()) return;
spawnProjectile(player, weapon, paidAmmo);          // 由武器实现；不要再 shrink 背包弹药
```

`find` 先查实体背包/饰品，再查其中携带的个人 EMC 仓。`findWarehouse` 仅查询虚拟仓。
任何弹药物品和匹配谓词均可使用，不要求继承 `ArrowItem` 或让武器继承 `ProjectileWeaponItem`。
一次选择从一个来源精确提取指定数量，不跨槽拼单；数量不足返回空，不部分扣费。
数量表示实际消耗的弹药数，不是发射物数量；自定义多重射击若只消耗一发，应取一发再由武器生成副弹。
`preview()` 是独立快照；`simulate()` 重验当前可用性；`take()` 只允许服务器主线程且单次使用。
实际提取后的弹药只交还调用方，不塞进玩家背包。`take(true)` 供可信服务端无限弹药规则使用；武器自己负责相应的拾取限制，不能因此生成可回收免费弹药。

## 通过标准取弹流程接入：注册一个武器族适配器

```java
ProjectileAmmoAdapters.register(new ResourceLocation(MOD_ID, "my_weapons"),
        new ProjectileAmmoAdapter() {
            public boolean supports(ItemStack weapon) {
                return weapon.getItem() instanceof MyWeaponItem;
            }
            public boolean accepts(Player player, ItemStack weapon, ItemStack ammo) {
                return ammo.is(MY_AMMO.get());
            }
            // 可按武器自己的无限弹药规则覆写 isInfinite，默认 false。
        });
```

注册应在通用初始化中完成一次；ID 重复会拒绝。自定义适配器优先于内置适配器，后注册者优先；可按 ID 注销。
`LivingGetProjectileEvent` 在原版/其他模组未给出弹药时，通过已注册适配器返回虚拟预览。
原版弓、弩及保持原生消费流程的派生武器有内置适配器；查询端没有 Mixin、没有精确物品 ID 白名单，也没有 `ArrowItem` 限制，使用武器自己的弹药匹配条件。

**注册只负责取弹与费用策略，不会拦截未知武器的消费代码。** 对自己实现了消费逻辑的武器，在确认射击/装填后、复制或消耗弹药前调用：

```java
ItemStack ammo = player.getProjectile(weapon);
if (ammo.isEmpty()) return;
if (!AmmoSources.consumeProjectile(player, weapon, ammo)) return;
// 继续武器原本的 shrink/split、装填/发射流程。
// 这里保留原生实体弹药消费；仓库支付已由 consumeProjectile 完成。
```

必须传原始预览对象，成功支付后才能复制。对其他来源的弹药，`consumeProjectile` 不额外收费，消费仍由武器原流程完成。
非 `ProjectileWeaponItem` 的武器可以直接使用第一种事务 API；若自行调用 Forge 取弹钩子，也可走适配器流程。
未知、未接入消费协议的第三方武器默认不提供虚拟预览，防止直接 `shrink` 预览却不付 EMC。
重写了原生弓弩消费方法的子类，也必须显式接入事务或消费接口，不能假设继承关系能覆盖任意重写。

## 保留的窄适配

弓 `releaseUsing`、弩 `loadProjectile` 的消费时机仍以小范围 Mixin 对接统一 API。
弓遵循 Forge `ArrowItem.isInfinite`，普通箭无限不收费，光灵/药水箭正常收费；弩多重射击只支付中心弹药一次，副弹保持原版不可回收规则。
实体弹药优先；其他模组已经通过事件给出的弹药不会被覆盖。
TACZ 继续使用原来的 `IItemHandler.extractItem` 路径，不走这套预览适配，不修改其换弹、计数或账户实现。
