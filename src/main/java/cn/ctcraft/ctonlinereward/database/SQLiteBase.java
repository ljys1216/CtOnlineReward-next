package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.utils.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteBase implements DataService {
    private final CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public SQLiteBase(){
        ctOnlineReward.debug("SQLiteBase 构造函数.");
        createTable();
    }

    public Connection getConnection() throws SQLException {
        return CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection();
    }


    public void createTable() {
        ctOnlineReward.debug("创建 SQLite 表.");
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = getConnection();
            String date = Util.getDate();
            String sql = "CREATE TABLE IF NOT EXISTS `"+date+"`  (" +
                    "  `uuid` varchar(255) NOT NULL," +
                    "  `online_data` varchar(255) DEFAULT NULL," +
                    "  PRIMARY KEY (`uuid`) " +
                    ")";
            ps = connection.prepareStatement(sql);
            int i = ps.executeUpdate();
            if (i > 0) {
                String lang = CtOnlineReward.languageHandler.getLang("mysql.createTable");
                ctOnlineReward.getLogger().info(lang);
                ctOnlineReward.debug("SQLite 表创建成功: " + date);
            } else {
                ctOnlineReward.debug("SQLite 表已存在或创建失败.");
            }
        } catch (Exception e) {
            ctOnlineReward.debug("创建 SQLite 表时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 表创建资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 表创建资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int getPlayerOnlineTime(OfflinePlayer pLayer) {
        ctOnlineReward.debug("获取玩家在线时间 (SQLite) for player: " + pLayer.getName());
        JsonObject playerOnlineData = getPlayerOnlineData(pLayer);
        JsonElement time = playerOnlineData.get("time");
        if (time == null){
            ctOnlineReward.debug("玩家在线时间未找到，插入初始值 0.");
            insertPlayerOnlineTime(pLayer,0);
            return 0;
        }
        int onlineTime = time.getAsInt();
        ctOnlineReward.debug("玩家在线时间 (SQLite): " + onlineTime);
        return onlineTime;
    }

    @Override
    public void addPlayerOnlineTime(OfflinePlayer player, int time) {
        ctOnlineReward.debug("增加玩家在线时间 (SQLite) for player: " + player.getName() + ", time: " + time);
        String date = Util.getDate();
        String sql = "update `"+date+"` set `online_data` = ? where `uuid` = ?";
        try (Connection connection = getConnection();PreparedStatement ps = connection.prepareStatement(sql);){
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            playerOnlineData.addProperty("time", time);
            String asString = playerOnlineData.toString();
            ps.setString(1, asString);
            ps.setString(2,player.getUniqueId().toString());
            ps.executeUpdate();
            ctOnlineReward.debug("玩家在线时间 (SQLite) 已更新并保存.");
        } catch (Exception e) {
            ctOnlineReward.debug("更新玩家在线时间 (SQLite) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }


    public JsonObject getPlayerOnlineData(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家在线数据 (SQLite) for player: " + player.getName());
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            String date = Util.getDate();
            String sql = "select `online_data` from `"+date+"` where `uuid`=?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, player.getUniqueId().toString());
            rs = ps.executeQuery();
            if (rs.next()) {
                String onlineData = rs.getString(1);
                ctOnlineReward.debug("从数据库获取原始在线数据: " + onlineData);
                JsonParser jsonParser = new JsonParser();
                JsonElement parse = jsonParser.parse(onlineData);
                if (parse.isJsonNull()) {
                    ctOnlineReward.debug("解析在线数据为 null.");
                    return new JsonObject();
                }
                ctOnlineReward.debug("成功解析在线数据.");
                return parse.getAsJsonObject();
            }
        } catch (Exception e) {
            ctOnlineReward.debug("获取玩家在线数据 (SQLite) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 在线数据查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 在线数据查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("返回新的 JsonObject (玩家在线数据未找到或为空).");
        return new JsonObject();
    }

    @Override
    public void insertPlayerOnlineTime(OfflinePlayer player,int time) {
        ctOnlineReward.debug("插入玩家在线时间 (SQLite) for player: " + player.getName() + ", time: " + time);
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = getConnection();
            String date = Util.getDate();
            String sql = "insert into `"+date+"` (`uuid`,`online_data`) values (?,?)";
            ps = connection.prepareStatement(sql);
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
                ctOnlineReward.debug("玩家在线时间 (SQLite) 已插入.");
            }
        } catch (Exception e) {
            String message = e.getMessage();
            ctOnlineReward.debug("插入玩家在线时间 (SQLite) 时发生异常: " + e.getMessage());
            if (message.contains("doesn't exist")){
                ctOnlineReward.debug("表不存在，尝试创建表.");
                createTable();
            }else {
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 插入在线时间资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 插入在线时间资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<String> getPlayerRewardArray(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家奖励数组 (SQLite) for player: " + player.getName());
        JsonObject playerOnlineData = getPlayerOnlineData(player);
        JsonElement reward = playerOnlineData.get("reward");
        List<String> rewardList = new ArrayList<>();
        if (reward == null) {
            ctOnlineReward.debug("玩家奖励数据为空.");
            return rewardList;
        }
        JsonArray asJsonArray = reward.getAsJsonArray();
        for (JsonElement jsonElement : asJsonArray) {
            rewardList.add(jsonElement.getAsString());
        }
        ctOnlineReward.debug("玩家奖励数组 (SQLite): " + rewardList);
        return rewardList;
    }

    @Override
    public boolean addRewardToPlayData(String rewardId, Player player) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            JsonElement reward = playerOnlineData.get("reward");
            if (reward == null) {
                playerOnlineData.add("reward", new JsonArray());
                reward = playerOnlineData.get("reward");
            }
            JsonArray asJsonArray = reward.getAsJsonArray();
            asJsonArray.add(rewardId);
            playerOnlineData.add("reward", asJsonArray);
            connection = getConnection();
            String date = Util.getDate();
            String sql = "update `"+date+"` set `online_data` = ? where `uuid` = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            int i = ps.executeUpdate();
            if (i > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }
        return false;
    }

    @Override
    public int getPlayerOnlineTimeWeek(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家周在线时间 (SQLite) for player: " + player.getName());
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
            if (sql.isEmpty()){
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
            ctOnlineReward.debug("获取玩家周在线时间 (SQLite) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 周在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 周在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总周在线时间 (SQLite): " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeMonth(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家月在线时间 (SQLite) for player: " + player.getName());
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
            if (sql.isEmpty()){
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
            ctOnlineReward.debug("获取玩家月在线时间 (SQLite) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 月在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 月在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总月在线时间 (SQLite): " + onlineTime);
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeAll(OfflinePlayer player) {
        ctOnlineReward.debug("获取玩家总在线时间 (SQLite) for player: " + player.getName());
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
            if (sql.isEmpty()){
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
            ctOnlineReward.debug("获取玩家总在线时间 (SQLite) 时发生异常: " + e.getMessage());
            if (ctOnlineReward.isDebugMode()) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (connection != null) connection.close();
                ctOnlineReward.debug("SQLite 总在线时间查询资源已关闭.");
            } catch (SQLException e) {
                ctOnlineReward.debug("关闭 SQLite 总在线时间查询资源时发生 SQL 异常: " + e.getMessage());
                if (ctOnlineReward.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        ctOnlineReward.debug("总在线时间 (SQLite): " + onlineTime);
        return onlineTime;
    }
}
