package xyz.refinedev.api.skin.player.impl;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.drizzy.api.ItzelHandler;
import me.drizzy.api.profile.GameProfileProvider;
import org.bukkit.entity.Player;

import xyz.refinedev.api.skin.data.CachedSkin;
import xyz.refinedev.api.skin.SkinAPI;
import xyz.refinedev.api.skin.player.IPlayerAdapter;

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

public class LegacyAdapter implements IPlayerAdapter {

    /**
     * Get the cached skin by the player.
     *
     * @param player {@link Player} Player to get the skin from.
     * @return {@link CachedSkin} CachedSkin
     */
    public CachedSkin getByPlayer(Player player) {
        GameProfileProvider gameProfileProvider = ItzelHandler.getInstance().getGameProfileProvider();
        GameProfile gameProfile = gameProfileProvider.getProfile(player);
        if (gameProfile == null || gameProfile.getProperties().isEmpty()) {
            return SkinAPI.DEFAULT;
        }

        Property skin = Iterables.getFirst(gameProfile.getProperties().get("textures"), null);
        if (skin != null) {
            return new CachedSkin(player.getName(), skin.getValue(), skin.getSignature());
        }
        return SkinAPI.DEFAULT;
    }
}
