package cn.ctcraft.ctonlinereward.service;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.database.YamlData;
import cn.ctcraft.ctonlinereward.pojo.RewardData;
import cn.ctcraft.ctonlinereward.utils.SerializableUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class RewardService {
    private static final RewardService instance = new RewardService();
    CtOnlineReward ctOnlineReward;

    private RewardService() {
        ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    }

    public static RewardService getInstance() {
        return instance;
    }

    public List<ItemStack> getItemStackFromRewardId(String rewardId) {
        ctOnlineReward.debug("从奖励ID获取物品堆: " + rewardId);
        YamlConfiguration rewardYaml = YamlData.rewardYaml;
        if (!rewardYaml.contains(rewardId)) {
            ctOnlineReward.debug("奖励ID不存在: " + rewardId);
            return null;
        }
        ConfigurationSection rewardIdYaml = rewardYaml.getConfigurationSection(rewardId);
        if (!rewardIdYaml.contains("rewardData")) {
            ctOnlineReward.debug("奖励ID没有 rewardData 配置: " + rewardId);
            return null;
        }
        String rewardDataPath = rewardIdYaml.getString("rewardData");
        ctOnlineReward.debug("奖励数据路径: " + rewardDataPath);
        File rewardDataFile = new File(ctOnlineReward.getDataFolder(), "rewardData/" + rewardDataPath);
        return getItemStackFromFile(rewardDataFile);
    }

    public List<ItemStack> getItemStackFromFile(File file) {
        ctOnlineReward.debug("从文件读取物品堆: " + file.getAbsolutePath());
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bFile = new byte[(int) file.length()];
            fileInputStream.read(bFile);
            SerializableUtil serializableUtil = new SerializableUtil();
            RewardData rewardData = serializableUtil.singleObjectFromByteArray(bFile, RewardData.class);
            fileInputStream.close();
            ctOnlineReward.debug("成功从文件读取奖励数据.");
            return rewardData.getRewardList();
        } catch (FileNotFoundException e) {
            String message = e.getMessage();
            if (message.contains("系统找不到指定的文件")) {
                int startIndex = 34;
                int endIndex = message.indexOf("(系统找不到指定的文件。)");
                String rewardDataName = message.substring(startIndex, endIndex);
                ctOnlineReward.debug("找不到奖励数据文件: " + rewardDataName + ". 错误: " + e.getMessage());
                ctOnlineReward.getLogger().warning("§c§l■ 找不到奖励数据!");
                ctOnlineReward.getLogger().warning("§c§l■ 请使用/cor reward set " + rewardDataName + "设置奖励数据!");
            } else {
                ctOnlineReward.debug("文件未找到异常: " + e.getMessage());
            }
        } catch (Exception e) {
            ctOnlineReward.debug("奖励数据读取失败: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
            ctOnlineReward.getLogger().warning("§c§l■ 奖励数据读取失败!");
        }
        return null;
    }


    public boolean saveRewardData(RewardData rewardData, String reward) {
        ctOnlineReward.debug("保存奖励数据: " + reward);
        try {
            byte[] rewardDataBytes = getRewardDataBytes(rewardData);
            File file = new File(ctOnlineReward.getDataFolder(), "rewardData/" + reward);
            if (!file.exists()) {
                ctOnlineReward.debug("奖励数据文件不存在，尝试创建: " + file.getAbsolutePath());
                boolean newFile = file.createNewFile();
                if (newFile) {
                    ctOnlineReward.getLogger().info("§a§l● 奖励数据创建成功，奖励名为" + reward);
                    ctOnlineReward.debug("奖励数据文件创建成功.");
                } else {
                    ctOnlineReward.getLogger().warning("§c§l■ 奖励数据创建失败，奖励名为" + reward);
                    ctOnlineReward.debug("奖励数据文件创建失败.");
                }
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(rewardDataBytes);
            fileOutputStream.close();
            ctOnlineReward.debug("奖励数据保存成功.");
            return true;
        } catch (Exception e) {
            ctOnlineReward.debug("奖励数据保存失败: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
            ctOnlineReward.getLogger().warning("§c§l■ 奖励数据保存失败!");
        }
        return false;
    }

    public byte[] getRewardDataBytes(RewardData rewardData) throws IOException {
        SerializableUtil serializableUtil = new SerializableUtil();
        return serializableUtil.singleObjectToByteArray(rewardData);
    }

    public boolean initRewardFile(){
        ItemStack itemStack = new ItemStack(Material.APPLE);
        RewardData rewardData = new RewardData(Collections.singletonList(itemStack));
        return saveRewardData(rewardData,"10min");
    }


}
