package xyz.refinedev.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.refinedev.api.storage.JsonStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
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
    private static final String MOJANG_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final String MOJANG_SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

    private final Map<String, CompletableFuture<CachedSkin>> skinFutures = new ConcurrentHashMap<>();

    /**
     * Custom JSON Storage for Skins
     */
    private final JsonStorage<Collection<CachedSkin>> skinStorage;

    /**
     * This list stores our cached skins, so we don't fetch them all the time.
     */
    private final Map<String, CachedSkin> skinCache = new ConcurrentHashMap<>();

    public SkinAPI(JavaPlugin plugin, Gson gson) {
        this.skinStorage = new JsonStorage<>("skins", plugin, gson);
        this.initiateCacheFromStorage();
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

    public void fetchSkin(String name, Consumer<CachedSkin> callback) {
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
        return this.skinCache.getOrDefault(name, null);
    }
}
