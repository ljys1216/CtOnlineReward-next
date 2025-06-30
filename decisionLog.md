2025-06-30 09:45:29 - 决定使用 Maven 的 `mvn clean package` 命令构建插件，因为项目结构显示这是一个 Maven 项目。
### 2025/6/30 上午9:56:54 - 插件版本升级至 1.21.4 决策

*   **Java 版本升级**: 决定将 Java 版本从 1.8 升级到 21，以满足 Minecraft 1.21.4 的运行环境要求。
*   **核心依赖更新**: 将 `ink.ptms.core` 依赖更新到 `v12104`，以确保与目标 Minecraft 版本的 API 兼容性。
*   **第三方依赖更新**: 更新 `VaultAPI` (1.7.1), `PlaceholderAPI` (2.11.5), `HikariCP` (5.0.1), `XSeries` (9.10.0) 到最新兼容版本，以提高插件的稳定性、性能和兼容性。
*   **旧依赖移除**: 移除 `authlib`, `bungeecord-chat`, `gson`, `json-simple`, `commons-lang` 等不再需要的依赖，以精简插件，减少潜在的冲突和文件大小。
*   **API 版本声明**: 更新 `plugin.yml` 中的 `api-version` 到 `1.20`，以声明插件兼容的最低 Spigot API 版本。