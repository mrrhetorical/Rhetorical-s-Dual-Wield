package com.rhetorical.dualwield;

import com.rhetorical.dualwield.raytracing.RayTrace;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.Map;

class PlayerUtility {

	/**
	 * Gets the armor points for the given player.
	 *
	 * @param p The player to get the armor points for.
	 * @return The amount of armor points the player has.
	 * */
	private static int getArmorPoints(Player p) {

		return (int) p.getAttribute(Attribute.GENERIC_ARMOR).getValue();
	}


	/**
	 * Gets the armor toughness for the given player.
	 *
	 * @param p The player to get the armor toughness for.
	 * @return The amount of armor toughness the player has.
	 * */
	private static int getArmorToughness(Player p) {
		return (int) p.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
	}

	/**
	 * Calculates the amount of damage that is done to the victim given the base damage and item stack in the hand.
	 * This takes into account both the victims armor as well as any enchantments on the weapon or on the armor.
	 *
	 * @param stack The ItemStack the player is currently holding.
	 * @param attacker The player that is the attacker.
	 * @param victim The victim of the attack. Can be any LivingEntity.
	 * @param baseDamage The calculated base damage of the attack.
	 * @return The amount of damage the attack should output.
	 * */
	static double getDamage(ItemStack stack, Player attacker, LivingEntity victim, double baseDamage) {

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
				if (victim instanceof Dolphin || victim instanceof Fish || victim instanceof Guardian || victim instanceof Squid || victim instanceof Turtle) {
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

		if (attacker.getGameMode() == GameMode.SURVIVAL || attacker.getGameMode() == GameMode.ADVENTURE)
			applyDurability(attacker, stack, enchantments);

		/* End enchantment management */

		if (!(victim instanceof Player)) {
			return baseDamage;
		}


		Player p = (Player) victim;

		if (PlayerUtility.getArmorPoints(p) == 0) {
			return baseDamage;
		}

		return baseDamage * (1 - Math.min(20, Math.max((float) PlayerUtility.getArmorPoints(p) / 5, PlayerUtility.getArmorPoints(p) - baseDamage / (float) (PlayerUtility.getArmorToughness(p) / 4 + 2))) / 25);
	}

private static void applyDurability(Player attacker, ItemStack stack, Map<Enchantment, Integer> enchantments) {
		ItemMeta meta = stack.getItemMeta();

		short durability = 0;
		try {
			durability = (short) ((Damageable) meta).getDamage();
		} catch (NullPointerException e) {
			if (Main.isDebug()) {
				e.printStackTrace();
			}

			return;
		}


		if (enchantments.containsKey(Enchantment.DURABILITY)) {
			double rand = Math.random() * 100;

			if ((100 / enchantments.get(Enchantment.DURABILITY) + 1) > rand) {
				durability = (short) (durability + (short) 1);
			}
		} else {
			durability = (short) (durability + (short) 1);
		}

		((Damageable) meta).setDamage((int) durability);

		if (durability >= stack.getType().getMaxDurability()) {
			attacker.getInventory().setItemInOffHand(new ItemStack(Material.AIR)); //todo: switch to null if error persists
			//attacker.getInventory().setItemInOffHand(null);
			attacker.playSound(attacker.getEyeLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
		} else {
			stack.setItemMeta(meta);
		}
	}

	/**
	 * Gets a target entity within the player's line of sight.
	 *
	 * @param player The player to check their line of sight.
	 * @param distance The distance of how far out from the player to check.
	 * @return Any LivingEntity that the player is looking at, or null if nothing.
	 * */
	static LivingEntity getTargetEntity(Player player, int distance) {

		Collection<Entity> entities = player.getNearbyEntities(distance, 10, 30);

		RayTrace rayTrace = new RayTrace(player.getEyeLocation().toVector(), player.getEyeLocation().getDirection().normalize());

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity))
				continue;

			LivingEntity entity = (LivingEntity) e;

			if (rayTrace.intersects(entity.getBoundingBox(), distance, Main.accuracy)) {
				return entity;
			}
		}

		return null;
	}
}