# DG-LAB x Minecraft (Bukkit/Paper)

本插件是 DG-LAB 模组**服务端逻辑**的 Bukkit/Paper 插件移植版。

### 项目目标
- **兼容原版客户端**：原版玩家可正常加入，插件消息通道会被自动忽略，不影响游戏。
- **支持模组客户端**：安装了 DG-LAB 客户端模组的玩家能正常接收自定义数据包。
- **脚本支持**：内置类 KubeJS 的 JS 脚本引擎（Rhino），支持通过脚本自定义惩罚逻辑。

---

## 运行环境
- **Java**: 21
- **服务端**: Paper/Spigot 1.21.x
- **核心依赖**: [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (必须安装，用于发送自定义数据包)

---

## 安装说明
1. **编译插件**:
   ```bash
   ./gradlew clean build
   ```
2. **获取 Jar 包**:
   编译完成后，在 `build/libs/` 目录下找到 `Minecraft-DG-LAB-Bukkit-1.0.jar`。
3. **部署**:
   将 Jar 包放入服务器的 `plugins/` 目录。
4. **生成配置**:
   启动一次服务器以生成默认配置文件：
   - `plugins/dglab/config.yml`
   - `plugins/dglab/scripts/example.js`

---

## 配置文件 (config.yml)
```yml
websocket:
  enabled: false    # 是否启用 WebSocket 服务器
  address: "127.0.0.1" # 二维码中使用的地址，通常应填写你的 MC 服务器公网 IP 或域名
  port: 8080        # WebSocket 端口
  useHttps: false   # 如果为 true，二维码将使用 wss:// 协议
```

---

## 插件命令
- `/dglab connect` - 向玩家发送 App 连接二维码。
- `/dglab disconnect` - 断开玩家当前的 DG-LAB 连接并清理客户端 UI。
- `/dglab reload` - 重载配置文件及所有脚本。
  - 权限: `dglab.admin` (默认仅 OP 可用)

---

## 脚本开发 (类 KubeJS)
脚本存放路径：`plugins/dglab/scripts/*.js`

### 全局 API (与模组版保持一致)
本插件暴露了以下全局对象，尽可能还原了模组版 KubeJS 的开发体验：
- `ChannelType`: 通道枚举 (`A`, `B`)
- `DgLabMessage`: 消息构造类
- `DgLabMessageType`: 消息类型枚举
- `DgLabManager`: 连接管理器 (对应 `com.tendoarisu.dglab.api.ConnectionManager`)
- `DgLabPulseUtil`: 脉冲工具类

### 支持事件
目前已实现的事件子集：
- `EntityEvents.death('player', callback)`: 玩家死亡时触发
- `EntityEvents.afterHurt('player', callback)`: 玩家受伤后触发

**事件对象属性**:
- `event.getEntity()`: 返回 `PlayerWrapper` 对象，可调用 `getUuid()`, `getName()` 等方法。
