package xyz.refinedev.api.skin.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.refinedev.api.skin.SkinAPI;

/**
 * <p>
 * This code is the property of Refine Development.<br>
 * Copyright © 2025, All Rights Reserved.<br>
 * </p>
 *
 * @author Drizzy
 * @version SkinAPI
 * @since 7/5/2025
 */

@RequiredArgsConstructor
public class PlayerListener implements Listener {

    private final SkinAPI api;

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.api.clear(event.getPlayer());
    }

}
