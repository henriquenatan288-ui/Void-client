package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.core.setting.Setting;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AntVanish extends Module {

    public static final class StaffListSetting extends Setting<String> {
        public StaffListSetting(String name, String description, String defaultValue) {
            super(name, description, defaultValue);
        }

        public Set<String> names() {
            Set<String> out = new LinkedHashSet<>();
            String v = get();
            if (v == null || v.isBlank()) return out;
            for (String s : v.split(",")) {
                String n = clean(s);
                if (!n.isEmpty()) out.add(n);
            }
            return out;
        }

        public boolean add(String nick) {
            String n = clean(nick);
            if (n.isEmpty()) return false;
            Set<String> all = names();
            for (String e : all) if (e.equalsIgnoreCase(n)) return false;
            all.add(n);
            set(String.join(",", all));
            return true;
        }

        public void remove(String nick) {
            Set<String> all = names();
            all.removeIf(e -> e.equalsIgnoreCase(nick));
            set(String.join(",", all));
        }

        private static String clean(String s) {
            return s == null ? "" : s.trim().replaceAll("[^A-Za-z0-9_]", "");
        }

        @Override public Object toConfigValue() { return get(); }

        @Override
        public void fromConfigValue(Object raw) {
            if (raw instanceof String v) {
                Set<String> parsed = new LinkedHashSet<>();
                for (String s : v.split(",")) {
                    String n = clean(s);
                    if (!n.isEmpty()) parsed.add(n);
                }
                set(String.join(",", parsed));
            }
        }
    }

    private final StaffListSetting staff = addSetting(new StaffListSetting("Staff list", "Nicks of staff/admins", ""));
    private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Alert distance (blocks)", 20, 5, 64, 1));
    private final BooleanSetting showList = addSetting(new BooleanSetting("Show list", "Online staff on HUD", true));
    private final BooleanSetting chatAlert = addSetting(new BooleanSetting("Chat alert", "Message in chat when near", true));
    private final BooleanSetting banner = addSetting(new BooleanSetting("Banner", "Big warning at the top", true));
    private final NumberSetting hudX = addSetting(new NumberSetting("X", "HUD x", 4, 0, 2000, 1));
    private final NumberSetting hudY = addSetting(new NumberSetting("Y", "HUD y", 60, 0, 2000, 1));

    private static final class Entry {
        String nick;
        boolean inTab, present, near, spectator, looking;
        double dist = -1;
        long lastAlert = -10_000;
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private int tick;

    public AntVanish() {
        super("AntVanish", "Alerts when listed staff are online or near you.", Category.UTILITY);
    }

    @Override
    protected void onDisable() { entries.clear(); }

    public static List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return out;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            out.add(info.getProfile().name());
        }
        return out;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { entries.clear(); return; }
        if (++tick % 4 != 0) return;

        Set<String> tab = new LinkedHashSet<>();
        for (String n : onlineNames()) tab.add(n.toLowerCase(Locale.ROOT));
        Map<String, AbstractClientPlayer> world = new HashMap<>();
        for (AbstractClientPlayer p : mc.level.players()) {
            world.put(p.getName().getString().toLowerCase(Locale.ROOT), p);
        }

        Set<String> wanted = new LinkedHashSet<>();
        double r = radius.get(), r2 = r * r;
        long now = mc.level.getGameTime();

        for (String nick : staff.names()) {
            String key = nick.toLowerCase(Locale.ROOT);
            wanted.add(key);
            Entry e = entries.computeIfAbsent(key, k -> new Entry());
            e.nick = nick;
            e.inTab = tab.contains(key);
            AbstractClientPlayer p = world.get(key);
            e.present = p != null && p != mc.player;
            boolean wasNear = e.near;
            if (e.present) {
                double d2 = mc.player.distanceToSqr(p);
                e.dist = Math.sqrt(d2);
                e.near = d2 <= r2;
                e.spectator = p.isSpectator();
                Vec3 look = p.getViewVector(1.0f);
                Vec3 to = mc.player.position().add(0, mc.player.getEyeHeight(), 0)
                        .subtract(p.position().add(0, p.getEyeHeight(), 0));
                double len = to.length();
                e.looking = len > 0.001 && look.dot(to.scale(1.0 / len)) > 0.96;
            } else {
                e.near = false; e.spectator = false; e.looking = false; e.dist = -1;
            }
            if (chatAlert.get() && e.near && (!wasNear || now - e.lastAlert > 600)) {
                e.lastAlert = now;
                String extra = (e.looking ? " \u00a7e[olhando para voce]" : "")
                        + (e.spectator ? " \u00a7d[spectator]" : "")
                        + (!e.inTab ? " \u00a76[fora do TAB]" : "");
                mc.player.sendSystemMessage(Component.literal(
                        "\u00a7c[AntVanish] \u00a7f" + e.nick + " \u00a77a " + Math.round(e.dist) + "m" + extra));
            }
        }
        entries.keySet().retainAll(wanted);
    }

    public void renderHud(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || ctx == null || mc.level == null) return;

        List<Entry> online = new ArrayList<>();
        List<Entry> near = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (e.inTab || e.present) online.add(e);
            if (e.near) near.add(e);
        }

        if (showList.get()) {
            int x = hudX.getInt(), y = hudY.getInt();
            List<String> lines = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            lines.add("STAFF ONLINE (" + online.size() + ")");
            colors.add(0xFFFFFFFF);
            if (staff.names().isEmpty()) { lines.add("(lista vazia - configure)"); colors.add(0xFF888888); }
            else if (online.isEmpty()) { lines.add("nenhum online"); colors.add(0xFF55FF55); }
            for (Entry e : online) {
                String t = e.nick;
                if (e.present) t += " " + Math.round(e.dist) + "m";
                if (e.spectator) t += " [SPEC]";
                if (e.present && !e.inTab) t += " [FORA DO TAB]";
                lines.add(t);
                colors.add(e.near ? 0xFFFF5555 : 0xFFFFD866);
            }
            int w = 0;
            for (String s : lines) w = Math.max(w, RenderUtils.textWidth(mc.font, s));
            ctx.fill(x - 4, y - 4, x + w + 6, y + lines.size() * 11 + 2, 0x99000000);
            ctx.fill(x - 4, y - 4, x - 2, y + lines.size() * 11 + 2, near.isEmpty() ? 0xFF7B2CFF : 0xFFFF3030);
            for (int i = 0; i < lines.size(); i++) {
                ctx.text(mc.font, lines.get(i), x, y + i * 11, colors.get(i), true);
            }
        }

        if (banner.get() && !near.isEmpty()) {
            Entry e = near.get(0);
            String msg = "STAFF PERTO: " + e.nick + " (" + Math.round(e.dist) + "m)"
                    + (e.looking ? "  OBSERVANDO VOCE" : "") + (near.size() > 1 ? "  +" + (near.size() - 1) : "");
            int sw = mc.getWindow().getGuiScaledWidth();
            int tw = RenderUtils.textWidth(mc.font, msg);
            int bx = (sw - tw) / 2, by = 18;
            double pulse = 0.5 + 0.5 * Math.sin(System.nanoTime() / 1.5e8);
            int a = (int) (0x70 + 0x60 * pulse);
            ctx.fill(bx - 10, by - 5, bx + tw + 10, by + 15, (a << 24) | 0xB01010);
            ctx.fill(bx - 10, by - 5, bx + tw + 10, by - 4, 0xFFFFFFFF);
            ctx.text(mc.font, msg, bx, by + 2, 0xFFFFFFFF, true);
        }
    }

    public static final class StaffListScreen extends Screen {
        private static final int W = 320, ROW = 18, BAR_W = 14;
        private final Screen parent;
        private final StaffListSetting setting;
        private EditBox box;
        private int scroll;
        private boolean pressing, moved, draggingBar;
        private double pressX, pressY;
        private int scrollAtPress;

        public StaffListScreen(Screen parent, StaffListSetting setting) {
            super(Component.literal("Staff list"));
            this.parent = parent;
            this.setting = setting;
        }

        private int px() { return (width - Math.min(W, width - 12)) / 2; }
        private int pw() { return Math.min(W, width - 12); }
        private int py() { return 6; }
        private int ph() { return height - 12; }
        private int listY() { return py() + 58; }
        private int listH() { return ph() - 58 - 34; }

        private record Row(int type, String text) {}

        private List<Row> rows() {
            List<Row> r = new ArrayList<>();
            r.add(new Row(0, "STAFF LIST - toque para remover"));
            for (String n : setting.names()) r.add(new Row(1, n));
            if (setting.names().isEmpty()) r.add(new Row(0, "(vazia)"));
            r.add(new Row(0, "ONLINE AGORA (TAB) - toque para adicionar"));
            Set<String> have = new LinkedHashSet<>();
            for (String n : setting.names()) have.add(n.toLowerCase(Locale.ROOT));
            for (String n : onlineNames()) if (!have.contains(n.toLowerCase(Locale.ROOT))) r.add(new Row(2, n));
            return r;
        }

        private int maxScroll(int n) { return Math.max(0, n * ROW - listH()); }
        private void clamp(int n) { scroll = Math.max(0, Math.min(scroll, maxScroll(n))); }

        @Override
        protected void init() {
            box = new EditBox(font, px() + 10, py() + 28, pw() - 90, 18, Component.literal("Nick"));
            box.setMaxLength(16);
            box.setHint(Component.literal("nick do staff"));
            addRenderableWidget(box);
            setFocused(box);
        }

        private void addTyped() {
            if (box == null) return;
            if (setting.add(box.getValue())) box.setValue("");
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
            List<Row> rows = rows();
            clamp(rows.size());
            ctx.fill(0, 0, width, height, 0xD2000000);
            RenderUtils.drawPanel(ctx, px(), py(), pw(), ph(), 8, 0xFF000000 | (Theme.BG_PANEL() & 0xFFFFFF));
            RenderUtils.drawText(ctx, font, "ANTIVANISH - STAFF", px() + 12, py() + 10, Theme.TEXT_PRIMARY(), false);

            int addX = px() + pw() - 74;
            boolean hov = RenderUtils.hovered(mx, my, addX, py() + 28, 64, 18);
            RenderUtils.fillRounded(ctx, addX, py() + 28, 64, 18, 4, hov ? Theme.BG_SELECTED() : Theme.BG_CARD());
            RenderUtils.drawText(ctx, font, "ADD", addX + 22, py() + 33, Theme.TEXT_PRIMARY(), false);

            int lw = pw() - 20 - BAR_W - 4;
            RenderUtils.fillRounded(ctx, px() + 10, listY() - 2, lw + BAR_W + 4, listH() + 4, 4, Theme.BG_CARD());
            for (int i = 0; i < rows.size(); i++) {
                int ry = listY() + i * ROW - scroll;
                if (ry < listY() - 1 || ry + ROW > listY() + listH() + 1) continue;
                Row r = rows.get(i);
                if (r.type() == 0) {
                    RenderUtils.drawText(ctx, font, r.text(), px() + 14, ry + 5, Theme.TEXT_MUTED(), false);
                } else {
                    if (RenderUtils.hovered(mx, my, px() + 12, ry, lw, ROW - 1)) {
                        RenderUtils.fillRounded(ctx, px() + 12, ry, lw, ROW - 1, 3, Theme.BG_CARD_HOVER());
                    }
                    RenderUtils.drawText(ctx, font, r.text(), px() + 18, ry + 5, Theme.TEXT_PRIMARY(), false);
                    String tag = r.type() == 1 ? "remover" : "+ adicionar";
                    RenderUtils.drawText(ctx, font, tag, px() + 12 + lw - RenderUtils.textWidth(font, tag) - 6, ry + 5,
                            r.type() == 1 ? 0xFFFF6666 : 0xFF66FF99, false);
                }
            }

            int bx = px() + pw() - 10 - BAR_W;
            RenderUtils.fillRounded(ctx, bx, listY(), BAR_W - 2, listH(), 3, 0x66000000);
            int max = maxScroll(rows.size());
            if (max > 0) {
                int th = Math.max(20, (int) (listH() * (listH() / (float) Math.max(1, rows.size() * ROW))));
                int ty = listY() + (int) ((listH() - th) * (scroll / (float) max));
                RenderUtils.fillRounded(ctx, bx + 1, ty, BAR_W - 4, th, 3, draggingBar ? 0xFFFFFFFF : Theme.ACCENT());
            }

            int dx = px() + pw() - 74, dy = py() + ph() - 26;
            boolean hd = RenderUtils.hovered(mx, my, dx, dy, 64, 18);
            RenderUtils.fillRounded(ctx, dx, dy, 64, 18, 4, hd ? Theme.BG_SELECTED() : Theme.BG_CARD());
            RenderUtils.drawText(ctx, font, "DONE", dx + 20, dy + 5, Theme.TEXT_PRIMARY(), false);
            super.extractRenderState(ctx, mx, my, delta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() != 0) return super.mouseClicked(click, doubled);
            double mx = click.x(), my = click.y();
            pressX = mx; pressY = my; moved = false; scrollAtPress = scroll; pressing = false; draggingBar = false;
            if (RenderUtils.hovered((int) mx, (int) my, px() + pw() - 74, py() + 28, 64, 18)) { addTyped(); return true; }
            if (RenderUtils.hovered((int) mx, (int) my, px() + pw() - 74, py() + ph() - 26, 64, 18)) { onClose(); return true; }
            int bx = px() + pw() - 10 - BAR_W;
            if (mx >= bx && mx <= bx + BAR_W && my >= listY() && my <= listY() + listH()) { draggingBar = true; barScroll(my); return true; }
            if (my >= listY() && my <= listY() + listH() && mx >= px() + 10 && mx <= px() + pw() - 10 - BAR_W) {
                pressing = true; return true;
            }
            return super.mouseClicked(click, doubled);
        }

        private void barScroll(double my) {
            int n = rows().size(), max = maxScroll(n);
            if (max <= 0) { scroll = 0; return; }
            double t = (my - listY()) / (double) listH();
            scroll = (int) Math.round(Math.max(0, Math.min(1, t)) * max);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
            if (draggingBar) { barScroll(click.y()); return true; }
            if (pressing) {
                if (Math.abs(click.y() - pressY) > 4 || Math.abs(click.x() - pressX) > 4) moved = true;
                if (moved) { scroll = scrollAtPress + (int) (pressY - click.y()); clamp(rows().size()); }
                return true;
            }
            return super.mouseDragged(click, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent click) {
            boolean tap = pressing && !moved && click.button() == 0;
            pressing = false; draggingBar = false;
            if (tap) {
                List<Row> rows = rows();
                int i = (int) ((click.y() - listY() + scroll) / ROW);
                if (i >= 0 && i < rows.size()) {
                    Row r = rows.get(i);
                    if (r.type() == 1) setting.remove(r.text());
                    else if (r.type() == 2) setting.add(r.text());
                }
            }
            moved = false;
            return super.mouseReleased(click);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
            scroll -= (int) (v * ROW * 2);
            clamp(rows().size());
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            int k = input.key();
            if (k == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) { addTyped(); return true; }
            return super.keyPressed(input);
        }

        @Override
        public void onClose() { if (minecraft != null) minecraft.gui.setScreen(parent); }

        @Override
        public boolean isPauseScreen() { return false; }
    }
}
