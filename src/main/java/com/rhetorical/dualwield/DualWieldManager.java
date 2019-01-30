package com.rhetorical.dualwield;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class DualWieldManager implements Listener {

    DualWieldManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    @EventHandler
    void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (!Main.allowedMaterials.contains(e.getPlayer().getInventory().getItemInOffHand().getType()))
            return;

        if (!Main.disallowedMaterials.contains(e.getPlayer().getInventory().getItemInMainHand().getType())) {
			performSwing(e.getPlayer());
		}
    }

    private double getDamage(ItemStack stack, LivingEntity victim, double baseDamage) {

        /* Enchantment management */

        Map<Enchantment, Integer> enchantments = stack.getEnchantments();

        if (enchantments.containsKey(Enchantment.DAMAGE_ARTHROPODS)) {
            if (victim instanceof Spider || victim instanceof Silverfish || victim instanceof Endermite) {
                baseDamage += (2.5 * enchantments.get(Enchantment.DAMAGE_ARTHROPODS));
            }
        }

        if (enchantments.containsKey(Enchantment.FIRE_ASPECT)) {
            victim.setFireTicks(80 * enchantments.get(Enchantment.FIRE_ASPECT));
        }

        if (Main.postWaterUpdate) {
            if (enchantments.containsKey(Enchantment.IMPALING)) {
                if (victim instanceof Dolphin || victim instanceof ElderGuardian || victim instanceof Fish || victim instanceof Guardian || victim instanceof Squid || victim instanceof Turtle) {
                    baseDamage += (2.5 * enchantments.get(Enchantment.IMPALING));
                }
            }
        }

        if (enchantments.containsKey(Enchantment.DAMAGE_ALL)) {
            baseDamage += 1 + (0.5 * enchantments.get(Enchantment.DAMAGE_ALL));
        }

        if (enchantments.containsKey(Enchantment.DAMAGE_UNDEAD)) {
            if (victim instanceof Skeleton || victim instanceof Zombie || victim instanceof Wither || victim instanceof SkeletonHorse) {
                baseDamage += (2.5 * enchantments.get(Enchantment.DAMAGE_UNDEAD));
            }


            if (Main.postWaterUpdate) {
                if (victim instanceof Phantom) {
                    baseDamage += (2.5 * enchantments.get(Enchantment.DAMAGE_UNDEAD));
                }
            }

        }

        if (enchantments.containsKey(Enchantment.DURABILITY)) {
            double rand = Math.random() * 100;

            if ((100 / enchantments.get(Enchantment.DURABILITY) + 1) > rand) {
                stack.setDurability((short) (stack.getDurability() + (short) 1));
            }
        } else {
            stack.setDurability((short) (stack.getDurability() + (short) 1));
        }


        /* End enchantment management */

        if (!(victim instanceof Player)) {
            return baseDamage;
        }

        
        Player p = (Player) victim;

        if (getArmorPoints(p) == 0) {
            return baseDamage;
        }

        return baseDamage * (1 - Math.min(20, Math.max(getArmorPoints(p) / 5, getArmorPoints(p) - baseDamage / (getArmorToughness(p) / 4 + 2))) / 25);
    }

    private int getArmorPoints(Player p) {

        return (int) p.getAttribute(Attribute.GENERIC_ARMOR).getValue();
    }

    private int getArmorToughness(Player p) {
        return (int) p.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
    }

    private boolean performSwing(Player from) {

        ItemStack heldItem = from.getInventory().getItemInOffHand();

        if (Main.requirePermission) {
            if (!from.hasPermission("rdw.use." + heldItem.getType().toString().toUpperCase()) && !from.isOp() && !from.hasPermission("rdw.use.*"))
                return false;
        }

        playAnimation(from);

        Entity e = getTargetEntity(from, ItemStats.getSwingDistance(heldItem.getType()));

        if (e != null) {
            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(from, e, EntityDamageEvent.DamageCause.CUSTOM, ItemStats.getAttackDamage(heldItem.getType()));
            Bukkit.getServer().getPluginManager().callEvent(event);
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerHitEvent(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            if (!(e.getEntity() instanceof LivingEntity))
                return;

            Player p = (Player) e.getDamager();

            ItemStack hit = p.getInventory().getItemInOffHand();

            LivingEntity le = (LivingEntity) e.getEntity();
            double damage = getDamage(hit, le, e.getDamage());
            le.damage(damage, e.getDamager());

            le.setVelocity(e.getDamager().getLocation().getDirection().setY(0.3d).multiply(1d));

            /* Enchantment stuff */

            if (!(e.getEntity() instanceof Player))
                return;

            Player victim = (Player) e.getEntity();

            List<ItemStack> armor = new ArrayList<ItemStack>();

            if (victim.getInventory().getBoots() != null) {
                armor.add(victim.getInventory().getBoots());
            }

            if (victim.getInventory().getLeggings() != null) {
                armor.add(victim.getInventory().getLeggings());
            }

            if (victim.getInventory().getChestplate() != null) {
                armor.add(victim.getInventory().getChestplate());
            }

            if (victim.getInventory().getHelmet() != null) {
                armor.add(victim.getInventory().getHelmet());
            }

            e.setCancelled(true);

//            EntityDamageByEntityEvent entityDamageByEntityEvent = new EntityDamageByEntityEvent(e.getDamager(), victim,
//					EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
//
//            Bukkit.getServer().getPluginManager().callEvent(entityDamageByEntityEvent);

            double thornsDamage = 0d;

            for(ItemStack a : armor) {
                Map<Enchantment, Integer> enchantments = a.getEnchantments();

                if (enchantments.isEmpty())
                    continue;


                if (enchantments.containsKey(Enchantment.THORNS)) {
                    int level = enchantments.get(Enchantment.THORNS);

                    double rand = Math.random();

                    if (level * .15 < rand) {
                        thornsDamage +=  Math.floor((Math.random() * 3) + 1);
                    }
                }
            }

            if (thornsDamage != 0) {
//            	EntityDamageByEntityEvent thornsEvent = new EntityDamageByEntityEvent(victim, e.getDamager(), EntityDamageEvent.DamageCause.THORNS, thornsDamage);
//            	Bukkit.getServer().getPluginManager().callEvent(thornsEvent);
                p.damage(thornsDamage);
                p.getWorld().playSound(victim.getLocation(), Sound.ENCHANT_THORNS_HIT, 1, 1);
            }

        }
    }

    private LivingEntity getTargetEntity(Player player, int distance) {
        Collection<Entity> entities = player.getNearbyEntities(distance, 10, 30);
        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = distance; i >= 1; i--) {
            locations.add(player.getTargetBlock(null, i).getLocation());
        }

        for(Entity e : entities) {

            if (!(e instanceof LivingEntity))
                continue;

            LivingEntity entity = (LivingEntity) e;

            for(Location loc : locations) {
                int locX = (int) loc.getX();
                int locY = (int) loc.getY();
                int locZ = (int) loc.getZ();

                int entX = (int) entity.getLocation().getX();
                int entY = (int) entity.getLocation().getY();
                int entZ = (int) entity.getLocation().getZ();

                if(((locX-2 < entX)&&(entX < locX+2))&&((locY-3 < entY)&&(entY < locY+3))&&((locZ-2 < entZ)&&(entZ < locZ+2))) {
                    return entity;
                }
            }
        }
        return null;
    }

    /* NMS Stuff */

    private void playAnimation(Player p) {
        try {
            Constructor<?> animationConstructor = Main.getNMSVersion("PacketPlayOutAnimation").getConstructor(Main.getNMSVersion("Entity"), int.class);
            Object packet = animationConstructor.newInstance(Main.getCraftPlayer().cast(p).getClass().getMethod("getHandle").invoke(p), 3);
            sendPacket(p, packet);
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage("§cCould not get class for name! Is there a typo?");
            e.printStackTrace();
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = Main.getCraftPlayer().cast(player).getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            Class<?> packetClass = Main.getNMSVersion("Packet");
            playerConnection.getClass().getMethod("sendPacket" , packetClass).invoke(playerConnection, packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}