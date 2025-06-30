2025-06-30 09:45:29 - 开始构建 MC 服务器插件。
2025-06-30 09:45:29 - 插件构建成功。
2025-06-30 09:52:56 - 查找插件注册的 PAPI 变量。
### 2025/6/30 上午9:56:46 - 插件版本升级至 1.21.4

*   开始修改插件以支持 1.21.4。
*   更新 `pom.xml` 中的 `java.version` 到 21。
*   更新 `pom.xml` 中的 `ink.ptms.core` 依赖到 1.21.4 兼容版本 (v12104)。
*   更新 `pom.xml` 中的 `VaultAPI` 到 1.7.1。
*   更新 `pom.xml` 中的 `PlaceholderAPI` 到 2.11.5。
*   更新 `pom.xml` 中的 `HikariCP` 到 5.0.1。
*   更新 `pom.xml` 中的 `XSeries` 到 9.10.0。
*   移除 `pom.xml` 中不再需要的 `authlib`, `bungeecord-chat`, `gson`, `json-simple`, `commons-lang` 依赖。
*   更新 `src/main/resources/plugin.yml` 中的 `api-version` 到 1.20。
### 2025/6/30 上午10:02:17 - 插件编译成功

*   更新 `pom.xml` 中的 `maven-shade-plugin` 版本到 3.5.0。
*   成功编译插件。
### 2025/6/30 上午10:06:43 - 修复运行时错误并重新编译

*   修复 `InventoryMonitor.java` 中的 `UnsupportedOperationException`，通过创建可修改的列表副本。
*   添加 `import java.util.ArrayList;` 到 `InventoryMonitor.java`。
*   成功编译插件。
### 2025/6/30 上午10:22:58 - 添加 GUI slot 位置兼容

*   修改 `InventoryFactory.java` 中的 `getIndexList` 方法，使其兼容 `slot: 0-53` 的图标位置选项。
### 2025/6/30 上午10:23:51 - 添加 GUI slot 列表位置兼容

*   修改 `InventoryFactory.java` 中的 `getIndexList` 方法，使其兼容 `slot: [0,1,2,3,4,5]` 这种多个图标位置的列表形式。
### 2025/6/30 上午10:24:38 - 重新编译插件

*   成功编译插件。
### 2025/6/30 上午10:26:32 - 更新插件版本号

*   更新 `pom.xml` 中的版本号为 `0.2.7`。
### 2025/6/30 上午10:30:49 - 添加 Debug 开关

*   在 `config.yml` 中添加 `Debug: false` 配置。
*   在 `CtOnlineReward.java` 中添加 debug 模式的读取和管理逻辑。
*   修改 `InventoryMonitor.java` 和 `InventoryFactory.java` 中的日志输出，使其在 debug 模式下输出。
### 2025/6/30 上午10:32:30 - 添加 Debug 调试功能

*   在 `CtOnlineReward.java` 中添加 `debug(String message)` 方法。
*   修改 `InventoryMonitor.java` 和 `InventoryFactory.java` 中的日志输出，使其使用 `debug` 方法。
### 2025/6/30 上午10:33:01 - 更新插件版本号

*   更新 `pom.xml` 中的版本号为 `0.2.8`。
### 2025/6/30 上午10:34:10 - 重新编译插件 (版本 0.2.8)

*   成功编译插件，版本号为 `0.2.8`。
### 2025/6/30 上午10:49:19 - 修复重复代码并增强 Debug 日志

*   修复 `InventoryMonitor.java` 中重复的 `executeCommand` 方法。
*   修复 `YamlBase.java`, `MysqlBase.java`, `SQLiteBase.java` 中重复的 `@Override` 注解。
*   在 `YamlBase.java`, `MysqlBase.java`, `SQLiteBase.java` 中添加了更多的 debug 信息。
### 2025/6/30 上午10:51:00 - 更新插件版本号

*   更新 `pom.xml` 中的版本号为 `0.2.9`。
### 2025/6/30 上午10:53:03 - 重新编译插件 (版本 0.2.9)

*   成功编译插件，版本号为 `0.2.9`。
### 2025/6/30 上午11:01:58 - 增强 GUI 图标位置和获取调试信息

*   增强 `InventoryFactory.java` 中的 debug 日志，包括获取图标和图标位置的调试信息。
### 2025/6/30 上午11:03:06 - 更新插件版本号

*   更新 `pom.xml` 中的版本号为 `0.2.10`。
### 2025/6/30 上午11:04:52 - 重新编译插件 (版本 0.2.10)

*   成功编译插件，版本号为 `0.2.10`。
### 2025/6/30 上午11:13:32 - Debug 开关支持热重载

*   修改 `CtOnlineReward.java` 中的 `load()` 方法，使其在重载时更新 debug 开关状态。
### 2025/6/30 上午11:16:04 - 更新插件版本号

*   更新 `pom.xml` 中的版本号为 `0.2.11`。
### 2025/6/30 上午11:17:21 - 重新编译插件 (版本 0.2.11)

*   成功编译插件，版本号为 `0.2.11`。