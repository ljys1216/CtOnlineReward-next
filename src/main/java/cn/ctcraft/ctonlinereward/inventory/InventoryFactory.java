package cn.ctcraft.ctonlinereward.inventory;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.RewardEntity;
import cn.ctcraft.ctonlinereward.database.DataService;
import cn.ctcraft.ctonlinereward.database.YamlData;
import cn.ctcraft.ctonlinereward.service.RewardStatus;
import cn.ctcraft.ctonlinereward.service.YamlService;
import cn.ctcraft.ctonlinereward.service.rewardHandler.RewardOnlineTimeHandler;
import cn.ctcraft.ctonlinereward.utils.ItemUtils;
import cn.ctcraft.ctonlinereward.utils.Position;
import cn.ctcraft.ctonlinereward.utils.Util;
import com.cryptomorin.xseries.XMaterial;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class InventoryFactory {
    private Player player;
    private final Set<RewardEntity> rewardSet = new LinkedHashSet<>();
    private final MainInventoryHolder mainInventoryHolder = new MainInventoryHolder();
    private final YamlService yamlService = YamlService.getInstance();
    private final CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public static Inventory build(String inventoryId, Player player) {
        return new InventoryFactory().getInventory(inventoryId, player);
    }

    /**
     * 抛弃正则匹配写法，按理说这样更高效
     *
     * @param str 字符串
     * @return 是否是数字
     */
    private static boolean isInteger(String str) {
        int length = str.length();
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (i == 0 && (ch == '-' || ch == '+')) {
                continue;
            }
            if (!Character.isDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    private Inventory getInventory(String inventoryId, Player player) {
        this.player = player;
        Map<String, YamlConfiguration> guiYaml = YamlData.guiYaml;

        if (!guiYaml.containsKey(inventoryId)) {
            player.sendMessage("§c§l菜单不存在!");
            player.closeInventory();
            return null;
        }

        YamlConfiguration yamlConfiguration = guiYaml.get(inventoryId);
        String name = yamlConfiguration.getString("name");
        int size = yamlConfiguration.getInt("slot");
        Inventory inventory = Bukkit.createInventory(mainInventoryHolder, size, name.replace("&", "§"));
        addItemStack(inventory, yamlConfiguration);
        mainInventoryHolder.inventoryID = inventoryId;
        ctOnlineReward.debug("GUI " + inventoryId + " 已创建，大小: " + size + ", 名称: " + name);
        return inventory;
    }

    private void addItemStack(Inventory inventory, YamlConfiguration guiYaml) {
        ctOnlineReward.debug("向 GUI 添加物品堆.");
        ConfigurationSection values = guiYaml.getConfigurationSection("values");
        if (values == null) {
            ctOnlineReward.debug("GUI 配置中没有 'values' 部分.");
            return;
        }

        Set<String> keys = values.getKeys(false);

        for (String key : keys) {
            ctOnlineReward.debug("处理物品配置键: " + key);
            ConfigurationSection value = values.getConfigurationSection(key);
            ItemStack valueItemStack = getValueItemStack(value);
            ctOnlineReward.debug("获取物品堆: " + (valueItemStack != null ? valueItemStack.getType().name() : "null"));

            if (valueItemStack == null) {
                continue;
            }

            Set<String> keys1 = value.getKeys(false);

            if (keys1.contains("index") || value.contains("slot")) { // 检查 index 或 slot
                List<Integer> indexs = getIndexList(value);
                ctOnlineReward.debug("获取到的物品位置 (indexs): " + indexs);

                for (Integer integer : indexs) {
                    inventory.setItem(integer, valueItemStack);
                    ctOnlineReward.debug("在槽位 " + integer + " 设置物品: " + (valueItemStack != null ? valueItemStack.getType().name() : "null"));
                }

                if (keys1.contains("mode")) {
                    String mode = value.getString("mode");
                    ctOnlineReward.debug("物品模式: " + mode);

                    if (mode.equalsIgnoreCase("reward")) {
                        String rewardId = value.getString("rewardId");
                        if (rewardId == null || rewardId.isEmpty()){
                            ctOnlineReward.debug("配置错误，没有找到对应的rewardId  错误位置:"+key);
                        }
                        handleRewardMode(rewardId, indexs);
                    } else if (mode.equalsIgnoreCase("command")) {
                        handleCommandMode(value, indexs);
                    } else if (mode.equalsIgnoreCase("gui")) {
                        handleGuiMode(value, indexs);
                    }

                    handleModeMap(mode, indexs);
                }
            }
        }
    }

    private List<Integer> getIndexList(ConfigurationSection value) {
        List<Integer> indexs = new ArrayList<>();
        
        ctOnlineReward.debug("进入 getIndexList 方法.");
        // 优先检查 'slot' 字段
        if (value.contains("slot")) {
            ctOnlineReward.debug("检测到 slot 字段.");
            Object slotObject = value.get("slot");
            ctOnlineReward.debug("slot 字段的原始值: " + slotObject + ", 类型: " + (slotObject != null ? slotObject.getClass().getName() : "null"));

            if (slotObject instanceof List) {
                List<?> slotList = (List<?>) slotObject;
                ctOnlineReward.debug("slot 字段是列表.");
                for (Object item : slotList) {
                    if (item instanceof Integer) {
                        indexs.add((Integer) item);
                        ctOnlineReward.debug("添加列表中的整数槽位: " + item);
                    } else if (item instanceof String) {
                        try {
                            int parsedSlot = Integer.parseInt((String) item);
                            indexs.add(parsedSlot);
                            ctOnlineReward.debug("添加列表中的字符串解析整数槽位: " + parsedSlot);
                        } catch (NumberFormatException e) {
                            ctOnlineReward.debug("无法解析列表中的字符串槽位为整数: " + item + ". 错误: " + e.getMessage());
                        }
                    } else {
                        ctOnlineReward.debug("列表中包含非整数/字符串类型: " + (item != null ? item.getClass().getName() : "null"));
                    }
                }
                if (!indexs.isEmpty()) {
                    ctOnlineReward.debug("从 slot 列表解析到槽位: " + indexs);
                    return indexs;
                }
            } else if (slotObject != null) { // 处理单个值，无论是 Integer 还是 String
                try {
                    int parsedSlot = Integer.parseInt(String.valueOf(slotObject));
                    indexs.add(parsedSlot);
                    ctOnlineReward.debug("添加单个解析整数槽位: " + parsedSlot);
                    return indexs;
                } catch (NumberFormatException e) {
                    ctOnlineReward.debug("无法解析单个槽位为整数: " + slotObject + ". 错误: " + e.getMessage());
                }
            }
        }

        // 如果没有 'slot' 或者 'slot' 不是有效整数/列表，则回退到 'index' 逻辑
        ctOnlineReward.debug("回退到 index 字段逻辑.");
        Object index = value.get("index");
        ctOnlineReward.debug("index 字段的原始值: " + index + ", 类型: " + (index != null ? index.getClass().getName() : "null"));

        if (index instanceof Integer) {
            indexs.add((Integer) index);
            ctOnlineReward.debug("添加单个整数 index: " + index);
        } else {
            String x = value.getString("index.x");
            String y = value.getString("index.y");
            ctOnlineReward.debug("解析 x: " + x + ", y: " + y + " 为坐标.");
            indexs = Position.get(x, y);
            ctOnlineReward.debug("添加坐标解析的 indexs: " + indexs);
        }

        return indexs;
    }

    private void handleRewardMode(String rewardId, List<Integer> indexs) {
        ctOnlineReward.debug("处理奖励模式 for rewardId: " + rewardId + ", indexs: " + indexs);
        RewardEntity rewardEntity = rewardSet.stream().filter(it -> it.getRewardID().equals(rewardId)).findFirst().orElse(null);
        if (rewardEntity == null) {
            ctOnlineReward.debug("未找到奖励实体 for rewardId: " + rewardId);
            return;
        }
        Map<Integer, RewardEntity> statusMap = mainInventoryHolder.statusMap;
        for (Integer integer : indexs) {
            statusMap.put(integer, rewardEntity);
            ctOnlineReward.debug("将奖励实体 " + rewardId + " 映射到槽位: " + integer);
        }
    }

    private void handleCommandMode(ConfigurationSection value, List<Integer> indexs) {
        ctOnlineReward.debug("处理命令模式 for indexs: " + indexs);
        ConfigurationSection configurationSection = getItemStackCommand(value);
        for (Integer integer : indexs) {
            mainInventoryHolder.commandMap.put(integer, configurationSection);
            ctOnlineReward.debug("将命令配置映射到槽位: " + integer);
        }
    }

    private void handleGuiMode(ConfigurationSection value, List<Integer> indexs) {
        ctOnlineReward.debug("处理 GUI 模式 for indexs: " + indexs);
        if (value.contains("gui")) {
            String gui = value.getString("gui");
            for (Integer integer : indexs) {
                mainInventoryHolder.guiMap.put(integer, gui);
                ctOnlineReward.debug("将 GUI " + gui + " 映射到槽位: " + integer);
            }
        }
    }

    private void handleModeMap(String mode, List<Integer> indexs) {
        ctOnlineReward.debug("处理模式映射: " + mode + ", indexs: " + indexs);
        Map<Integer, String> modeMap = mainInventoryHolder.modeMap;

        for (Integer integer : indexs) {
            modeMap.put(integer, mode);
        }
    }

    private ConfigurationSection getItemStackCommand(ConfigurationSection value) {
        ctOnlineReward.debug("获取物品堆命令配置.");
        if (!value.contains("command")) {
            ctOnlineReward.debug("物品配置中没有 'command' 部分.");
            return null;
        }
        return value.getConfigurationSection("command");
    }

    private ItemStack getValueItemStack(ConfigurationSection value) {
        ctOnlineReward.debug("获取值物品堆.");
        ItemStack itemStack = getItemStackType(null, value);
        ctOnlineReward.debug("获取物品堆类型: " + (itemStack != null ? itemStack.getType().name() : "null"));
        ItemMeta itemMeta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(itemStack.getType());
        itemMetaHandler(value, itemMeta);
        itemStack.setItemMeta(itemMeta);
        ctOnlineReward.debug("物品堆元数据已设置.");

        if (value.contains("mode")) {
            String mode = value.getString("mode");
            ctOnlineReward.debug("物品堆模式: " + mode);
            if (mode.equalsIgnoreCase("reward")) {
                itemStack = extendHandler(itemStack, value, value.getString("rewardId"));
            }
        }

        return itemStack;
    }

    private ItemStack extendHandler(ItemStack itemStack, ConfigurationSection value, String rewardId) {
        ctOnlineReward.debug("处理扩展物品堆 for rewardId: " + rewardId);
        RewardStatus rewardStatus = getRewardStatus(player, rewardId);
        ctOnlineReward.debug("奖励状态: " + rewardStatus.name());
        RewardEntity rewardEntity = new RewardEntity(rewardId, rewardStatus);

        if (!value.contains("extend")) {
            ctOnlineReward.debug("物品配置中没有 'extend' 部分.");
            rewardSet.add(rewardEntity);
            return itemStack;
        }

        ConfigurationSection extend = value.getConfigurationSection("extend");
        ConfigurationSection targetSection = null;
        ctOnlineReward.debug("扩展配置: " + (extend != null ? extend.getName() : "null"));

        switch (rewardStatus) {
            case before:
                targetSection = extend.getConfigurationSection("before");
                break;
            case after:
                targetSection = extend.getConfigurationSection("after");
                break;
            case activation:
                targetSection = extend.getConfigurationSection("activation");
                break;
        }

        if (targetSection != null) {
            ctOnlineReward.debug("找到目标扩展部分: " + targetSection.getName());
            itemStack = getItemStackType(itemStack,targetSection);
            ctOnlineReward.debug("获取扩展物品堆类型: " + (itemStack != null ? itemStack.getType().name() : "null"));
            ItemMeta itemMeta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(itemStack.getType());
            itemMetaHandler(targetSection, itemMeta);
            itemStack.setItemMeta(itemMeta);
            ctOnlineReward.debug("扩展物品堆元数据已设置.");
        } else {
            ctOnlineReward.debug("未找到目标扩展部分.");
        }

        rewardSet.add(rewardEntity);
        return itemStack;
    }

    private ItemStack getItemStackType(ItemStack itemStack, ConfigurationSection config) {
        ctOnlineReward.debug("获取物品堆类型.");
        CtOnlineReward plugin = CtOnlineReward.getPlugin(CtOnlineReward.class);
        if (!config.contains("type")) {
            ctOnlineReward.debug("配置中没有 'type' 部分，使用默认物品类型.");
            config = plugin.getConfig().getConfigurationSection("Setting.defaultItemType");
        }

        String type = config.getString("type.name", "chest");
        ctOnlineReward.debug("物品类型名称: " + type);
        if (type.equalsIgnoreCase("skull")) {
            String skull = config.getString("type.skull");
            ctOnlineReward.debug("物品类型是 skull，头颅名称: " + skull);
            itemStack = ItemUtils.createSkull(skull);
        } else {
            itemStack = getItemStackByNMS(type);
        }

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            ctOnlineReward.debug("物品堆为 null 或 AIR，设置为 CHEST.");
            itemStack = new ItemStack(Material.CHEST);
        }

        boolean enchantment = config.getBoolean("type.enchantment");
        ctOnlineReward.debug("附魔状态: " + enchantment);
        if (enchantment) {
            itemStack.addUnsafeEnchantment(Enchantment.getByKey(NamespacedKey.minecraft("unbreaking")), 1);
            ctOnlineReward.debug("添加附魔: Unbreaking 1.");
        } else {
            itemStack.removeEnchantment(Enchantment.getByKey(NamespacedKey.minecraft("unbreaking")));
            ctOnlineReward.debug("移除附魔: Unbreaking.");
        }

        return itemStack;
    }

    private ItemStack getItemStackByNMS(String name) {
        ctOnlineReward.debug("通过 NMS 获取物品堆: " + name);
        String nName = name.toLowerCase();
        if (isInteger(nName)) {
            ctOnlineReward.debug("物品名称是整数，尝试通过 XMaterial 解析.");
            return new ItemStack(XMaterial.matchXMaterial(nName).get().parseMaterial());
        }

        if (nName.startsWith("minecraft:")) {
            String materialName = nName.substring(10).toUpperCase();
            ctOnlineReward.debug("物品名称是 minecraft: 前缀，尝试通过 XMaterial 解析: " + materialName);
            Material material = XMaterial.matchXMaterial(materialName).get().parseMaterial();
            if (material == null){
                ctOnlineReward.debug("解析材质为 null.");
                return null;
            }else{
                ctOnlineReward.debug("解析材质成功: " + material.name());
                return new ItemStack(material);
            }
        }

        String versionString = Util.getVersionString();
        ctOnlineReward.debug("获取服务器版本字符串: " + versionString);
        try {
            String className = "net.minecraft.server." + versionString + ".";
            Class<?> itemStackClass = Class.forName(className + "ItemStack");
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + versionString + ".inventory.CraftItemStack");
            ctOnlineReward.debug("成功加载 ItemStack 和 CraftItemStack 类.");

            Class<?> itemClass;
            try {
                itemClass = Class.forName(className + "Item");
            } catch (ClassNotFoundException e) {
                itemClass = Class.forName(className + "IRegistry").getField("ITEM").get(null).getClass();
            }

            Object invoke;
            try {
                Method b = itemClass.getMethod("b", String.class);
                invoke = b.invoke(itemClass, nName);
                ctOnlineReward.debug("通过方法 'b' 获取 NMS 物品.");
            } catch (NoSuchMethodException e) {
                ctOnlineReward.debug("方法 'b' 不存在，尝试通过 IRegistry 获取 NMS 物品.");
                Object itemRegistry = Class.forName(className + "IRegistry").getField("ITEM").get(null);
                Object key = Class.forName(className + "MinecraftKey").getConstructor(String.class).newInstance(nName);
                Method getMethod = itemRegistry.getClass().getMethod("get", Class.forName(className + "MinecraftKey"));
                invoke = getMethod.invoke(itemRegistry, key);
                ctOnlineReward.debug("通过 IRegistry 获取 NMS 物品成功.");
            }

            Constructor<?> itemStackConstructor;
            try {
                itemStackConstructor = itemStackClass.getDeclaredConstructor(itemClass);
                ctOnlineReward.debug("获取 ItemStack 构造函数 (Item).");
            } catch (NoSuchMethodException e) {
                ctOnlineReward.debug("ItemStack 构造函数 (Item) 不存在，尝试获取 (IMaterial).");
                itemStackConstructor = itemStackClass.getDeclaredConstructor(Class.forName(className + "IMaterial"));
                ctOnlineReward.debug("获取 ItemStack 构造函数 (IMaterial) 成功.");
            }

            Object nmsItemStack = itemStackConstructor.newInstance(invoke);
            Method asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", itemStackClass);
            ItemStack finalItemStack = (ItemStack) asBukkitCopy.invoke(craftItemStack, nmsItemStack);
            ctOnlineReward.debug("成功通过 NMS 获取物品堆: " + (finalItemStack != null ? finalItemStack.getType().name() : "null"));
            return finalItemStack;
        } catch (Exception e) {
            ctOnlineReward.debug("通过 NMS 获取物品堆时发生错误! 错误的材质名称: " + name + ". 错误: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
            throw new RuntimeException("GUI物品材质名称配置错误! 错误的材质名称: " + name, e);
        }
    }

    private void itemMetaHandler(ConfigurationSection config, ItemMeta itemMeta) {
        ctOnlineReward.debug("处理物品元数据.");
        if (config.contains("name")) {
            String name = config.getString("name").replace("&", "§");
            String s = PlaceholderAPI.setPlaceholders(player, name);
            itemMeta.setDisplayName(s);
            ctOnlineReward.debug("设置物品显示名称: " + s);
        }
        if (config.contains("lore")) {
            List<String> lore = config.getStringList("lore");
            lore.replaceAll(line -> line.replace("&", "§"));
            List<String> processedLore = PlaceholderAPI.setPlaceholders(player, lore);
            itemMeta.setLore(processedLore);
            ctOnlineReward.debug("设置物品 Lore: " + processedLore);
        }
        if (config.contains("customModelData")) {

            int customModelData = config.getInt("customModelData");
            ctOnlineReward.debug("设置 CustomModelData: " + customModelData);
            try {
                Method setCustomModelData = itemMeta.getClass().getMethod("setCustomModelData", Integer.class);
                setCustomModelData.setAccessible(true);
                setCustomModelData.invoke(itemMeta, customModelData);
                ctOnlineReward.debug("CustomModelData 设置成功.");
            } catch (Exception e) {
                ctOnlineReward.debug("Failed to set CustomModelData for the item! (Unsupported in this version). 错误: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        if (itemMeta instanceof SkullMeta && config.contains("skull")) {
            String skull = config.getString("skull");
            ctOnlineReward.debug("设置头颅所有者: " + skull);
            boolean setOwnerSuccess = ((SkullMeta) itemMeta).setOwner(skull);
            if (!setOwnerSuccess) {
                ctOnlineReward.debug("头颅读取失败！");
            } else {
                ctOnlineReward.debug("头颅所有者设置成功.");
            }
        }
    }

    private RewardStatus getRewardStatus(Player player, String rewardId) {
        ctOnlineReward.debug("获取奖励状态 for player: " + player.getName() + ", rewardId: " + rewardId);
        ConfigurationSection configurationSection = YamlData.rewardYaml.getConfigurationSection(rewardId);
        if (configurationSection == null) {
            ctOnlineReward.debug("未找到奖励配置 " + rewardId + " 请检查reward.yml配置文件中是否有指定配置!");
            return RewardStatus.before;
        }
        if (!configurationSection.contains("time")) {
            ctOnlineReward.debug("奖励配置中没有 'time' 字段.");
            return RewardStatus.before;
        }
        boolean timeIsOk = RewardOnlineTimeHandler.getInstance().onlineTimeIsOk(player, configurationSection.getString("time"));
        ctOnlineReward.debug("在线时间检查结果: " + timeIsOk);
        if (!timeIsOk) {
            return RewardStatus.before;
        }
        List<String> playerRewardArray = CtOnlineReward.dataService.getPlayerRewardArray(player);
        ctOnlineReward.debug("玩家已领取奖励列表: " + playerRewardArray);
        if (playerRewardArray.isEmpty() || !playerRewardArray.contains(rewardId)) {
            ctOnlineReward.debug("奖励状态: activation (未领取或列表为空).");
            return RewardStatus.activation;
        }
        ctOnlineReward.debug("奖励状态: after (已领取).");
        return RewardStatus.after;
    }


}
