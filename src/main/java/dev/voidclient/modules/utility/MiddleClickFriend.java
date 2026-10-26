package dev.voidclient.modules.utility;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Middle-click a player to mark/unmark as friend (used by ESP/Tracers later). */
public class MiddleClickFriend extends Module {
    private static final Set<UUID> FRIENDS = new HashSet<>();
    private boolean wasDown;

    public MiddleClickFriend() {
        super("MiddleClickFriend", "Middle-click players to add/remove friends.", Category.UTILITY);
    }

    public static boolean isFriend(UUID id) {
        return id != null && FRIENDS.contains(id);
    }

    public static boolean isFriend(Player player) {
        return player != null && isFriend(player.getUUID());
    }

    public static Set<UUID> getFriends() {
        return Collections.unmodifiableSet(FRIENDS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean down = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                mc.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (down && !wasDown) {
            HitResult hit = mc.hitResult;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player target
                    && target != mc.player) {
                UUID id = target.getUUID();
                if (FRIENDS.contains(id)) {
                    FRIENDS.remove(id);
                    VoidClient.LOGGER.info("Removed friend {}", target.getName().getString());
                } else {
                    FRIENDS.add(id);
                    VoidClient.LOGGER.info("Added friend {}", target.getName().getString());
                }
            }
        }
        wasDown = down;
    }
}
