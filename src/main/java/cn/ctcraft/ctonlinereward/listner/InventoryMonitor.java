package cn.ctcraft.ctonlinereward.listner;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.RewardEntity;
import cn.ctcraft.ctonlinereward.database.DataService;
import cn.ctcraft.ctonlinereward.database.YamlData;
import cn.ctcraft.ctonlinereward.inventory.ActionType;
import cn.ctcraft.ctonlinereward.inventory.InventoryFactory;
import cn.ctcraft.ctonlinereward.inventory.MainInventoryHolder;
import cn.ctcraft.ctonlinereward.service.RewardService;
import cn.ctcraft.ctonlinereward.service.RewardStatus;
import cn.ctcraft.ctonlinereward.service.YamlService;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.EconomyResponse;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class InventoryMonitor implements Listener {
    private RewardService rewardService = RewardService.getInstance();
    private CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);
    private InventoryHolder holder = null;

    @EventHandler
    public void InventoryClick(InventoryClickEvent e) {
        Inventory inventory = e.getInventory();
        ctOnlineReward.debug("InventoryClick 事件触发.");
        if (inventory == null || e.getRawSlot() < 0 || e.getClickedInventory() == null) {
            ctOnlineReward.debug("InventoryClick 事件无效 (inventory, rawSlot, 或 clickedInventory 为空).");
            return;
        }
        e.getCursor();
        holder = inventory.getHolder();
        if (!(holder instanceof MainInventoryHolder)) {
            ctOnlineReward.debug("InventoryHolder 不是 MainInventoryHolder.");
            return;
        }

        if (e.getClick().isShiftClick() || e.getClickedInventory().getHolder() instanceof MainInventoryHolder) {
            ctOnlineReward.debug("Shift点击或点击的库存是主库存，取消事件.");
            e.setCancelled(true);
        }

        int rawSlot = e.getRawSlot();
        MainInventoryHolder mainInventoryHolder = (MainInventoryHolder) holder;
        Map<Integer, RewardEntity> statusMap = mainInventoryHolder.statusMap;
        Player player = (Player) e.getWhoClicked();

        if (statusMap.containsKey(rawSlot)) {
            ctOnlineReward.debug("点击了奖励槽位: " + rawSlot);
            rewardExecute(statusMap.get(rawSlot), player);
        }

        Map<Integer, ConfigurationSection> commandMap = mainInventoryHolder.commandMap;
        if (commandMap.containsKey(rawSlot)) {
            ctOnlineReward.debug("点击了命令槽位: " + rawSlot);
            commandExecute(commandMap.get(rawSlot), player);
        }

        Map<Integer, String> guiMap = mainInventoryHolder.guiMap;
        if (guiMap.containsKey(rawSlot)) {
            ctOnlineReward.debug("点击了GUI槽位: " + rawSlot);
            guiExecute(guiMap.get(rawSlot), player);
        }
    }


    private void guiExecute(String gui, Player player) {
        ctOnlineReward.debug("执行 GUI 命令: " + gui + " for player: " + player.getName());
        Inventory build = InventoryFactory.build(gui, player);
        if (build != null) {
            player.openInventory(build);
            ctOnlineReward.debug("成功打开 GUI: " + gui);
        } else {
            ctOnlineReward.debug("无法打开 GUI: " + gui + ", build 为 null.");
        }
    }

    private void commandExecute(ConfigurationSection command, Player player) {
        ctOnlineReward.debug("执行命令: " + command.getName() + " for player: " + player.getName());
        Set<String> keys = command.getKeys(false);

        List<String> playerCommands = keys.contains("PlayerCommands") ? command.getStringList("PlayerCommands") : null;
        List<String> opCommands = keys.contains("OpCommands") ? command.getStringList("OpCommands") : null;
        List<String> consoleCommands = keys.contains("ConsoleCommands") ? command.getStringList("ConsoleCommands") : null;

        if (playerCommands != null) {
            ctOnlineReward.debug("执行玩家命令: " + playerCommands);
            List<String> list = PlaceholderAPI.setPlaceholders(player, playerCommands);
            for (String s : list) {
                player.performCommand(s);
                ctOnlineReward.debug("执行玩家命令: " + s);
            }
        }

        boolean isOp = player.isOp();
        try {
            player.setOp(true);
            if (opCommands != null) {
                ctOnlineReward.debug("执行OP命令: " + opCommands);
                List<String> list1 = PlaceholderAPI.setPlaceholders(player, opCommands);
                for (String c : list1) {
                    player.performCommand(c);
                    ctOnlineReward.debug("执行OP命令: " + c);
                }
            }
        } finally {
            player.setOp(isOp);
        }

        if (consoleCommands != null) {
            ctOnlineReward.debug("执行控制台命令: " + consoleCommands);
            List<String> list2 = PlaceholderAPI.setPlaceholders(player, consoleCommands);
            for (String s : list2) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
                ctOnlineReward.debug("执行控制台命令: " + s);
            }
        }
    }

    private void rewardExecute(RewardEntity rewardEntity, Player player) {
        ctOnlineReward.debug("执行奖励: " + rewardEntity.getRewardID() + " for player: " + player.getName());
        if (rewardEntity.getStatus() != RewardStatus.activation) {
            ctOnlineReward.debug("奖励状态不是激活状态，跳过.");
            return;
        }


            if (holder instanceof MainInventoryHolder) {
                ctOnlineReward.debug("重新打开主菜单: " + ((MainInventoryHolder) holder).inventoryID);
                Inventory build = InventoryFactory.build(((MainInventoryHolder) holder).inventoryID, player);
                if (build != null) {
                    player.openInventory(build);
                } else {
                    ctOnlineReward.debug("无法重新打开主菜单，build 为 null.");
                    return;
                }
            } else {
                ctOnlineReward.debug("Holder 不是 MainInventoryHolder，无法重新打开菜单.");
                return;
            }
        try {
            if (permissionHandler(rewardEntity.getRewardID(), player)) {
                ctOnlineReward.debug("权限检查通过 for reward: " + rewardEntity.getRewardID());

                List<ItemStack> itemStackFromRewardId = rewardService.getItemStackFromRewardId(rewardEntity.getRewardID());
                ctOnlineReward.debug("从奖励ID获取物品堆: " + rewardEntity.getRewardID() + ", 物品数量: " + (itemStackFromRewardId != null ? itemStackFromRewardId.size() : "null"));
                DataService playerDataService = CtOnlineReward.dataService;

                if (!isPlayerInventorySizeEnough(itemStackFromRewardId, player)) {
                    player.sendMessage(CtOnlineReward.languageHandler.getLang("reward.volume").replace("{rewardSize}", String.valueOf(itemStackFromRewardId.size())));
                    player.sendMessage(CtOnlineReward.languageHandler.getLang("reward.volume2"));
                    ctOnlineReward.debug("玩家背包空间不足.");
                    return;
                }

                if (playerDataService.addRewardToPlayData(rewardEntity.getRewardID(), player)) {
                    ctOnlineReward.debug("成功添加奖励数据到玩家数据.");
                    if (itemStackFromRewardId != null) {
                        givePlayerItem(itemStackFromRewardId, player);
                        ctOnlineReward.debug("给予玩家物品.");
                    }

                    executeCommand(rewardEntity.getRewardID(), player);
                    giveMoney(player, rewardEntity.getRewardID());
                    action(rewardEntity.getRewardID(), player);
                }
            } else {
                String lang = CtOnlineReward.languageHandler.getLang("reward.volume3");
                player.sendMessage(lang);
                ctOnlineReward.debug("玩家没有权限领取奖励: " + rewardEntity.getRewardID());
            }
        } catch (Exception ex) {
            ctOnlineReward.getLogger().warning("§c§l■ 奖励配置异常!");
            ctOnlineReward.debug("奖励配置异常: " + ex.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                ex.printStackTrace();
            }
        }
    }


    private boolean permissionHandler(String rewardId, Player player) {
        ctOnlineReward.debug("检查奖励权限 for reward: " + rewardId + ", player: " + player.getName());
        ConfigurationSection rewardSection = YamlData.rewardYaml.getConfigurationSection(rewardId);
        String permission = rewardSection.getString("permission");
        boolean hasPermission = (permission == null || player.hasPermission(permission));
        ctOnlineReward.debug("权限检查结果: " + hasPermission + ", 权限: " + permission);
        return hasPermission;
    }


    private void action(String rewardID, Player player) {
        ctOnlineReward.debug("执行奖励动作 for reward: " + rewardID + ", player: " + player.getName());
        ConfigurationSection rewardSection = YamlData.rewardYaml.getConfigurationSection(rewardID);
        if (!rewardSection.contains("receiveAction")) {
            ctOnlineReward.debug("奖励没有 receiveAction 配置.");
            return;
        }
        List<String> receiveAction = rewardSection.getStringList("receiveAction");
        ctOnlineReward.debug("奖励动作列表: " + receiveAction);
        receiveAction.forEach(action -> actionHandler(action, player, rewardSection));
    }


    private void actionHandler(String actionContent, Player player, ConfigurationSection configurationSection) {
        ctOnlineReward.debug("处理动作: " + actionContent + " for player: " + player.getName());
        ActionType actionType = ActionType.getActionType(actionContent);
        if (actionType == null) {
            ctOnlineReward.debug("未知动作类型: " + actionContent);
            return;
        }

        switch (actionType) {
            case sound:
                String soundText = actionContent.replace("[sound]", "").replace(" ", "");
                try {
                    Sound sound = Sound.valueOf(soundText);
                    player.playSound(player.getLocation(), sound, 1, 1);
                    ctOnlineReward.debug("播放音效: " + soundText);
                } catch (IllegalArgumentException e) {
                    ctOnlineReward.debug("无效音效名称: " + soundText + ". 错误: " + e.getMessage());
                }
                break;
            case Message:
                String messageText = actionContent.replace("[Message]", "").replace(" ", "").replace("&", "§");
                int moneyNum = configurationSection.getInt("economy.money");
                messageText = messageText.replace("{money}", String.valueOf(moneyNum));
                int pointsNum = configurationSection.getInt("economy.points");
                messageText = messageText.replace("{points}", String.valueOf(pointsNum));
                player.sendMessage(messageText);
                ctOnlineReward.debug("发送消息: " + messageText);
                break;
            case closeGUI:
                player.closeInventory();
                ctOnlineReward.debug("关闭 GUI.");
                break;
            case openGUI:
                String guiText = actionContent.replace("[openGUI]", "").replace(" ", "");
                ctOnlineReward.debug("打开 GUI: " + guiText);
                Inventory build = InventoryFactory.build(guiText, player);
                if (build != null) {
                    player.openInventory(build);
                    ctOnlineReward.debug("成功打开 GUI: " + guiText);
                } else {
                    ctOnlineReward.debug("无法打开 GUI: " + guiText + ", build 为 null.");
                }
                break;
        }
    }

    private void giveMoney(Player player, String rewardID) {
        ctOnlineReward.debug("给予金钱/点券 for reward: " + rewardID + ", player: " + player.getName());
        ConfigurationSection rewardSection = YamlData.rewardYaml.getConfigurationSection(rewardID);
        if (!rewardSection.contains("economy")) {
            ctOnlineReward.debug("奖励没有 economy 配置.");
            return;
        }
        ConfigurationSection economy = rewardSection.getConfigurationSection( "economy");
        if (economy.contains("money")) {
            double money = economy.getDouble("money");
            CtOnlineReward.economy.depositPlayer(player, money);
            ctOnlineReward.debug("给予玩家金钱: " + money);
        }
        if (economy.contains("points")) {
            int points = economy.getInt("points");
            try {
                PlayerPointsAPI playerPointsAPI = new PlayerPointsAPI(ctOnlineReward.getPlayerPoints());
                playerPointsAPI.give(player.getUniqueId(), points);
                ctOnlineReward.debug("给予玩家点券: " + points);
            } catch (NoClassDefFoundError e) {
                ctOnlineReward.debug("未找到点券插件,请勿在配置文件(reward.yml)中配置点券项，如果需要使用点券请安装PlayerPoints. 错误: " + e.getMessage());
            }
        }
    }

    private void executeCommand(String rewardID, Player player) {
        ctOnlineReward.debug("执行命令 for reward: " + rewardID + ", player: " + player.getName());
        ConfigurationSection rewardSection = YamlData.rewardYaml.getConfigurationSection(rewardID);
        if (!rewardSection.contains("command")) {
            ctOnlineReward.debug("奖励没有 command 配置.");
            return;
        }
        ConfigurationSection command = rewardSection.getConfigurationSection("command");
        List<String> playerCommands = command.getStringList("PlayerCommands");
        if (!playerCommands.isEmpty()) {
            ctOnlineReward.debug("执行玩家命令: " + playerCommands);
            List<String> list = PlaceholderAPI.setPlaceholders(player, playerCommands);
            for (String s : list) {
                player.performCommand(s);
                ctOnlineReward.debug("执行玩家命令: " + s);
            }
        }
        List<String> opCommands = command.getStringList("OpCommands");
        if (!opCommands.isEmpty()) {
            ctOnlineReward.debug("执行OP命令: " + opCommands);
            List<String> list1 = PlaceholderAPI.setPlaceholders(player, opCommands);
            boolean isOp = player.isOp();
            try {
                player.setOp(true);
                for (String c : list1) {
                    player.performCommand(c);
                    ctOnlineReward.debug("执行OP命令: " + c);
                }
            } finally {
                player.setOp(isOp);
            }
        }

        List<String> consoleCommands = command.getStringList("ConsoleCommands");
        if (!consoleCommands.isEmpty()) {
            ctOnlineReward.debug("执行控制台命令: " + consoleCommands);
            List<String> list2 = PlaceholderAPI.setPlaceholders(player, consoleCommands);
            for (String s : list2) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
                ctOnlineReward.debug("执行控制台命令: " + s);
            }
        }
    }

    private boolean givePlayerItem(List<ItemStack> list, Player player) {
        ctOnlineReward.debug("给予玩家物品: " + list.size() + " items for player: " + player.getName());
        if (!isPlayerInventorySizeEnough(list, player)) {
            ctOnlineReward.debug("玩家背包空间不足，无法给予物品.");
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        list.forEach(itemStack -> {
            if (itemStack != null) {
                inventory.addItem(itemStack);
                ctOnlineReward.debug("给予物品: " + itemStack.getType().name() + " x " + itemStack.getAmount());
            } else {
                ctOnlineReward.debug("尝试给予空物品.");
            }
        });

        return true;
    }


    private boolean isPlayerInventorySizeEnough(List<ItemStack> itemStacks, Player player) {
        ctOnlineReward.debug("检查玩家背包空间 for player: " + player.getName() + ", 物品数量: " + itemStacks.size());
        PlayerInventory inventory = player.getInventory();
        int emptySize = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                emptySize++;
            }
        }
        ctOnlineReward.debug("玩家背包空槽位: " + emptySize);

        // 创建一个可修改的副本，以避免UnsupportedOperationException
        List<ItemStack> mutableItemStacks = new ArrayList<>(itemStacks);
        mutableItemStacks.removeIf(Objects::isNull);

        return mutableItemStacks.size() <= emptySize;
    }

}
