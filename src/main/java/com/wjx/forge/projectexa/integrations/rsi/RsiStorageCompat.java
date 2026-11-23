package com.wjx.forge.projectexa.integrations.rsi;

/** Checks whether Refined Storage's optional API is present. */
public final class RsiStorageCompat {
    public static final String RS = "refinedstorage";
    private static final String RS_STORAGE_CACHE =
            "com/refinedmods/refinedstorage/api/storage/cache/IStorageCache.class";

    private RsiStorageCompat() {}

    public static boolean hasRefinedStorageApi() {
        ClassLoader[] loaders = {RsiStorageCompat.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()};
        for (ClassLoader loader : loaders) {
            if (loader != null && loader.getResource(RS_STORAGE_CACHE) != null) return true;
        }
        return false;
    }
}
