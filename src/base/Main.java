package base;

import arc.Core;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.world.blocks.logic.LogicBlock;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends Plugin {
    private static final String DisconnectMessageInfo = "\n[white]Hash Identifier: %s-%s\nIf you didn't place a banned mindustry image, join [sky]discord.gg/v7SyYd2D3y []and file a report.";

    private static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, Config.HTTPThreadCount.num(),
            5000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    private static final MessageDigest messageDigest;

    private static long Wait404 = System.currentTimeMillis();

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Type {
        NSFW, BORDERLINE, FURRY, UNKNOWN
    }

    @Override
    public void init() {
        if (Config.ApiKey.string().isEmpty()) {
            Log.warn("\n\n\nBMI: &rAPI key has not been configured. &frPlease go to discord.gg/v7SyYd2D3y and use the bot slash command to get an api key. Configure the api key using the \"bmiconfig ApiKey insertKeyHere\" command\n\n");
            return;
        }
        HttpGet temp1 = new HttpGet(Vars.ghApi + "/repos/L0615T1C5-216AC-9437/BannedMindustryImage/releases/latest");
        HttpGet temp2 = new HttpGet("http://c-n.ddns.net:8888/bmi/v2/ping");
        temp2.addHeader("X-api-key", Config.ApiKey.string());
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(temp1)) {
                int code = response.getStatusLine().getStatusCode();
                if (code == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    final JSONObject data = new JSONObject(responseBody);
                    if (data.getFloat("tag_name") > Float.parseFloat(Vars.mods.list().find(b -> b.meta.name.equals("BMI")).meta.version)) {
                        Log.warn("\n\n\nBMI: &yNewer version of this plugin found! Please update the mod to maintain access to the BMI API!&fr\n\n");
                    } else {
                        Log.info("BMI: &gThis plugin is up to date!&fr");
                    }
                } else {
                    Log.err("BMI: &rUnable to check for updates&fr");
                }
            }
            try (CloseableHttpResponse response = client.execute(temp2)) {
                int code = response.getStatusLine().getStatusCode();
                switch (code) {
                    case 200 -> Log.info("BMI: &gApi connection is OK&fr");
                    case 400 -> {
                        Log.err("BMI: &rInvalid Api Key! \"&fr" + Config.ApiKey.string() + "&r\" is not a valid api key! &lbGo to discord.gg/v7SyYd2D3y to get a new api key.&fr");
                        return;
                    }
                    case 404 -> Log.warn("BMI: &yBMI Api is offline!&fr");
                }
            }
        } catch (IOException e) {
            Log.err(e);
        }

        Events.on(EventType.BlockBuildBeginEvent.class, event -> {
            if (event.breaking || event.unit == null || event.unit.getPlayer() == null || Wait404 > System.currentTimeMillis())
                return;
            final Player p = event.unit.getPlayer();
            if (event.tile.build instanceof final LogicBlock.LogicBuild lb) {
                lb.configured(null, lb.config());
                if (lb.code.contains("drawflush display") && isImageProc(lb.code)) {
                    final String[] check = Config.ComplexSearch.bool() ? lb.code.split("drawflush display.\n") : new String[]{lb.code};
                    executorService.execute(() -> {
                        RequestConfig config = RequestConfig.custom().setConnectTimeout(Config.ConnectionTimeout.num()).setSocketTimeout(Config.ConnectionTimeout.num()).build();
                        HttpClientBuilder hcb = HttpClientBuilder.create();
                        hcb.setDefaultRequestConfig(config);
                        try (CloseableHttpClient client = hcb.build()) {
                            var a = new URIBuilder(URI.create("http://c-n.ddns.net:8888/bmi/v2/check"));
                            var e = Base64.getEncoder();
                            //sha256, then encode to base 64
                            for (int i = 0, count = 0; i < check.length && count < 128; i++, count++)
                                a.addParameter("hash", e.encodeToString(messageDigest.digest(check[i].getBytes(StandardCharsets.UTF_8))));
                            Log.debug("BMI: Checking " + check.length + " hash(es)");
                            //make get request
                            HttpGet get = new HttpGet(a.build());
                            get.addHeader("X-api-key", Config.ApiKey.string());
                            //execute
                            try (CloseableHttpResponse response = client.execute(get)) {
                                int code = response.getStatusLine().getStatusCode();
                                switch (code) {
                                    case 404 -> {
                                        Log.err("BMI: &rFailed to connect to BMI api!&fr");
                                        Wait404 = System.currentTimeMillis() + 300000L;
                                    }
                                    case 200 -> {
                                        //dev
                                        if (Administration.Config.debug.bool()) {
                                            Log.debug("BMI: Miss!");
                                            for (var h : response.getAllHeaders())
                                                System.out.println(h.getName() + ": " + h.getValue());
                                        }
                                    }
                                    case 302 -> {
                                        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                                        final JSONObject data = new JSONObject(responseBody);
                                        //dev
                                        if (Administration.Config.debug.bool()) {
                                            Log.debug("BMI: Hit!");
                                            for (var h : response.getAllHeaders())
                                                System.out.println(h.getName() + ": " + h.getValue());
                                            Log.debug(responseBody);
                                        }

                                        Core.app.post(() -> hit(p, data));
                                    }
                                }
                            } catch (SocketTimeoutException ignored) {
                                Log.err("BMI: &rHttp socket has timed out!&fr Consider increasing&lb ConnectionTimeout&fr in&lb bmiconfig&fr to allow more time for the socket to connect.");
                            }
                        } catch (IOException | URISyntaxException e) {
                            Log.err(e);
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        });
    }
    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("bmiconfig", "[name] [value...]", "Configure server settings.", arg -> {
            if (arg.length == 0) {
                Log.info("All config values:");
                for (Config c : Config.all) {
                    Log.info("&lk| @: @", c.name(), "&lc&fi" + c.get());
                    Log.info("&lk| | &lw" + c.description);
                    Log.info("&lk|");
                }
                Log.info("use the command with the value set to \"default\" in order to use the default value.");
                return;
            }

            try {
                Config c = Config.valueOf(arg[0]);
                if (arg.length == 1) {
                    Log.info("'@' is currently @.", c.name(), c.get());
                } else {
                    if (arg[1].equals("default")) {
                        c.set(c.defaultValue);
                    } else if (c.isBool()) {
                        c.set(arg[1].equals("on") || arg[1].equals("true"));
                    } else if (c.isNum()) {
                        try {
                            c.set(Integer.parseInt(arg[1]));
                        } catch (NumberFormatException e) {
                            Log.err("Not a valid number: @", arg[1]);
                            return;
                        }
                    } else if (c.isString()) {
                        c.set(arg[1].replace("\\n", "\n"));
                    }

                    Log.info("@ set to @.", c.name(), c.get());
                    Core.settings.forceSave();
                }
            } catch (IllegalArgumentException e) {
                Log.err("Unknown config: '@'. Run the command with no arguments to get a list of valid configs.", arg[0]);
            }
        });
    }

    private static void hit(Player p, JSONObject data) {
        Type type = Type.valueOf(data.getString("type"));
        switch (switch (type) {
            case NSFW -> Config.NudityAction.num();
            case BORDERLINE -> Config.BorderlineAction.num();
            case FURRY -> Config.FurryAction.num();
            case UNKNOWN -> 3;
        }) {
            case 0 -> {
                Log.info("&lbBanning &fr" + Strings.stripColors(p.name) + " &lbfor placing `&fr" + type.name() + "&lb` Banned Mindustry Image!&fr");
                Vars.netServer.admins.banPlayerID(p.con.uuid);
                Vars.netServer.admins.banPlayerIP(p.con.address);
                p.con.kick(Config.DisconnectMessage.string() + String.format(DisconnectMessageInfo, Integer.toHexString(data.getInt("bid")), Integer.toHexString(data.getInt("id"))), Config.KickDuration.num() * 60000L);
            }
            case 1 -> {
                Log.info("&lbKicking &fr" + Strings.stripColors(p.name) + " &lbfor placing `&fr" + type.name() + "&lb` Banned Mindustry Image!&fr");
                p.con.kick(Config.DisconnectMessage.string() + String.format(DisconnectMessageInfo, Integer.toHexString(data.getInt("bid")), Integer.toHexString(data.getInt("id"))), Config.KickDuration.num() * 60000L);
            }
            case 2 -> {
                Log.info("&lbDisconnecting &fr" + Strings.stripColors(p.name) + " &lbfor placing `&fr" + type.name() + "&lb` Banned Mindustry Image!&fr");
                Call.kick(p.con, Config.DisconnectMessage.string() + String.format(DisconnectMessageInfo, Integer.toHexString(data.getInt("bid")), Integer.toHexString(data.getInt("id"))));
            }
            case 3 -> Log.debug("Ignored due to config.");
        }
    }

    public static boolean isImageProc(String code) {
        int count = 0;
        for (int index = 0, temp; (temp = code.indexOf("draw ", index)) != -1 && count < 200; ) {
            index = temp + "draw ".length();
            count++;
        }
        return count == 200;
    }
}
