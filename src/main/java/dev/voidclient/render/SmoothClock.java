package dev.voidclient.render;

import net.minecraft.client.Minecraft;

/**
 * SmoothClock — relógio de animação por TEMPO REAL (nanoTime), independente do tick de 20 Hz.
 *
 * Por que existe: animações que usam (gameTime + partialTick) / 20 só ficam lisas se o partialTick vier certo.
 * Quando ele vem 0 ou congelado (comum em renderizador traduzido no Android), a animação dá "pulos" de 20 Hz e
 * parece teletransporte. Aqui:
 *   - seconds(): tempo contínuo e monotônico (congela com o jogo pausado, nunca anda em degraus de tick);
 *   - partial(): fração do tick atual medida pelo relógio de parede, a partir do instante em que o gameTime mudou;
 *   - pt(enginePt): usa o partialTick da engine SE ele for válido; senão usa o medido aqui.
 *
 * Uso nos extractors (idempotente, pode chamar em vários módulos no mesmo frame):
 *     SmoothClock.frame(mc);
 *     float pt = SmoothClock.pt(partialTick);   // para lerp de posição/rotação de entidades
 *     double t  = SmoothClock.seconds();        // para fases de animação (giro, pulso, brilho)
 *
 * Caminho: src/main/java/dev/voidclient/render/SmoothClock.java
 */
public final class SmoothClock {
    private SmoothClock() {}

    private static long lastFrameNs;
    private static long tickStartNs;
    private static long lastGameTime = Long.MIN_VALUE;
    private static double tickNs = 50_000_000.0;
    private static double seconds;
    private static float partial;

    /** Atualiza o relógio uma vez por frame (chamadas repetidas no mesmo frame não fazem nada). */
    public static void frame(Minecraft mc) {
        long now = System.nanoTime();
        if (lastFrameNs != 0L && now - lastFrameNs < 400_000L) return;     // mesmo frame

        double dt = lastFrameNs == 0L ? 0.0 : Math.min(0.1, (now - lastFrameNs) / 1.0e9);
        lastFrameNs = now;

        boolean paused = false;
        try { paused = mc.isPaused(); } catch (Throwable ignored) { }
        if (!paused) seconds += dt;

        long gt = 0L;
        if (mc.level != null) {
            try { gt = mc.level.getGameTime(); } catch (Throwable ignored) { }
        }
        if (gt != lastGameTime) {
            if (lastGameTime != Long.MIN_VALUE && gt - lastGameTime == 1L && tickStartNs != 0L) {
                double measured = now - tickStartNs;                          // duração real do tick (média móvel)
                if (measured > 20_000_000.0 && measured < 120_000_000.0) tickNs += (measured - tickNs) * 0.1;
            }
            tickStartNs = now;
            lastGameTime = gt;
        }
        double p = (now - tickStartNs) / tickNs;
        partial = (float) Math.max(0.0, Math.min(1.0, p));
    }

    /** Segundos de animação contínuos. */
    public static double seconds() { return seconds; }

    /** Fração do tick medida pelo relógio de parede (0..1). */
    public static float partial() { return partial; }

    /** partialTick da engine se válido, senão o medido. */
    public static float pt(float enginePt) {
        if (enginePt > 1.0e-4f && enginePt <= 1.0001f) return Math.min(1f, enginePt);
        return partial;
    }

    public static double lerp(double prev, double cur, float pt) { return prev + (cur - prev) * pt; }

    /** Interpolação angular (graus) pelo menor caminho. */
    public static float rotLerp(float prev, float cur, float pt) {
        float d = cur - prev;
        while (d > 180f) d -= 360f;
        while (d < -180f) d += 360f;
        return prev + d * pt;
    }

    /** Seguidor exponencial (independente de FPS): útil para ancorar efeitos que não devem "pular". */
    public static final class Follow {
        private double x, y, z;
        private boolean init;

        public void reset() { init = false; }

        /** rate ~ 20..40 = quase colado; menor = mais macio. dt em segundos. */
        public double[] update(double tx, double ty, double tz, double rate, double dt) {
            if (!init) { x = tx; y = ty; z = tz; init = true; }
            double a = 1.0 - Math.exp(-rate * Math.max(0.0, dt));
            x += (tx - x) * a; y += (ty - y) * a; z += (tz - z) * a;
            return new double[]{x, y, z};
        }
    }
}
