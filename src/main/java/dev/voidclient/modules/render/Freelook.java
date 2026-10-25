package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;

/**
 * Third-person freelook style — switches to reverse/third person while active.
 * Full independent yaw without camera mixin is limited; this is a stable version.
 */
public class Freelook extends Module {

    private Perspective previous;

    public Freelook() {
        super("Freelook", "Look around in third person.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        previous = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (previous != null) {
            mc.options.setPerspective(previous);
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        // Keep third person while enabled
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }
}
