package dogged.swordtrims.listeners;

import dogged.swordtrims.SwordTrims;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class SwordListener implements Listener {

    private Map<UUID, Long> raiserCooldowns = new HashMap<>();
    private Map<UUID, Long> wardCooldowns = new HashMap<>();
    private Map<UUID, Long> silenceCooldowns = new HashMap<>();

    private final SwordTrims plugin;

    public SwordListener(SwordTrims plugin) {
        getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getConfig().get("RaiserTrim.multiplier") == null) {
            plugin.getConfig().set("RaiserTrim.multiplier", 2);
            plugin.saveConfig();
        }

        this.plugin = plugin;
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent e) {
        if (!e.getAction().toString().contains("RIGHT_CLICK")) return;
        Player p = e.getPlayer();

        PlayerInventory inv = p.getInventory();
        ItemStack hand = inv.getItemInMainHand();

        if (hand.getItemMeta() == null) return;
        if (!hand.getType().toString().contains("SWORD")) return;

        List<Component> lores = hand.lore();
        if (lores == null) return;
        if (lores.isEmpty()) return;

        Location loc = p.getLocation();

        for (Component comp : lores) {
            String lore = LegacyComponentSerializer.legacySection().serialize(comp);

            if (lore.contains("Raiser Trim")) {
                Map<Map<UUID, Long>, Boolean> map = isOnCooldown(p, 20*1000, raiserCooldowns);
                if (map.get(raiserCooldowns)) {
                    List<Map<UUID, Long>> cooldowns = new ArrayList<>(map.keySet());
                    raiserCooldowns = cooldowns.get(0);
                    return;
                }

                int multiplier = plugin.getConfig().getInt("RaiserTrim.multiplier");

                Vector velocity = loc.getDirection().multiply(multiplier);
                p.setVelocity(velocity);
            } else if (lore.contains("Ward Trim")) {
                Map<Map<UUID, Long>, Boolean> map = isOnCooldown(p, 30*1000, wardCooldowns);
                if (map.get(wardCooldowns)) {
                    List<Map<UUID, Long>> cooldowns = new ArrayList<>(map.keySet());
                    wardCooldowns = cooldowns.get(0);
                    return;
                }

                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 20, 1, false, true));
            } else if (lore.contains("Silence Trim")) {
                Map<Map<UUID, Long>, Boolean> map = isOnCooldown(p, 45*1000, silenceCooldowns);
                if (map.get(silenceCooldowns)) {
                    List<Map<UUID, Long>> cooldowns = new ArrayList<>(map.keySet());
                    silenceCooldowns = cooldowns.get(0);
                    return;
                }

                double length = 25.0;
                double particleDistance = 0.5;
                int distance = 25;

                loc = loc.add(0, 1.5, 0);

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 10, 1);

                for (double i = 1; i < length; i += particleDistance) {
                    Vector v = loc.getDirection().multiply(i);
                    loc.add(v);

                    loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);
                }

                RayTraceResult entityResult = p.rayTraceEntities(distance);

                if (entityResult != null && entityResult.getHitEntity() != null) {
                    if (entityResult.getHitEntity() instanceof Player) {
                        Player victim = (Player) entityResult.getHitEntity();
                        victim.setHealth(victim.getHealth() - 6.0);
                    }
                }
            }
        }
    }

    private Map<Map<UUID, Long>, Boolean> isOnCooldown(Player p, int cooldownTime, Map<UUID, Long> cooldowns) {
        Map<Map<UUID, Long>, Boolean> map = new HashMap<>();
        UUID uuid = p.getUniqueId();

        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);

            if (currentTime - lastUsed < cooldownTime) {
                long remainingTime = (cooldownTime - (currentTime - lastUsed)) / 1000;
                p.sendMessage("§cPlease wait " + remainingTime + " seconds to use that ability again.");

                map.put(cooldowns, true);
            } else {
                cooldowns.replace(uuid, currentTime);
                map.put(cooldowns, false);
            }
        } else {
            cooldowns.put(uuid, currentTime);
            map.put(cooldowns, false);
        }

        return map;
    }
}
