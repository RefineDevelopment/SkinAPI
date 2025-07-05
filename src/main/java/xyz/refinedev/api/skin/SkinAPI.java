package xyz.refinedev.api.skin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.refinedev.api.skin.data.CachedSkin;
import xyz.refinedev.api.skin.player.IPlayerAdapter;
import xyz.refinedev.api.skin.player.impl.CarbonAdapter;
import xyz.refinedev.api.skin.player.impl.LegacyAdapter;
import xyz.refinedev.api.skin.player.impl.ModernAdapter;
import xyz.refinedev.api.storage.json.JsonStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author Drizzy
 * @version Bolt
 * @since 9/9/2024
 */
public class SkinAPI {

    private static final Type TYPE = new TypeToken<Collection<CachedSkin>>(){}.getType();
    public static final CachedSkin DEFAULT = new CachedSkin("Default", "", "");
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/user/%s";

    private final Map<String, CompletableFuture<CachedSkin>> skinFutures = new ConcurrentHashMap<>();

    /**
     * Custom JSON Storage for Skins
     */
    private final JsonStorage<Collection<CachedSkin>> skinStorage;

    /**
     * This list stores our cached skins, so we don't fetch them all the time.
     */
    @Getter private final Map<String, CachedSkin> skinCache = new ConcurrentHashMap<>();

    /**
     * Temporary cache for skins that are fetched on login and then removed.
     */
    @Getter private final Map<String, CachedSkin> temporaryCache = new ConcurrentHashMap<>();

    /**
     * Player adapter for fetching skins
     */
    private final IPlayerAdapter playerAdapter;

    public SkinAPI(JavaPlugin plugin, Gson gson) {
        this.skinStorage = new JsonStorage<>("skins", plugin, gson);
        this.initiateCacheFromStorage();

        if (MINOR_VERSION >= 13) {
            this.playerAdapter = new ModernAdapter();
        } else if (SUPPORTS_CARBON) {
            this.playerAdapter = new CarbonAdapter();
        } else {
            this.playerAdapter = new LegacyAdapter();
        }
    }

    public void unload() {
        this.skinStorage.save(this.skinCache.values());
        this.skinCache.clear();
    }

    /**
     * Set up the storages from our cache
     */
    public void initiateCacheFromStorage() {
        Collection<CachedSkin> skinList = this.skinStorage.getData(TYPE);
        if (skinList == null || skinList.isEmpty()) return;

        for ( CachedSkin skin : skinList ) {
            this.skinCache.put(skin.getName(), skin);
        }
    }

    /**
     * Get the cached skin by the player.
     *
     * @param player {@link Player} Player to get the skin from.
     * @return {@link CachedSkin} CachedSkin
     */
    public CachedSkin getByPlayer(Player player) {
        CachedSkin skin = this.getSkin(player.getName());
        if (skin != null) {
            return skin;
        }

        // If the skin is not cached, we will fetch it from the player adapter
        skin = this.playerAdapter.getByPlayer(player);
        if (skin == null) {
            return DEFAULT;
        }

        // Register the skin to the cache
        this.temporaryCache.put(player.getName(), skin);
        return skin;
    }

    public void fetchSkin(String name, Consumer<CachedSkin> callback) {
        // Register skin only when it's demanded
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) {
            CachedSkin skin = this.getByPlayer(player);
            callback.accept(skin);
            return;
        }

        // Avoid race conditions and fetching the same skin multiple times
        if (this.skinFutures.containsKey(name)) {
            CompletableFuture<CachedSkin> skinFuture = this.skinFutures.get(name);
            skinFuture.whenComplete((skin, throwable) -> {
                callback.accept(skin);
            });
            return;
        }

        CompletableFuture<CachedSkin> skinFuture = CompletableFuture.supplyAsync(() -> fetchSkin(name));
        skinFuture.whenComplete((skin, throwable) -> {
            if (skin == null) {
                callback.accept(DEFAULT);
                return;
            }

            this.registerSkin(name, skin);
            callback.accept(skin);
            this.skinFutures.remove(name);
        });
        this.skinFutures.put(name, skinFuture);
    }

    /**
     * Fetches the given player's skin from session server
     *
     * @param name  {@link String name}
     * @return      {@link String[] skin}
     */
    public CachedSkin fetchSkin(String name) {
        try {
            URL url = new URL(String.format(ASHCON_URL, name));
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != 200) {
                return null;
            } else {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();

                    String inputLine;
                    while((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }

                    JsonElement element = JsonParser.parseString(sb.toString());
                    if (!element.isJsonObject()) return DEFAULT;

                    JsonObject object = element.getAsJsonObject();
                    JsonObject textures = object.get("textures").getAsJsonObject();
                    JsonObject raw = textures.get("raw").getAsJsonObject();
                    String value = raw.get("value").getAsString();
                    String signature = raw.get("signature").getAsString();

                    return new CachedSkin(name, value, signature);
                }
            }
        } catch (NullPointerException | IOException ex) {
            return DEFAULT;
        }
    }

    /**
     * Register a skin to the cache.
     *
     * @param name {@link String} Skin name
     * @param skin {@link CachedSkin} Skin to cache
     */
    public void registerSkin(String name, CachedSkin skin) {
        this.skinCache.put(name, skin);
        this.skinStorage.saveAsync(this.skinCache.values());
    }

    /**
     * Get a cached skin associated with this specific name.
     *
     * @param name {@link String} Name
     * @return     {@link CachedSkin} Skin
     */
    public CachedSkin getSkin(String name) {
        return this.temporaryCache.getOrDefault(name, this.skinCache.getOrDefault(name, null));
    }

    public void clear(Player player) {
        this.temporaryCache.remove(player.getName());
    }

    public static String VERSION;
    public static int MINOR_VERSION;
    public static boolean SUPPORTS_CARBON;

    static {
        try {
            String versionName = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = versionName.length() < 4 ? versionName.split("\\.")[2] : versionName.split("\\.")[3];
            MINOR_VERSION = Integer.parseInt(VERSION.split("_")[1]);
        } catch (Exception e) {
            VERSION = "v" + Bukkit.getServer().getBukkitVersion().replace("-SNAPSHOT", "").replace("-R0.", "_R").replace(".", "_");
            MINOR_VERSION = Integer.parseInt(VERSION.split("_")[1]);
        }

        try {
            Class.forName("xyz.refinedev.spigot.features.combat.CombatAPI");
            SUPPORTS_CARBON = true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("xyz.refinedev.spigot.api.knockback.KnockbackAPI");
                SUPPORTS_CARBON = true;
            } catch (ClassNotFoundException ex) {
                SUPPORTS_CARBON = false;
            }
        }
    }
}
