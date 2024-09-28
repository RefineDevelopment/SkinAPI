package xyz.refinedev.api.skin.player.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import lombok.RequiredArgsConstructor;

import org.bukkit.entity.Player;

import xyz.refinedev.api.skin.CachedSkin;
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

@RequiredArgsConstructor
public class CarbonAdapter implements IPlayerAdapter {

    private final SkinAPI skinAPI;

    /**
     * Get the cached skin by the player.
     *
     * @param player {@link Player} Player to get the skin from.
     * @return {@link CachedSkin} CachedSkin
     */
    public CachedSkin getByPlayer(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        if (!profile.hasTextures() || !profile.isComplete()) {
            return SkinAPI.DEFAULT;
        }

        ProfileProperty profileProperty = null;
        for ( ProfileProperty property : profile.getProperties() ) {
            if (property.getName().equals("textures")) {
                profileProperty = property;
                break;
            }
        }

        if (profileProperty == null) {
            return SkinAPI.DEFAULT;
        }

        return new CachedSkin(profile.getName(), profileProperty.getValue(), profileProperty.getSignature());
    }

    /**
     * Register the skin for the player.
     *
     * @param player {@link Player} Player to register the skin for.
     */
    public void registerSkin(Player player) {
        CachedSkin skin = this.getByPlayer(player);
        this.skinAPI.registerSkin(player.getName(), skin);
    }

}
