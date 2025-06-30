package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.utils.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MysqlBase implements DataService {
    private CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public MysqlBase() {
        ctOnlineReward.debug("MysqlBase 构造函数.");
        createTable();
    }

    public Connection getConnection() throws SQLException {
        return CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection();
    }


    public void createTable() {
        ctOnlineReward.debug("创建 MySQL 表.");
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String date = Util.getDate();
            String sql = "CREATE TABLE IF NOT EXISTS `" + date + "`  (" +
                    "  `uuid` varchar(255) NOT NULL COMMENT '玩家uuid'," +
                    "  `online_data` varchar(255) DEFAULT NULL COMMENT '在线数据'," +
                    "  PRIMARY KEY (`uuid`) " +
                    ") ENGINE = InnoDB CHARACTER SET = utf8";
            int i = statement.executeUpdate(sql);
            if (i > 0) {
                String lang = CtOnlineReward.languageHandler.getLang("mysql.createTable");
                ctOnlineReward.getLogger().info(lang);
                ctOnlineReward.debug("MySQL 表创建成功: " + date);
            } else {
                ctOnlineReward.debug("MySQL 表已存在或创建失败.");
            }
        } catch (SQLException e) {
            ctOnlineReward.debug("创建 MySQL 表时发生 SQL 异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public int getPlayerOnlineTime(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家在线时间 (MySQL) for player: " + player.getName());
        JsonObject playerOnlineData = getPlayerOnlineData(player);
        JsonElement time = playerOnlineData.get("time");
        int onlineTime = time != null ? time.getAsInt() : -1;

        if (onlineTime == -1) {
            ctOnlineReward.debug("玩家在线时间未找到，插入初始值 0.");
            insertPlayerOnlineTime(player, 0);
        }
        ctOnlineReward.debug("玩家在线时间 (MySQL): " + onlineTime);
        return onlineTime;
    }

    @Override
    public void addPlayerOnlineTime(OfflinePlayer player, int time) {
        ctOnlineReward.debug("增加玩家在线时间 (MySQL) for player: " + player.getName() + ", time: " + time);
        String date = Util.getDate();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE `"+date+"` SET `online_data` = ? WHERE `uuid` = ?")) {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            playerOnlineData.addProperty("time", time);

            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            ps.executeUpdate();
            ctOnlineReward.debug("玩家在线时间 (MySQL) 已更新并保存.");
        } catch (SQLException e) {
            ctOnlineReward.debug("更新玩家在线时间 (MySQL) 时发生 SQL 异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }


    public JsonObject getPlayerOnlineData(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家在线数据 (MySQL) for player: " + player.getName());
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT `online_data` FROM `"+Util.getDate()+"` WHERE `uuid` = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String onlineData = rs.getString(1);
                    ctOnlineReward.debug("从数据库获取原始在线数据: " + onlineData);
                    if (onlineData != null && !onlineData.isEmpty()) {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement parse = jsonParser.parse(onlineData);
                        if (!parse.isJsonNull()) {
                            ctOnlineReward.debug("成功解析在线数据.");
                            return parse.getAsJsonObject();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            ctOnlineReward.debug("获取玩家在线数据 (MySQL) 时发生 SQL 异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }
        ctOnlineReward.debug("返回新的 JsonObject (玩家在线数据未找到或为空).");
        return new JsonObject();
    }

    @Override
    public void insertPlayerOnlineTime(OfflinePlayer player, int time) {
        ctOnlineReward.debug("插入玩家在线时间 (MySQL) for player: " + player.getName() + ", time: " + time);
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO `"+Util.getDate()+"` (`uuid`, `online_data`) VALUES (?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", time);
            jsonObject.add("reward", new JsonArray());
            ps.setString(2, jsonObject.toString());
            int i = ps.executeUpdate();
            if (i < 0) {
                ctOnlineReward.getLogger().warning("§c§l■ 数据库异常，数据插入失败！");
                ctOnlineReward.debug("数据库异常，数据插入失败 for player: " + player.getName());
            } else {
                ctOnlineReward.debug("玩家在线时间 (MySQL) 已插入.");
            }
        } catch (SQLException e) {
            String message = e.getMessage();
            ctOnlineReward.debug("插入玩家在线时间 (MySQL) 时发生 SQL 异常: " + e.getMessage());
            if (message.contains("doesn't exist")) {
                ctOnlineReward.debug("表不存在，尝试创建表.");
                createTable();
            } else {
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<String> getPlayerRewardArray(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家奖励数组 (MySQL) for player: " + player.getName());
        JsonObject playerOnlineData = getPlayerOnlineData(player);
        JsonElement reward = playerOnlineData.get("reward");
        List<String> rewardList = new ArrayList<>();
        if (reward != null && reward.isJsonArray()) {
            JsonArray jsonArray = reward.getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                rewardList.add(jsonElement.getAsString());
            }
            ctOnlineReward.debug("玩家奖励数组 (MySQL): " + rewardList);
        } else {
            ctOnlineReward.debug("玩家奖励数据为空或不是 JsonArray.");
        }
        return rewardList;
    }


    @Override
    public boolean addRewardToPlayData(String rewardId, Player player) {
        ctOnlineReward.debug("添加奖励到玩家数据 (MySQL) for reward: " + rewardId + ", player: " + player.getName());
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE `"+Util.getDate()+"` SET `online_data` = ? WHERE `uuid` = ?")) {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            JsonElement reward = playerOnlineData.get("reward");
            if (reward == null) {
                ctOnlineReward.debug("奖励数据中没有 'reward' 字段，添加新的 JsonArray.");
                playerOnlineData.add("reward", new JsonArray());
                reward = playerOnlineData.get("reward");
            }
            if (reward.isJsonArray()) {
                JsonArray rewardArray = reward.getAsJsonArray();
                rewardArray.add(rewardId);
                ctOnlineReward.debug("奖励 " + rewardId + " 已添加到 JsonArray.");
            } else {
                ctOnlineReward.debug("'reward' 字段不是 JsonArray 类型.");
            }
            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            int rowsUpdated = ps.executeUpdate();
            ctOnlineReward.debug("更新玩家数据 (MySQL) 影响行数: " + rowsUpdated);
            return rowsUpdated > 0;
        } catch (SQLException e) {
            ctOnlineReward.debug("添加奖励到玩家数据 (MySQL) 时发生 SQL 异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }
        return false;
    }


    @Override
    public int getPlayerOnlineTimeWeek(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家周在线时间 (MySQL) for player: " + player.getName());
        String uuid = player.getUniqueId().toString();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int onlineTime = 0;
        try {
            connection = getConnection();
            SqlUtil sqlUtil = SqlUtil.getInstance();
            List<String> tableList = sqlUtil.getTableList();
            List<String> weekString = Util.getWeekString();
            String sql = "";
            for (String s : weekString) {
                if (tableList.contains(s)) {
                    if (sql.equalsIgnoreCase("")) {
                        sql = "select `online_data` from `" + s + "` where uuid='" + uuid + "'";
                    } else {
                        sql = sql.concat(" union all select `online_data` from `" + s + "` where uuid='" + uuid + "'");
                    }
                    ctOnlineReward.debug("构建周在线时间 SQL: " + sql);
                } else {
                    ctOnlineReward.debug("表 " + s + " 不存在，跳过.");
                }
            }
            if (StringUtils.isEmpty(sql)){
                ctOnlineReward.debug("周在线时间 SQL 为空，返回 0.");
                return onlineTime;
            }
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                JsonParser jsonParser = new JsonParser();
                String onlineData = rs.getString(1);
                ctOnlineReward.debug("从数据库获取周在线数据: " + onlineData);
                JsonElement parse = jsonParser.parse(onlineData);
                if (!parse.isJsonNull()) {
                    int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                    onlineTime += onlineTimeByJsonObject;
                    ctOnlineReward.debug("当前总周在线时间: " + onlineTime);
                } else {
                    ctOnlineReward.debug("解析周在线数据为 null.");
                }
            }
        } catch (Exception e) {
            ctOnlineReward.debug("获取玩家周在线时间 (MySQL) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("周在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭周在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总周在线时间 (MySQL): " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeMonth(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家月在线时间 (MySQL) for player: " + player.getName());
        String uuid = player.getUniqueId().toString();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int onlineTime = 0;
        try {
            connection = getConnection();
            SqlUtil sqlUtil = SqlUtil.getInstance();
            List<String> tableList = sqlUtil.getTableList();
            List<String> monthString = Util.getMonthString();
            String sql = "";
            for (String s : monthString) {
                if (tableList.contains(s)) {
                    if (sql.equalsIgnoreCase("")) {
                        sql = "select `online_data` from `" + s + "` where uuid='" + uuid + "'";
                    } else {
                        sql = sql.concat(" union all select `online_data` from `" + s + "` where uuid='" + uuid + "'");
                    }
                    ctOnlineReward.debug("构建月在线时间 SQL: " + sql);
                } else {
                    ctOnlineReward.debug("表 " + s + " 不存在，跳过.");
                }
            }
            if (StringUtils.isEmpty(sql)){
                ctOnlineReward.debug("月在线时间 SQL 为空，返回 0.");
                return onlineTime;
            }
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                JsonParser jsonParser = new JsonParser();
                String onlineData = rs.getString(1);
                ctOnlineReward.debug("从数据库获取月在线数据: " + onlineData);
                JsonElement parse = jsonParser.parse(onlineData);
                if (!parse.isJsonNull()) {
                    int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                    onlineTime += onlineTimeByJsonObject;
                    ctOnlineReward.debug("当前总月在线时间: " + onlineTime);
                } else {
                    ctOnlineReward.debug("解析月在线数据为 null.");
                }
            }
        } catch (Exception e) {
            ctOnlineReward.debug("获取玩家月在线时间 (MySQL) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("月在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭月在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总月在线时间 (MySQL): " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeAll(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家总在线时间 (MySQL) for player: " + player.getName());
        String uuid = player.getUniqueId().toString();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int onlineTime = 0;
        try {
            connection = getConnection();
            SqlUtil sqlUtil = SqlUtil.getInstance();
            List<String> tableList = sqlUtil.getTableList();
            String sql = "";
            for (String s : tableList) {
                if (sql.equalsIgnoreCase("")) {
                    sql = "select `online_data` from `" + s + "` where uuid='" + uuid + "'";
                } else {
                    sql = sql.concat(" union all select `online_data` from `" + s + "` where uuid='" + uuid + "'");
                }
                ctOnlineReward.debug("构建总在线时间 SQL: " + sql);
            }
            if (StringUtils.isEmpty(sql)){
                ctOnlineReward.debug("总在线时间 SQL 为空，返回 0.");
                return onlineTime;
            }
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                JsonParser jsonParser = new JsonParser();
                String onlineData = rs.getString(1);
                ctOnlineReward.debug("从数据库获取总在线数据: " + onlineData);
                JsonElement parse = jsonParser.parse(onlineData);
                if (!parse.isJsonNull()) {
                    int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                    onlineTime += onlineTimeByJsonObject;
                    ctOnlineReward.debug("当前总在线时间: " + onlineTime);
                } else {
                    ctOnlineReward.debug("解析总在线数据为 null.");
                }
            }
        } catch (Exception e) {
            ctOnlineReward.debug("获取玩家总在线时间 (MySQL) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("总在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭总在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总在线时间 (MySQL): " + onlineTime);
        return onlineTime;
    }
}
