package xyz.refinedev.api.skin.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.refinedev.api.skin.SkinAPI;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author Drizzy
 * @version SkinAPI
 * @since 9/28/2024
 */

@RequiredArgsConstructor
public class SkinListener implements Listener {

    private final SkinAPI api;

    @EventHandler
    public void onLoginEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        api.registerSkin(player);
    }
}

