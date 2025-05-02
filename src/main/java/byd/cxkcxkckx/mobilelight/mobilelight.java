package byd.cxkcxkckx.mobilelight;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class mobilelight extends JavaPlugin implements Listener {
    private final Map<UUID, Location> lightLocations = new HashMap<>();
    private List<Material> lightItems = new ArrayList<>();
    private boolean debugMode = false;
    private boolean isLegacyVersion = false;
    private final Map<UUID, Location> torchLocations = new HashMap<>();
    private Object viaAPI;
    private final Map<UUID, Location> playerTorches = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // 保存默认配置
        saveDefaultConfig();
        // 从配置中读取调试模式设置
        debugMode = getConfig().getBoolean("debug-mode", false);
        // 检查服务器版本
        String version = getServer().getBukkitVersion();
        isLegacyVersion = version.contains("1.16") || version.contains("1.15") || 
                         version.contains("1.14") || version.contains("1.13") || 
                         version.contains("1.12") || version.contains("1.11") || 
                         version.contains("1.10") || version.contains("1.9") || 
                         version.contains("1.8");
        debugLog("服务器版本: " + version + ", 是否老版本: " + isLegacyVersion);
        
        // 只在非低版本服务器初始化ViaVersion
        if (!isLegacyVersion && getServer().getPluginManager().getPlugin("ViaVersion") != null) {
            try {
                Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
                viaAPI = viaClass.getMethod("getAPI").invoke(null);
                debugLog("ViaVersion API 已加载");
            } catch (Exception e) {
                getLogger().info("ViaVersion API 加载失败: " + e.getMessage());
            }
        } else if (!isLegacyVersion) {
            getLogger().info("ViaVersion 未找到，将直接使用光源方块");
        }
        
        // 从配置中读取触发物品列表
        List<String> itemNames = getConfig().getStringList("light-items");
        for (String itemName : itemNames) {
            try {
                Material material = Material.valueOf(itemName);
                lightItems.add(material);
            } catch (IllegalArgumentException e) {
                getLogger().warning("无效的物品名称: " + itemName);
            }
        }
        if (lightItems.isEmpty()) {
            getLogger().warning("没有有效的光源物品，将使用默认的火把");
            lightItems.add(Material.TORCH);
        }
    }

    private void debugLog(String message) {
        if (debugMode) {
            getLogger().info(message);
        }
    }

    private void sendFakeLightBlock(Player player, Location location) {
        // 获取玩家所在区块的所有玩家
        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.getWorld().equals(location.getWorld()) && 
                nearbyPlayer.getLocation().distance(location) <= 64) {
                // 根据观看者的版本发送不同的方块
                if (isLegacyClient(nearbyPlayer)) {
                    // 老版本玩家看到火把
                    nearbyPlayer.sendBlockChange(location, Material.TORCH.createBlockData());
                    debugLog("向老版本玩家 " + nearbyPlayer.getName() + " 发送假火把方块更新");
                } else {
                    // 新版本玩家看到光源方块
                    nearbyPlayer.sendBlockChange(location, Material.LIGHT.createBlockData());
                    debugLog("向新版本玩家 " + nearbyPlayer.getName() + " 发送假光源方块更新");
                }
            }
        }
    }

    private void removeFakeLightBlock(Player player, Location location) {
        // 获取玩家所在区块的所有玩家
        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.getWorld().equals(location.getWorld()) && 
                nearbyPlayer.getLocation().distance(location) <= 64) {
                // 发送真实方块更新
                Block block = location.getBlock();
                nearbyPlayer.sendBlockChange(location, block.getBlockData());
                debugLog("向玩家 " + nearbyPlayer.getName() + " 发送真实方块更新");
            }
        }
    }

    private boolean hasLightItem(Player player) {
        PlayerInventory inv = player.getInventory();
        // 检查主手
        if (lightItems.contains(inv.getItemInMainHand().getType())) {
            debugLog("玩家 " + player.getName() + " 主手持有光源物品: " + inv.getItemInMainHand().getType());
            return true;
        }
        // 检查副手
        if (lightItems.contains(inv.getItemInOffHand().getType())) {
            debugLog("玩家 " + player.getName() + " 副手持有光源物品: " + inv.getItemInOffHand().getType());
            return true;
        }
        // 检查铠甲栏
        if (inv.getHelmet() != null && lightItems.contains(inv.getHelmet().getType())) {
            debugLog("玩家 " + player.getName() + " 头盔是光源物品: " + inv.getHelmet().getType());
            return true;
        }
        if (inv.getChestplate() != null && lightItems.contains(inv.getChestplate().getType())) {
            debugLog("玩家 " + player.getName() + " 胸甲是光源物品: " + inv.getChestplate().getType());
            return true;
        }
        if (inv.getLeggings() != null && lightItems.contains(inv.getLeggings().getType())) {
            debugLog("玩家 " + player.getName() + " 护腿是光源物品: " + inv.getLeggings().getType());
            return true;
        }
        if (inv.getBoots() != null && lightItems.contains(inv.getBoots().getType())) {
            debugLog("玩家 " + player.getName() + " 靴子是光源物品: " + inv.getBoots().getType());
            return true;
        }
        debugLog("玩家 " + player.getName() + " 没有持有任何光源物品");
        return false;
    }

    private boolean isLegacyClient(Player player) {
        // 1. 如果是低版本服务器（1.16及以下），直接使用火把
        if (isLegacyVersion) {
            return true;
        }
        
        // 2. 如果是高版本服务器（1.17及以上）
        // 每次都重新检查ViaVersion插件是否存在
        Plugin viaVersion = getServer().getPluginManager().getPlugin("ViaVersion");
        if (viaVersion != null && viaVersion.isEnabled()) {
            try {
                // 2.1 如果安装了ViaVersion，检查玩家协议版本
                com.viaversion.viaversion.api.ViaAPI api = com.viaversion.viaversion.api.Via.getAPI();
                int version = api.getPlayerVersion(player);
                // 1.17的协议版本是755，小于这个的都是老版本
                return version < 755;
            } catch (Exception e) {
                debugLog("获取玩家 " + player.getName() + " 的协议版本时出错: " + e.getMessage());
                // 如果获取版本失败，默认使用光源方块
                return false;
            }
        }
        
        // 2.2 如果没有安装ViaVersion，直接使用光源方块
        return false;
    }

    private void checkAndUpdateLight(Player player) {
        UUID playerId = player.getUniqueId();
        debugLog("检查玩家 " + player.getName() + " 的光源状态");
        
        if (!hasLightItem(player)) {
            // 如果玩家没有拿着指定的物品，移除光源
            if (lightLocations.containsKey(playerId)) {
                debugLog("玩家 " + player.getName() + " 不再持有光源物品，移除假光源方块");
                removeFakeLightBlock(player, lightLocations.get(playerId));
                lightLocations.remove(playerId);
            }
        } else {
            // 如果玩家拿着指定的物品，更新光源位置
            Location currentLocation = player.getLocation();
            debugLog("玩家 " + player.getName() + " 当前位置: " + 
                   "X=" + currentLocation.getX() + 
                   ", Y=" + currentLocation.getY() + 
                   ", Z=" + currentLocation.getZ());
            
            if (lightLocations.containsKey(playerId)) {
                debugLog("移除玩家 " + player.getName() + " 的旧假光源方块");
                removeFakeLightBlock(player, lightLocations.get(playerId));
                lightLocations.remove(playerId);
            }
            
            // 发送假光源方块更新
            sendFakeLightBlock(player, currentLocation);
            // 记录新位置
            lightLocations.put(playerId, currentLocation);
            debugLog("成功在玩家 " + player.getName() + " 位置记录假光源方块");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // 如果玩家移动了位置
        if (to != null && (from.getBlockX() != to.getBlockX() || 
                          from.getBlockY() != to.getBlockY() || 
                          from.getBlockZ() != to.getBlockZ())) {
            if (isLegacyClient(player)) {
                updateTorchPosition(player);
            } else {
                checkAndUpdateLight(player);
            }
        }
    }

    private void updateTorchPosition(Player player) {
        if (!hasLightItem(player)) {
            removeTorch(player);
            return;
        }

        // 获取玩家背后的位置
        Location torchLoc = getTorchLocation(player);
        
        // 如果火把位置发生变化，更新它
        if (!torchLocations.containsKey(player.getUniqueId()) || 
            !torchLocations.get(player.getUniqueId()).equals(torchLoc)) {
            // 移除旧火把
            if (torchLocations.containsKey(player.getUniqueId())) {
                removeTorch(player);
            }
            // 放置新火把
            placeTorch(player);
            torchLocations.put(player.getUniqueId(), torchLoc);
        }
    }

    private Location getTorchLocation(Player player) {
        Location loc = player.getLocation();
        // 获取玩家背后的位置（根据玩家朝向）
        double distance = -1.0; // 火把距离玩家的距离（负数表示在背后）
        double yaw = Math.toRadians(loc.getYaw());
        double x = loc.getX() - Math.sin(yaw) * distance;
        double z = loc.getZ() + Math.cos(yaw) * distance;
        return new Location(loc.getWorld(), x, loc.getY(), z);
    }

    private void placeTorch(Player player) {
        // 检查是否已经有光源
        if (playerTorches.containsKey(player.getUniqueId())) {
            return;
        }
        
        Location torchLoc;
        // 根据玩家版本选择光源类型和位置
        if (isLegacyClient(player)) {
            // 对于1.16及以下的玩家，显示火把在背后一格
            torchLoc = getTorchLocation(player);
            // 使用sendBlockChange发送假火把方块
            player.sendBlockChange(torchLoc, Material.TORCH.createBlockData());
        } else {
            // 对于1.17及以上的玩家，显示光源方块在玩家位置
            torchLoc = player.getLocation();
            // 使用sendBlockChange发送假光源方块
            player.sendBlockChange(torchLoc, Material.LIGHT.createBlockData());
        }
        
        playerTorches.put(player.getUniqueId(), torchLoc);
        debugLog("玩家 " + player.getName() + " 放置了光源");
    }

    private void removeTorch(Player player) {
        Location torchLoc = playerTorches.remove(player.getUniqueId());
        if (torchLoc != null) {
            // 发送真实的方块状态给玩家
            player.sendBlockChange(torchLoc, torchLoc.getBlock().getBlockData());
            debugLog("玩家 " + player.getName() + " 移除了光源");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (isLegacyClient(player)) {
            removeTorch(player);
        } else {
            if (lightLocations.containsKey(playerId)) {
                removeFakeLightBlock(player, lightLocations.get(playerId));
                lightLocations.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isLegacyClient(event.getPlayer())) {
            updateTorchPosition(event.getPlayer());
        } else {
            checkAndUpdateLight(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isLegacyClient(event.getPlayer())) {
            updateTorchPosition(event.getPlayer());
        } else {
            checkAndUpdateLight(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.PLAYER) {
            if (isLegacyClient((Player) event.getWhoClicked())) {
                updateTorchPosition((Player) event.getWhoClicked());
            } else {
                checkAndUpdateLight((Player) event.getWhoClicked());
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (isLegacyClient(event.getPlayer())) {
            updateTorchPosition(event.getPlayer());
        } else {
            checkAndUpdateLight(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (debugMode) {
            if (isLegacyVersion) {
                debugLog("玩家 " + player.getName() + " 加入服务器 - 服务器版本: 1.16及以下，将使用火把");
            } else {
                // 每次都重新检查ViaVersion插件是否存在
                Plugin viaVersion = getServer().getPluginManager().getPlugin("ViaVersion");
                if (viaVersion != null && viaVersion.isEnabled()) {
                    try {
                        com.viaversion.viaversion.api.ViaAPI api = com.viaversion.viaversion.api.Via.getAPI();
                        int version = api.getPlayerVersion(player);
                        debugLog("玩家 " + player.getName() + " 加入服务器 - 协议版本: " + version + 
                               (version < 755 ? " (1.16及以下，将使用火把)" : " (1.17及以上，将使用光源方块)"));
                    } catch (Exception e) {
                        debugLog("玩家 " + player.getName() + " 加入服务器 - 无法获取协议版本，将使用光源方块: " + e.getMessage());
                    }
                } else {
                    debugLog("玩家 " + player.getName() + " 加入服务器 - 服务器版本: 1.17及以上 (未安装ViaVersion)，将使用光源方块");
                }
            }
        }
    }
}