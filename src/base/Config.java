package base;

import arc.Core;

public enum Config {
    ApiKey("Api Key to access BMI API. To get an API key go to discord.gg/v7SyYd2D3y and use the bot slash command.", "", "ApiKey"),
    NudityAction("0: Ban, 1: Kick, 2: Force Disconnect, 3: Ignore", 1, "NudityAction"),
    BorderlineAction("0: Ban, 1: Kick, 2: Force Disconnect, 3: Ignore", 1, "BorderlineAction"),
    FurryAction("Not NSFW or Borderline | 0: Ban, 1: Kick, 2: Force Disconnect, 3: Ignore", 3, "FurryAction"),
    ComplexSearch("Whether to perform a complex search on code. This prevents easy bypass of scan by manually editing a few lines code.", false, "ComplexSearch"),
    DisconnectMessage("What message to send when user is banned/kicked/disconnected. Identifier and BMI discord invite will still be sent.", "[scarlet]Built banned logic image", "KickBanMessage"),
    BroadcastTimeout("How often, in millis, the server will broadcast when a player is building nsfw.", 2000, "BroadcastTimeout"),
    ConnectionTimeout("How long, in millis, the server will wait for a http response before giving up.", 1000, "ConnectionTimeout"),
    KickDuration("How many minutes the player will kick be for.", 3 * 60, "KickDuration"),
    HTTPThreadCount("How many threads used to send HTTP Get request to the api.", 4);

    public static final Config[] all = values();

    public final Object defaultValue;
    public final String key, description;
    final Runnable changed;

    Config(String description, Object def) {
        this(description, def, null, null);
    }

    Config(String description, Object def, String key) {
        this(description, def, key, null);
    }

    Config(String description, Object def, Runnable changed) {
        this(description, def, null, changed);
    }

    Config(String description, Object def, String key, Runnable changed) {
        this.description = description;
        this.key = "gib_" + (key == null ? name() : key);
        this.defaultValue = def;
        this.changed = changed == null ? () -> {
        } : changed;
    }

    public boolean isNum() {
        return defaultValue instanceof Integer;
    }

    public boolean isBool() {
        return defaultValue instanceof Boolean;
    }

    public boolean isString() {
        return defaultValue instanceof String;
    }

    public Object get() {
        return Core.settings.get(key, defaultValue);
    }

    public boolean bool() {
        return Core.settings.getBool(key, (Boolean) defaultValue);
    }

    public int num() {
        return Core.settings.getInt(key, (Integer) defaultValue);
    }

    public String string() {
        return Core.settings.getString(key, (String) defaultValue);
    }

    public void set(Object value) {
        Core.settings.put(key, value);
        changed.run();
    }
}