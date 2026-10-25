package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Forces maximum brightness (gamma).
 *
 * Confiança: MÉDIA
 * - GameOptions.getGamma() retorna SimpleOption<Double> em 1.21.x (Yarn).
 * - O valor máximo vanilla é 1.0. Definir valores > 1.0 funciona na maioria dos casos,
 *   mas pode ser limitado pelo SimpleOption. Se não ficar 100% claro, usaremos mixin no lightmap depois.
 * Verificar: net.minecraft.client.option.GameOptions#getGamma()
 */
public class Fullbright extends Module {

    private Double previousGamma = null;

    public Fullbright() {
        super("Fullbright", "Maximum brightness, removes darkness.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        SimpleOption<Double> gamma = mc.options.getGamma();
        previousGamma = gamma.getValue();
        // 16.0 is commonly used by clients; vanilla UI max is 1.0
        gamma.setValue(16.0);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || previousGamma == null) return;

        mc.options.getGamma().setValue(previousGamma);
        previousGamma = null;
    }
}
