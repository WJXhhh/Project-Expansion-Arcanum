package com.wjx.forge.projectexa.integrations.rsi;

/** Shared by early Mixin selection and the quote path; deliberately has no optional API types. */
public final class RsiStorageCompat {
    public static final String RS = "refinedstorage";
    public static final String BD = "beyonddimensions";
    private static final String RS_STORAGE_CACHE =
            "com/refinedmods/refinedstorage/api/storage/cache/IStorageCache.class";

    private RsiStorageCompat() {}

    public static boolean hasLegacyInventory() {
        ClassLoader[] loaders = {RsiStorageCompat.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()};
        for (ClassLoader loader : loaders) {
            if (loader != null && loader.getResource(RS_STORAGE_CACHE) != null) return true;
        }
        return false;
    }

    public static boolean hasNetworkEmc(String backendId) {
        // With RS present, the existing ledger Mixins supply EMC through inventory for BOTH backends.
        // Without RS, only the BD session Mixin supplies it, through the network snapshot instead.
        return BD.equals(backendId) && !hasLegacyInventory();
    }
}
