package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.utils.Util;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class YamlBase implements DataService {
    private final CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);
    private AbstractMap.Entry<String,YamlConfiguration> yamlDataPair = null;
    private final ReadWriteLock readWriteLock=new ReentrantReadWriteLock(true);
    private final Lock readLock=readWriteLock.readLock();
    private final Lock writeLock=readWriteLock.writeLock();

    public AbstractMap.Entry<String,YamlConfiguration> getYamlData(){
        ctOnlineReward.debug("进入 getYamlData 方法.");
        readLock.lock();
        Map.Entry<String,YamlConfiguration> pastYamlDataPair=null;
        try {
            File dataFolder = new File(ctOnlineReward.getDataFolder() + "/playerData");
            if (!dataFolder.exists()) {
                ctOnlineReward.debug("玩家数据文件夹不存在，尝试创建: " + dataFolder.getAbsolutePath());
                boolean mkdir = dataFolder.mkdir();
                if (mkdir) {
                    ctOnlineReward.getLogger().info("§a§l● 玩家数据文件夹构建成功!");
                    ctOnlineReward.debug("玩家数据文件夹创建成功.");
                } else {
                    ctOnlineReward.getLogger().warning("§c§l■ 玩家数据文件夹构建失败!");
                    ctOnlineReward.debug("玩家数据文件夹创建失败.");
                    return null;
                }
            }
            String date = Util.getDate();
            ctOnlineReward.debug("当前日期: " + date);
            //比较日期，未初始化或日期已更新即进行初始化并保存数据
            if (yamlDataPair == null || !yamlDataPair.getKey().equals(date)) {
                ctOnlineReward.debug("yamlDataPair 未初始化或日期已更新，进行初始化.");
                File file = new File(ctOnlineReward.getDataFolder() + "/playerData/" + date + ".yml");
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                if (!file.exists()) {
                    ctOnlineReward.debug("玩家数据文件不存在，尝试创建: " + file.getAbsolutePath());
                    try {
                        boolean newFile = file.createNewFile();
                        if (!newFile) {
                            ctOnlineReward.getLogger().warning("§c§l■ 玩家数据创建失败!");
                            ctOnlineReward.debug("玩家数据文件创建失败.");
                        } else {
                            ctOnlineReward.debug("玩家数据文件创建成功.");
                        }
                    } catch (IOException e) {
                        ctOnlineReward.debug("创建玩家数据文件时发生IO异常: " + e.getMessage());
                        if (ctOnlineReward.isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    ctOnlineReward.debug("玩家数据文件已存在，加载: " + file.getAbsolutePath());
                    try {
                        yamlConfiguration.load(file);
                        ctOnlineReward.debug("玩家数据文件加载成功.");
                    } catch (Exception e) {
                        ctOnlineReward.debug("加载玩家数据文件时发生异常: " + e.getMessage());
                        if (ctOnlineReward.isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                }
                if (yamlDataPair != null) {
                    //避免死锁 在锁外保存数据
                    pastYamlDataPair=yamlDataPair;
                    ctOnlineReward.debug("旧的 yamlDataPair 存在，将在锁外保存.");
                }
                yamlDataPair = new AbstractMap.SimpleEntry<>(date, yamlConfiguration);
                ctOnlineReward.debug("yamlDataPair 更新为: " + date);
            } else {
                ctOnlineReward.debug("yamlDataPair 已是最新，无需更新.");
            }
        }finally {
            readLock.unlock();
            ctOnlineReward.debug("读锁已释放.");
            //避免死锁 在锁外保存数据
            if(pastYamlDataPair!=null){
                saveData(pastYamlDataPair);
                ctOnlineReward.debug("旧的 yamlDataPair 已保存.");
            }
        }
        ctOnlineReward.debug("返回 yamlDataPair.");
        return yamlDataPair;
    }

    private void saveData(Map.Entry<String,YamlConfiguration> yamlDataPair){
        ctOnlineReward.debug("进入 saveData 方法 for: " + yamlDataPair.getKey());
        String s = yamlDataPair.getKey();
        File file = new File(ctOnlineReward.getDataFolder() + "/playerData/" + s + ".yml");
        writeLock.lock();
        try {
            yamlDataPair.getValue().save(file);
            ctOnlineReward.debug("数据保存成功到文件: " + file.getAbsolutePath());
        }catch (Exception e){
            ctOnlineReward.debug("保存数据到文件失败: " + file.getAbsolutePath() + ". 错误: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }finally {
            writeLock.unlock();
            ctOnlineReward.debug("写锁已释放.");
        }
    }


    public int getPlayerOnlineTime(OfflinePlayer pLayer){
        ctOnlineReward.debug("获取玩家在线时间 for player: " + pLayer.getName());
        Map.Entry<String,YamlConfiguration> playerData = getYamlData();
        int time = playerData.getValue().getInt(pLayer.getUniqueId().toString()+".time");
        ctOnlineReward.debug("玩家在线时间: " + time);
        return time;
    }

    public void addPlayerOnlineTime(OfflinePlayer player,int time){
        ctOnlineReward.debug("增加玩家在线时间 for player: " + player.getName() + ", time: " + time);
        Map.Entry<String,YamlConfiguration> playerData = getYamlData();
        playerData.getValue().set(player.getUniqueId().toString()+".time",time);
        saveData(playerData);
        ctOnlineReward.debug("玩家在线时间已更新并保存.");
    }

    @Override
    public void insertPlayerOnlineTime(OfflinePlayer player,int time) {
        ctOnlineReward.debug("插入玩家在线时间 for player: " + player.getName() + ", time: " + time);
        Map.Entry<String,YamlConfiguration> playerData = getYamlData();
        playerData.getValue().set(player.getUniqueId().toString()+".time",time);
        saveData(playerData);
        ctOnlineReward.debug("玩家在线时间已插入并保存.");
    }

    public List<String> getPlayerRewardArray(OfflinePlayer player){
        ctOnlineReward.debug("获取玩家奖励数组 for player: " + player.getName());
        Map.Entry<String,YamlConfiguration> playerDataPair = getYamlData();
        List<String> rewardList = playerDataPair.getValue().getStringList(player.getUniqueId().toString() + ".reward");
        ctOnlineReward.debug("玩家奖励数组: " + rewardList);
        return rewardList;
    }

    public boolean addRewardToPlayData(String rewardId,Player player){
        ctOnlineReward.debug("添加奖励到玩家数据 for reward: " + rewardId + ", player: " + player.getName());
        Map.Entry<String,YamlConfiguration> playerDataPair = getYamlData();
        YamlConfiguration playerData=playerDataPair.getValue();
        List<String> rewardList = playerData.getStringList(player.getUniqueId().toString() + ".reward");
        ctOnlineReward.debug("当前玩家奖励列表: " + rewardList);
        rewardList.add(rewardId);
        playerData.set(player.getUniqueId().toString()+".reward",rewardList);
        saveData(playerDataPair);
        ctOnlineReward.debug("奖励 " + rewardId + " 已添加到玩家数据并保存.");
        return true;
    }

    @Override
    public int getPlayerOnlineTimeWeek(OfflinePlayer player) {
        int onlineTime = 0;
        List<String> weekString = Util.getWeekString();

        ctOnlineReward.debug("获取玩家周在线时间 for player: " + player.getName());
        Map.Entry<String, YamlConfiguration> yamlData = getYamlData();

        for (String s : weekString) {
            ctOnlineReward.debug("处理周数据文件: " + s + ".yml");
            File file = new File(ctOnlineReward.getDataFolder() + "/playerData/" + s+".yml");
            if (file.exists()){
                try {
                    int time;
                    //如果是当前文件，使用内存数据
                    if(file.getName().startsWith(yamlData.getKey())){
                        readLock.lock();
                        try {
                            time = yamlData.getValue().getInt(player.getUniqueId().toString() + ".time");
                            ctOnlineReward.debug("从当前内存数据获取时间: " + time);
                        }finally {
                            readLock.unlock();
                        }
                    }else {
                        YamlConfiguration yamlConfiguration = new YamlConfiguration();
                        yamlConfiguration.load(file);
                        time=yamlConfiguration.getInt(player.getUniqueId().toString() + ".time");
                        ctOnlineReward.debug("从文件 " + file.getName() + " 获取时间: " + time);
                    }
                    onlineTime += time;
                    ctOnlineReward.debug("当前总周在线时间: " + onlineTime);
                }catch (Exception e){
                    ctOnlineReward.debug("读取周数据文件时发生异常: " + file.getName() + ". 错误: " + e.getMessage());
                    if (ctOnlineReward.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            } else {
                ctOnlineReward.debug("周数据文件不存在: " + file.getName());
            }
        }
        ctOnlineReward.debug("总周在线时间: " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeMonth(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家月在线时间 for player: " + player.getName());
        int onlineTime = 0;
        List<String> monthString = Util.getMonthString();
        Map.Entry<String, YamlConfiguration> yamlData = getYamlData();

        for (String s : monthString) {
            ctOnlineReward.debug("处理月数据文件: " + s + ".yml");
            File file = new File(ctOnlineReward.getDataFolder() + "/playerData/" + s+".yml");
            if (file.exists()){
                try {
                    int time;
                    //如果是当前文件，使用内存数据
                    if(file.getName().startsWith(yamlData.getKey())){
                        readLock.lock();
                        try {
                            time = yamlData.getValue().getInt(player.getUniqueId().toString() + ".time");
                            ctOnlineReward.debug("从当前内存数据获取时间: " + time);
                        }finally {
                            readLock.unlock();
                        }
                    }else {
                        YamlConfiguration yamlConfiguration = new YamlConfiguration();
                        yamlConfiguration.load(file);
                        time=yamlConfiguration.getInt(player.getUniqueId().toString() + ".time");
                        ctOnlineReward.debug("从文件 " + file.getName() + " 获取时间: " + time);
                    }
                    onlineTime += time;
                    ctOnlineReward.debug("当前总月在线时间: " + onlineTime);
                }catch (Exception e){
                    ctOnlineReward.debug("读取月数据文件时发生异常: " + file.getName() + ". 错误: " + e.getMessage());
                    if (ctOnlineReward.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            } else {
                ctOnlineReward.debug("月数据文件不存在: " + file.getName());
            }
        }
        ctOnlineReward.debug("总月在线时间: " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeAll(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家总在线时间 for player: " + player.getName());
        int onlineTime = 0;
        File file = new File(ctOnlineReward.getDataFolder() + "/playerData");
        File[] files = file.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null){
            ctOnlineReward.debug("玩家数据目录为空.");
            return 0;
        }
        Map.Entry<String, YamlConfiguration> yamlData = getYamlData();

        for (File file1 : files) {
            ctOnlineReward.debug("处理总数据文件: " + file1.getName());
            try {
                int time;
                //如果是当前文件，使用内存数据
                if(file1.getName().startsWith(yamlData.getKey())){
                    readLock.lock();
                    try {
                        time = yamlData.getValue().getInt(player.getUniqueId().toString() + ".time");
                        ctOnlineReward.debug("从当前内存数据获取时间: " + time);
                    }finally {
                        readLock.unlock();
                    }
                }else {
                    YamlConfiguration yamlConfiguration = new YamlConfiguration();
                    yamlConfiguration.load(file1);
                    time=yamlConfiguration.getInt(player.getUniqueId().toString() + ".time");
                    ctOnlineReward.debug("从文件 " + file1.getName() + " 获取时间: " + time);
                }
                onlineTime += time;
                ctOnlineReward.debug("当前总在线时间: " + onlineTime);
            }catch (Exception e){
                ctOnlineReward.debug("读取总数据文件时发生异常: " + file1.getName() + ". 错误: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总在线时间: " + onlineTime);
        return onlineTime;
    }

}

