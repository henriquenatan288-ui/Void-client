package dev.voidclient.core.setting;

import dev.voidclient.modules.render.BlockCatalog;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** A comma-separated set of Minecraft block IDs, editable through the block picker screen. */
public final class BlockListSetting extends Setting<String> {
    public BlockListSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue);
    }

    public Set<String> getSelectedIds() {
        Set<String> ids = new LinkedHashSet<>();
        String value = get();
        if (value == null || value.isBlank()) return ids;
        for (String item : value.split(",")) {
            String id = item.trim().toLowerCase();
            if (!id.isEmpty() && BlockCatalog.byId(id) != null) ids.add(id);
        }
        return ids;
    }

    public boolean isSelected(String id) {
        return getSelectedIds().contains(id);
    }

    public void toggle(String id) {
        if (BlockCatalog.byId(id) == null) return;
        Set<String> ids = getSelectedIds();
        if (!ids.add(id)) ids.remove(id);
        setSelectedIds(ids);
    }

    public void setSelectedIds(Iterable<String> selected) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String id : BlockCatalog.idsInCatalogOrder(selected)) ids.add(id);
        set(ids.stream().collect(Collectors.joining(",")));
    }

    @Override
    public Object toConfigValue() {
        return get();
    }

    @Override
    public void fromConfigValue(Object raw) {
        if (!(raw instanceof String value)) return;

        Set<String> parsed = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String id = item.trim().toLowerCase();
            if (BlockCatalog.byId(id) != null) parsed.add(id);
        }

        // Older versions stored presets such as "Ores" here. Ignore those values and
        // preserve the safe default selection instead of clearing the user's ESP.
        if (!parsed.isEmpty() || value.isBlank()) setSelectedIds(parsed);
    }
}
