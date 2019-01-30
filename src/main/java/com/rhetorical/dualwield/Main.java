package com.rhetorical.dualwield;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin {

    static Plugin plugin;

    private static String versionNMS;

    static List<Material> allowedMaterials;
    static List<Material> disallowedMaterials; //Materials that will prevent the offhand from swinging if they are present.

    static boolean requirePermission;

    static boolean postWaterUpdate = false;

    private DualWieldManager manager;

    private String prefix = ChatColor.WHITE + "[" + ChatColor.YELLOW + "DW" + ChatColor.WHITE + "]" + ChatColor.RESET + " ";

    @Override
    public void onEnable() {
        plugin = this;

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        versionNMS = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        manager = new DualWieldManager();

        if (Bukkit.getServer().getBukkitVersion().startsWith("1.13")) {
            postWaterUpdate = true;
        } else {
            for (int i = 14; i < 100; i++) {
                if (Bukkit.getServer().getBukkitVersion().startsWith("1." + i)) {
                    postWaterUpdate = true;
                    break;
                }

                if (Bukkit.getServer().getBukkitVersion().startsWith("2")) {
                    postWaterUpdate = true;
                    break;
                }
            }
        }

        if (postWaterUpdate) {
            Bukkit.getConsoleSender().sendMessage(prefix + "Spigot version 1.13+ detected!");
        } else {
            Bukkit.getConsoleSender().sendMessage(prefix + "Spigot version < 1.12 detected!");
        }


        List<String> materialNames = plugin.getConfig().getStringList("offhand_materials");
        allowedMaterials = new ArrayList<>();

        for(String mat : materialNames) {
            Material m;
            try {
                m = Material.valueOf(mat);
            } catch(Exception ignored) {
                continue;
            }


            allowedMaterials.add(m);
        }

        List<String> disallowedMaterialNames = plugin.getConfig().getStringList("disallowed_materials");
        disallowedMaterials = new ArrayList<>();

        for (String mat : disallowedMaterialNames) {
        	Material m;
        	try {
        		m = Material.valueOf(mat);
			} catch(Exception ignored) {
        		continue;
			}

        	disallowedMaterials.add(m);
		}

        requirePermission = plugin.getConfig().getBoolean("require_permission");

        Bukkit.getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "DuelWielding v" + ChatColor.WHITE + plugin.getDescription().getVersion() + ChatColor.GREEN + " is now enabled!");

    }

    @Override
    public void onDisable() {
    	allowedMaterials = new ArrayList<>();
    	disallowedMaterials = new ArrayList<>();

    	System.gc();
    }

    static Class<?> getNMSVersion(String name){
        try {
            return Class.forName("net.minecraft.server." + versionNMS + "." + name);
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not get class for name! Is there a typo?");
            e.printStackTrace();
            return null;
        }
    }

    static Class getCraftPlayer() {
        try {
            return Class.forName("org.bukkit.craftbukkit." + versionNMS + ".entity.CraftPlayer");
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not get CraftPlayer class! Is there a typo?");
            e.printStackTrace();
            return null;
        }
    }

}