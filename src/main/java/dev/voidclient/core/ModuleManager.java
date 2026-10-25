package dev.voidclient.core;

import dev.voidclient.core.event.EventBus;
import dev.voidclient.core.event.events.KeyEvent;
import dev.voidclient.core.event.events.Render2DEvent;
import dev.voidclient.core.event.events.TickEvent;
import dev.voidclient.gui.ClickGuiModule;
import dev.voidclient.modules.combat.AutoTotem;
import dev.voidclient.modules.combat.SlowHit;
import dev.voidclient.modules.hud.FPS;
import dev.voidclient.modules.movement.Fly;
import dev.voidclient.modules.movement.FlyBoat;
import dev.voidclient.modules.movement.Jesus;
import dev.voidclient.modules.movement.ToggleSprint;
import dev.voidclient.modules.render.BlockESP;
import dev.voidclient.modules.render.BreakWaves;
import dev.voidclient.modules.render.Freecam;
import dev.voidclient.modules.render.Freelook;
import dev.voidclient.modules.render.Fullbright;
import dev.voidclient.modules.render.Particles;
import dev.voidclient.modules.utility.Welcome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds and manages all modules.
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(EventBus eventBus) {
        // GUI
        register(new ClickGuiModule());

        // Combat
        register(new AutoTotem());
        register(new SlowHit());

        // Movement
        register(new Fly());
        register(new Jesus());
        register(new FlyBoat());
        register(new ToggleSprint());

        // Render
        register(new Freecam());
        register(new Freelook());
        register(new BlockESP());
        register(new Particles());
        register(new BreakWaves());
        register(new Fullbright());

        // Utility
        register(new Welcome());

        // HUD
        register(new FPS());

        eventBus.subscribe(TickEvent.class, this::onTick);
        eventBus.subscribe(Render2DEvent.class, this::onRender2D);
        eventBus.subscribe(KeyEvent.class, this::onKey);
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
    }

    public Module getByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getEnabled() {
        return modules.stream().filter(Module::isEnabled).collect(Collectors.toList());
    }

    private void onTick(TickEvent event) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                try {
                    module.onTick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onRender2D(Render2DEvent event) {
        for (Module module : modules) {
            if (!module.isEnabled()) continue;
            try {
                if (module instanceof FPS fps) {
                    fps.render(event.getContext(), event.getTickDelta());
                } else if (module instanceof BlockESP blockEsp) {
                    blockEsp.renderHud(event.getContext(), event.getTickDelta());
                } else {
                    module.onRender2D(event.getTickDelta());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onKey(KeyEvent event) {
        if (!event.isPress()) return;

        for (Module module : modules) {
            if (module.getKeybind() != -1 && module.getKeybind() == event.getKey()) {
                module.toggle();
            }
        }
    }
}
