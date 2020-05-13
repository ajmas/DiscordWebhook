/*
 * Copyright (C) 2017 Artu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.artuto.discordwebhook;

import me.artuto.discordwebhook.loader.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.Reader;

import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("WeakerAccess")
public class Webhook extends JavaPlugin
{
    public static Webhook plugin;

    @Override
    public void onEnable()
    {
        plugin = this;

        plugin.getServer().getLogger().info(String.format("Enabling DiscordWebhook V.%s...", Const.VERSION));
        CommandHandler cmdHandler = new CommandHandler();
        Config config = new Config(this.getConfig());
        saveConfig();

        if(config.getUrl().isEmpty())
        {
            getServer().getLogger().warning("The webhook URL is not configured!");
        }

        this.getCommand("webhook").setExecutor(cmdHandler);
        getServer().getPluginManager().registerEvents(new EventListener(config), this);

        // Checks the server's external IP and announces it
        if (config.getEnabledEvents().indexOf("externalIP") > -1) {
            String externalIP = this.getExternalIP(config.getIPCheckUrl());
            getServer().getLogger().info("External IP: " + externalIP);
            Sender.externalIP(externalIP, config.getUrl());
        }
    }

    @Override
    public void onDisable()
    {
        plugin.getLogger().info(String.format("Disabling DiscordWebhook V.%s...", Const.VERSION));
        getServer().getScheduler().cancelTasks(this);
    }

    public String getExternalIP(String ipCheckUrl) {
        String address = null;
        Response response = null;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(ipCheckUrl)
                    .get()
                    .build();

            response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                StringBuilder strBuilder = new StringBuilder();
                char[] buffer = new char[128];
                Reader reader = response.body().charStream();
                int len = -1;
                while ((len = reader.read(buffer)) > -1) {
                    strBuilder.append(buffer, 0, len);
                }
                address = strBuilder.toString();
            }

        } catch (Exception ex) {
            logError(ex);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return address;
    }


    public static void logError(Exception e)
    {
        plugin.getServer().getLogger().severe("Error with DiscordWebhook "+e);
        e.printStackTrace();
    }


    public static boolean checkUrl(String url)
    {
        if(url.trim().isEmpty() || url.trim().equals("https://canary.discordapp.com/api/webhooks"))
        {
            plugin.getServer().getLogger().severe("The Webhook URL is empty!");
            return false;
        }
        else return true;
    }
}
