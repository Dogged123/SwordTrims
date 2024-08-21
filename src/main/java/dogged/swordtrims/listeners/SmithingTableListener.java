package dogged.swordtrims.listeners;

import dogged.swordtrims.SwordTrims;
import dogged.swordtrims.utilties.ItemBuilder;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class SmithingTableListener implements Listener {

    private final List<Player> doNotGiveItemsOnClose = new ArrayList<>();
    private final List<Player> receivedExcessUpgradeTemplates = new ArrayList<>();
    private final List<Player>  receivedExcessMaterial = new ArrayList<>();
    private final List<Player>  receivedExcessTrims = new ArrayList<>();

    private final Map<Player, ItemStack> prev16 = new HashMap<>();
    private final Map<Player, ItemStack> prev10 = new HashMap<>();
    private final Map<Player, ItemStack> prev12 = new HashMap<>();
    private final Material[] materials = {
            Material.COPPER_INGOT,
            Material.REDSTONE,
            Material.QUARTZ,
            Material.AMETHYST_SHARD,
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.LAPIS_LAZULI,
            Material.EMERALD,
            Material.DIAMOND,
            Material.NETHERITE_INGOT
    };

    Map<Material, String[]> trimPatterns = new HashMap<>();

    private final SwordTrims plugin;

    public SmithingTableListener(SwordTrims plugin) {
        getServer().getPluginManager().registerEvents(this, plugin);

        trimPatterns.put(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, new String[] {
                "Raiser Trim", "§3Ability ⇒", "§7[§fRight Click§7] §rDash Through the Air"});

        trimPatterns.put(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, new String[] {
                "Ward Trim", "§3Ability ⇒", "§7[§fRight Click§7] §r Strength II for 20 Seconds"});

        trimPatterns.put(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, new String[] {
                "Silence Trim", "§3Ability ⇒", "§7[§fRight Click§7] §rSonic Boom another Player"});

        this.plugin = plugin;
    }

    @EventHandler
    public void onSmithingTableUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (p.getTargetBlock(null, 5).getType().equals(Material.SMITHING_TABLE)) {
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                if (p.getInventory().getItemInMainHand().getType().toString().contains("SWORD"))
                    Bukkit.getScheduler().runTaskLater(plugin, () -> createSmithingGUI(p), 1);
            }
        }
    }

    @EventHandler
    public void onSmithingTableClose(InventoryCloseEvent e){
        if (e.getView().getOriginalTitle().equals("§0Smithing Table")){
            Player p = (Player) e.getPlayer();
            Inventory inv = e.getInventory();

            receivedExcessUpgradeTemplates.remove(p);
            receivedExcessMaterial.remove(p);
            receivedExcessTrims.remove(p);

            if (doNotGiveItemsOnClose.contains(p)){
                doNotGiveItemsOnClose.remove(p);
                return;
            }

            for (int i = 10; i <= 12; i++) {
                if (inv.getItem(i) != null && !(inv.getItem(i).getItemMeta().getDisplayName().equals("§7"))) p.getInventory().addItem(inv.getItem(i));
            }

            if (!(inv.getItem(16).getType().equals(Material.BARRIER))) p.getInventory().addItem(prev16.get(p));
            ItemStack item;
            if (prev10.containsKey(p) && prev10.get(p).getType().toString().contains("TRIM_SMITHING")){
                item = prev10.get(p);
                item.setAmount(1);
                p.getInventory().addItem(item);
            }else if (prev10.containsKey(p) && prev10.get(p).getType().equals(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)){
                p.getInventory().addItem(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            }
            if (prev12.containsKey(p)){
                item = prev12.get(p);
                item.setAmount(1);
                p.getInventory().addItem(item);
            }
            prev16.remove(p);
            prev10.remove(p);
            prev12.remove(p);
        }
    }

    @EventHandler
    public void onSmithingGUIUse(InventoryClickEvent e){
        if (e.getView().getOriginalTitle().equals("§0Smithing Table")) {
            Player p = (Player) e.getWhoClicked();
            if (e.getRawSlot() < e.getInventory().getSize() && e.getSlot() != 10 && e.getSlot() != 11 && e.getSlot() != 12) e.setCancelled(true);
            if (e.getRawSlot() < e.getInventory().getSize() && (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta().getDisplayName().equals("§7"))) e.setCancelled(true);
            if (e.getSlot() == 16 && !(e.getCurrentItem().getType().equals(Material.BARRIER))){
                p.getInventory().addItem(e.getCurrentItem());
                prev16.remove(p);
                prev10.remove(p);
                prev12.remove(p);
                if (!doNotGiveItemsOnClose.contains(p)){
                    doNotGiveItemsOnClose.add(p);
                }
                receivedExcessUpgradeTemplates.remove(p);
                receivedExcessMaterial.remove(p);
                receivedExcessMaterial.remove(p);
                receivedExcessTrims.remove(p);
                createSmithingGUI(p);
                return;
            }
            int emptySlots = 0;
            Inventory inv = e.getInventory();

            for (ItemStack is : inv){
                if (is == null){
                    emptySlots ++;
                }
            }

            if (emptySlots == 0) {
                if (inv.getItem(10).getType().equals(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                        && inv.getItem(11).getType().toString().toLowerCase().contains("diamond")
                        && inv.getItem(12).getType().equals(Material.NETHERITE_INGOT)) {
                    ItemStack item;

                    if (inv.getItem(10).getAmount() > 1) {
                        if (!receivedExcessUpgradeTemplates.contains(p)) {
                            item = inv.getItem(10);
                            item.setAmount(inv.getItem(10).getAmount() - 1);
                            p.getInventory().addItem(item);
                            receivedExcessUpgradeTemplates.add(p);
                        }
                    }
                    if (inv.getItem(12).getAmount() > 1) {
                        if (!receivedExcessMaterial.contains(p)) {
                            item = inv.getItem(12);
                            item.setAmount(inv.getItem(12).getAmount() - 1);
                            p.getInventory().addItem(item);
                            receivedExcessMaterial.add(p);
                        }
                    }

                    prev10.put(p, inv.getItem(10));
                    prev12.put(p, inv.getItem(12));
                    ItemStack output = inv.getItem(11).withType(Material.getMaterial(inv.getItem(11).getType().toString().replace("DIAMOND", "NETHERITE")));

                    item = inv.getItem(11).withType(Material.getMaterial(inv.getItem(11).getType().toString().replace("NETHERITE", "DIAMOND")));
                    prev16.put(p, item);

                    inv.setItem(10, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                    inv.setItem(11, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                    inv.setItem(12, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                    inv.setItem(14, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§7").toItemStack());
                    inv.setItem(16, output);
                    if (!doNotGiveItemsOnClose.contains(p)) {
                        doNotGiveItemsOnClose.add(p);
                    }
                    p.openInventory(inv);

                } else {
                    boolean correctMaterial = false;
                    for (Material material : materials) {
                        if (material.equals(inv.getItem(12).getType())) {
                            correctMaterial = true;
                            break;
                        }
                    }

                    boolean correctTrim = trimPatterns.containsKey(inv.getItem(10).getType());

                    boolean correctEquipment = false;
                    if (inv.getItem(11).getType().toString().toLowerCase().contains("sword")) {
                        correctEquipment = true;
                    }

                    if (inv.getItem(10).getType().toString().toLowerCase().contains("trim_smithing")
                            && correctEquipment
                            && correctMaterial
                            && correctTrim) {
                        ItemStack item;

                        if (inv.getItem(10).getAmount() > 1) {
                            if (!receivedExcessTrims.contains(p)) {
                                item = inv.getItem(10);
                                item.setAmount(inv.getItem(10).getAmount() - 1);
                                p.getInventory().addItem(item);
                                receivedExcessTrims.add(p);
                            }
                        }
                        if (inv.getItem(12).getAmount() > 1) {
                            if (!receivedExcessMaterial.contains(p)) {
                                item = inv.getItem(12);
                                item.setAmount(inv.getItem(12).getAmount() - 1);
                                p.getInventory().addItem(item);
                                receivedExcessMaterial.add(p);
                            }
                        }

                        Map<Material, String> trimMaterials = new HashMap<>();
                        trimMaterials.put(Material.COPPER_INGOT, "§6");
                        trimMaterials.put(Material.QUARTZ, "§f");
                        trimMaterials.put(Material.AMETHYST_SHARD, "§5");
                        trimMaterials.put(Material.IRON_INGOT, "§f");
                        trimMaterials.put(Material.GOLD_INGOT, "§e");
                        trimMaterials.put(Material.LAPIS_LAZULI, "§1");
                        trimMaterials.put(Material.EMERALD, "§a");
                        trimMaterials.put(Material.DIAMOND, "§b");
                        trimMaterials.put(Material.NETHERITE_INGOT, "§0");


                        prev10.put(p, inv.getItem(10));
                        prev16.put(p, inv.getItem(11).clone());
                        prev12.put(p, inv.getItem(12));

                        List<Component> trimMaterial = new ArrayList<>();
                        trimMaterial.add(Component.text(trimMaterials.get(inv.getItem(12).getType()) + trimPatterns.get(inv.getItem(10).getType())[0]));
                        trimMaterial.add(Component.text(trimPatterns.get(inv.getItem(10).getType())[1]));
                        trimMaterial.add(Component.text(trimPatterns.get(inv.getItem(10).getType())[2]));

                        ItemStack output = inv.getItem(11).clone();
                        ItemMeta outputMeta = output.getItemMeta();
                        outputMeta.lore(trimMaterial);
                        output.setItemMeta(outputMeta);

                        inv.setItem(10, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                        inv.setItem(11, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                        inv.setItem(12, new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack());
                        inv.setItem(14, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§7").toItemStack());
                        inv.setItem(16, output);

                        if (!doNotGiveItemsOnClose.contains(p)) {
                            doNotGiveItemsOnClose.add(p);
                        }
                        p.openInventory(inv);
                    }
                }
            }
        }
    }

    public void createSmithingGUI(Player p) {
        Inventory smithingGUI = Bukkit.createInventory(p, 27, Component.text("§0Smithing Table"));

        ItemStack filler = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName("§7").toItemStack();
        ItemStack template = new ItemBuilder(Material.WRITTEN_BOOK).setName("§4Template/Armour Trim").toItemStack();
        ItemStack equipment = new ItemBuilder(Material.DIAMOND_SWORD).setName("§aEquipment").toItemStack();
        ItemStack material = new ItemBuilder(Material.IRON_INGOT).setName("§aMaterial").toItemStack();
        ItemStack becomes = new ItemBuilder(Material.SPECTRAL_ARROW).setName("§bBecomes").toItemStack();
        ItemStack output = new ItemBuilder(Material.NETHERITE_INGOT).setName("§2Output").toItemStack();
        ItemStack redPane = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§fClick to see output").toItemStack();
        ItemStack barrier = new ItemBuilder(Material.BARRIER).setName("§7").toItemStack();

        smithingGUI.setItem(1, template);
        smithingGUI.setItem(2, equipment);
        smithingGUI.setItem(3, material);
        smithingGUI.setItem(5, becomes);
        smithingGUI.setItem(7, output);
        smithingGUI.setItem(14, redPane);
        smithingGUI.setItem(16, barrier);
        for (int i = 0; i < smithingGUI.getSize(); i++) {
            if (smithingGUI.getItem(i) == null){
                if (i != 10 && i != 11 && i != 12) smithingGUI.setItem(i, filler);
            }
        }
        p.openInventory(smithingGUI);
    }
}

