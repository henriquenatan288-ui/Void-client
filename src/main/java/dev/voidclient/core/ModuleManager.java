package dev.voidclient.core;

import dev.voidclient.VoidClient;
import dev.voidclient.core.event.EventBus;
import dev.voidclient.core.event.events.KeyEvent;
import dev.voidclient.core.event.events.Render2DEvent;
import dev.voidclient.core.event.events.TickEvent;
import dev.voidclient.gui.ClickGuiModule;
import dev.voidclient.modules.combat.AutoTotem;
import dev.voidclient.modules.combat.SlowHit;
import dev.voidclient.modules.hud.ArrayListHud;
import dev.voidclient.modules.hud.FPS;
import dev.voidclient.modules.hud.Watermark;
import dev.voidclient.modules.misc.DeathCoords;
import dev.voidclient.modules.misc.Derp;
import dev.voidclient.modules.misc.NameProtect;
import dev.voidclient.modules.misc.NoHurtCam;
import dev.voidclient.modules.misc.Panic;
import dev.voidclient.modules.misc.Zoom;
import dev.voidclient.modules.movement.Fly;
import dev.voidclient.modules.movement.FlyBoat;
import dev.voidclient.modules.movement.Jesus;
import dev.voidclient.modules.movement.NoFall;
import dev.voidclient.modules.movement.Speed;
import dev.voidclient.modules.movement.ToggleSprint;
import dev.voidclient.modules.personalization.GuiTheme;
import dev.voidclient.modules.personalization.HudTheme;
import dev.voidclient.modules.render.BlockESP;
import dev.voidclient.modules.render.BreakWaves;
import dev.voidclient.modules.render.Freecam;
import dev.voidclient.modules.render.Freelook;
import dev.voidclient.modules.render.Fullbright;
import dev.voidclient.modules.render.Particles;
import dev.voidclient.modules.render.PlayerESP;
import dev.voidclient.modules.render.Tracers;
import dev.voidclient.modules.utility.AutoEat;
import dev.voidclient.modules.utility.AutoRespawn;
import dev.voidclient.modules.utility.AutoSprint;
import dev.voidclient.modules.utility.ChestStealer;
import dev.voidclient.modules.utility.CoordsCopy;
import dev.voidclient.modules.utility.InvCleaner;
import dev.voidclient.modules.utility.MiddleClickFriend;
import dev.voidclient.modules.utility.Timer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final long FAILURE_LOG_INTERVAL_NS = 10_000_000_000L;
    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Long> lastFailureLogged = new IdentityHashMap<>();

    public ModuleManager(EventBus eventBus) {
        register(new ClickGuiModule());

        // Combat
        register(new AutoTotem());
        register(new SlowHit());

        // Movement
        register(new Fly());
        register(new Jesus());
        register(new FlyBoat());
        register(new Speed());
        register(new NoFall());
        register(new ToggleSprint());

        // Render
        register(new Freecam());
        register(new Freelook());
        register(new BlockESP());
        register(new PlayerESP());
        register(new Tracers());
        register(new Particles());
        register(new BreakWaves());
        register(new Fullbright());

        // Utility
        register(new AutoSprint());
        register(new AutoRespawn());
        register(new AutoEat());
        register(new MiddleClickFriend());
        register(new CoordsCopy());
        register(new ChestStealer());
        register(new InvCleaner());
        register(new Timer());

        // Misc
        register(new Derp());
        register(new NoHurtCam());
        register(new Zoom());
        register(new DeathCoords());
        register(new NameProtect());
        register(new Panic());

        // Personalization
        register(new GuiTheme());
        register(new HudTheme());

        // HUD
        register(new FPS());
        register(new ArrayListHud());
        register(new Watermark());

        eventBus.subscribe(TickEvent.class, this::onTick);
        eventBus.subscribe(Render2DEvent.class, this::onRender2D);
        eventBus.subscribe(KeyEvent.class, this::onKey);
    }

    private void register(Module module) { modules.add(module); }
    public List<Module> getModules() { return modules; }

    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category)
                .sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList());
    }

    public Module getByName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public List<Module> getEnabled() {
        return modules.stream().filter(Module::isEnabled).collect(Collectors.toList());
    }

    private void logFailure(Module module, Exception error) {
        long now = System.nanoTime();
        Long previous = lastFailureLogged.get(module);
        if (previous != null && now - previous < FAILURE_LOG_INTERVAL_NS) return;
        lastFailureLogged.put(module, now);
        VoidClient.LOGGER.error("Module {} failed; repeated errors are throttled for 10 seconds.",
                module.getName(), error);
    }

    private void onTick(TickEvent event) {
        for (Module module : modules) {
            if (!module.isEnabled()) continue;
            try { module.onTick(); }
            catch (Exception error) { logFailure(module, error); }
        }
    }

    private void onRender2D(Render2DEvent event) {
        for (Module module : modules) {
            if (!module.isEnabled()) continue;
            try {
                if (module instanceof FPS fps) fps.render(event.getContext(), event.getTickDelta());
                else if (module instanceof ArrayListHud array) array.render(event.getContext(), event.getTickDelta());
                else if (module instanceof Watermark watermark) watermark.render(event.getContext(), event.getTickDelta());
                else if (module instanceof BlockESP blockEsp) blockEsp.renderHud(event.getContext(), event.getTickDelta());
                else if (module instanceof PlayerESP playerEsp) playerEsp.renderHud(event.getContext(), event.getTickDelta());
                else module.onRender2D(event.getTickDelta());
            } catch (Exception error) { logFailure(module, error); }
        }
    }

    private void onKey(KeyEvent event) {
        if (!event.isPress()) return;
        for (Module module : modules) {
            if (module.getKeybind() != -1 && module.getKeybind() == event.getKey()) module.toggle();
        }
    }
}
