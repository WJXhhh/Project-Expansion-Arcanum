package cool.furry.mc.neoforge.projectexpansion.util;

import moze_intel.projecte.utils.text.ILangEntry;

public enum SearchType implements ILangEntry {
    NORMAL(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE_NORMAL, false, false),
    AUTOSELECTED(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE_AUTOSELECTED, true, false),
    NORMAL_JEI(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE_NORMAL_JEI, false, true),
    AUTOSELECTED_JEI(Lang.GUI.ARCANE_TRANSMUTATION_TABLET_SEARCH_TYPE_AUTOSELECTED_JEI, true, true),
    ;

    public static final SearchType[] VALUES = values();

    public final ILangEntry translation;
    public final boolean autoSelected, jeiSync;
    SearchType(ILangEntry translation, boolean autoSelected, boolean jeiSync) {
        this.translation = translation;
        this.autoSelected = autoSelected;
        this.jeiSync = jeiSync;
    }

    @Override
    public String getTranslationKey() {
        return translation.getTranslationKey();
    }

    public SearchType next() {
        return SearchType.values()[(ordinal() + 1) % VALUES.length];
    }
}
