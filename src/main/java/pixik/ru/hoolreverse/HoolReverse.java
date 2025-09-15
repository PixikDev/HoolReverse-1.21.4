package pixik.ru.hoolreverse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class HoolReverse extends JavaPlugin implements Listener, TabExecutor {

    private static final String REVERSE_PICKAXE_KEY = "reverse_pickaxe";
    private NamespacedKey reverseKey;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private int maxUses;
    private Material pickaxeMaterial;
    private String displayName;
    private List<String> lore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        reverseKey = new NamespacedKey(this, REVERSE_PICKAXE_KEY);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("hoolreverse").setExecutor(this);
        getCommand("hoolreverse").setTabCompleter(this);

        getServer().getPluginManager().registerEvents(new CommandListener(), this);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        maxUses = config.getInt("max-uses", 1);
        pickaxeMaterial = Material.matchMaterial(config.getString("pickaxe-material", "GOLDEN_PICKAXE"));
        if (pickaxeMaterial == null) {
            pickaxeMaterial = Material.GOLDEN_PICKAXE;
        }

        displayName = config.getString("display-name", "&x&A&9&0&0&F&F[&x&A&5&0&4&F&F♦&x&A&0&0&8&F&F] &x&9&7&1&0&F&FК&x&9&3&1&4&F&Fи&x&8&E&1&8&F&Fр&x&8&A&1&C&F&Fк&x&8&5&2&0&F&Fa &x&7&C&2&7&F&Fr&x&7&8&2&B&F&Fe&x&7&3&2&F&F&Fv&x&6&F&3&3&F&Fe&x&6&A&3&7&F&Fr&x&6&6&3&B&F&Fs&x&6&1&3&F&F&Fe");
        lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            lore = List.of(
                    "§7Специальная кирка для добычи спавнеров",
                    "§cПрочность: 1"
            );
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hoolreverse.admin")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("add")) {
            sender.sendMessage("§cИспользование: /hoolreverse add <ник> <количество>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cИгрок " + args[1] + " не найден или не в сети!");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount < 1) {
                sender.sendMessage("§cКоличество должно быть положительным числом!");
                return true;
            }
            if (amount > 1) {
                sender.sendMessage("§cМаксимальное количество - 1! Выдаю 1 кирку.");
                amount = 1;
            }

            ItemStack reversePickaxe = createReversePickaxe();
            if (target.getInventory().addItem(reversePickaxe).isEmpty()) {
                sender.sendMessage("§aКирка Reverse выдана игроку " + target.getName());
                target.sendMessage("§aВы получили Кирку Reverse!");
            } else {
                sender.sendMessage("§cНе удалось выдать кирку. Инвентарь игрока полон!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cКоличество должно быть числом!");
        }

        return true;
    }

    private ItemStack createReversePickaxe() {
        ItemStack pickaxe = new ItemStack(pickaxeMaterial);
        ItemMeta meta = pickaxe.getItemMeta();

        if (meta != null) {
            Component displayNameComponent = legacySerializer.deserialize(displayName);
            meta.displayName(displayNameComponent);

            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(legacySerializer.deserialize(line));
            }
            meta.lore(loreComponents);

            meta.getPersistentDataContainer().set(reverseKey, PersistentDataType.BYTE, (byte) 1);

            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                damageable.setDamage(pickaxeMaterial.getMaxDurability() - maxUses);
            }

            pickaxe.setItemMeta(meta);
        }

        return pickaxe;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (isReversePickaxe(item) && block.getType() == Material.SPAWNER) {
            event.setExpToDrop(0);

            ItemStack spawnerItem = new ItemStack(Material.SPAWNER);

            if (player.getInventory().addItem(spawnerItem).isEmpty()) {
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
                player.sendMessage("§6Ваш инвентарь полон! Спавнер выпал на землю.");
            }

            if (item.getItemMeta() instanceof Damageable) {
                Damageable damageable = (Damageable) item.getItemMeta();
                int currentDamage = damageable.getDamage();
                int maxDurability = item.getType().getMaxDurability();

                if (currentDamage >= maxDurability - 1) {
                    player.sendMessage("§6Ваша Кирка Reverse сломалась!");
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
        } else if (isReversePickaxe(item) && block.getType() != Material.SPAWNER) {
            event.setCancelled(true);
            player.sendMessage("§cЭта кирка может ломать только спавнеры!");
        }
    }

    private boolean isReversePickaxe(ItemStack item) {
        if (item == null || item.getType() != pickaxeMaterial || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(reverseKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getInventory().getFirstItem() != null && isReversePickaxe(event.getInventory().getFirstItem())) {
            event.setResult(null);
            if (event.getView().getPlayer() instanceof Player) {
                ((Player) event.getView().getPlayer()).sendMessage("§cКирку Reverse нельзя ремонтировать в наковальне!");
            }
        }

        if (event.getInventory().getSecondItem() != null && isReversePickaxe(event.getInventory().getSecondItem())) {
            event.setResult(null);
            if (event.getView().getPlayer() instanceof Player) {
                ((Player) event.getView().getPlayer()).sendMessage("§cКирку Reverse нельзя использовать для ремонта!");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("add".startsWith(args[0].toLowerCase())) {
                completions.add("add");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            if ("1".startsWith(args[2])) {
                completions.add("1");
            }
        }

        return completions;
    }

    private class CommandListener implements Listener {

        @EventHandler
        public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            String message = event.getMessage().toLowerCase();

            if (message.startsWith("/fix") || message.startsWith("/repair") ||
                    message.startsWith("/minecraft:fix") || message.startsWith("/minecraft:repair") ||
                    message.startsWith("/essentials:fix") || message.startsWith("/essentials:repair")) {

                for (ItemStack item : player.getInventory().getContents()) {
                    if (isReversePickaxe(item)) {
                        player.sendMessage("§cНельзя использовать команды ремонта, пока у вас есть Кирка Reverse!");
                        event.setCancelled(true);
                        return;
                    }
                }

                if (isReversePickaxe(player.getInventory().getItemInMainHand()) ||
                        isReversePickaxe(player.getInventory().getItemInOffHand())) {
                    player.sendMessage("§cНельзя использовать команды ремонта, пока у вас есть Кирка Reverse!");
                    event.setCancelled(true);
                }
            }
        }
    }
}