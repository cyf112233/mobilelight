# mobilelight
一个安装在服务上的移动光源 ( 给不会装模组和材质的玩家和基岩版玩家的方便 ) 
<br>
如果服务器是高版本并且安装了都多版本插件，会给低版本使用兼容模式，基岩版可以正常使用
<br>
这是我这辈子做过最麻烦的插件
# 移动光源插件逻辑流程

```mermaid
flowchart TD
    %% 主流程
    Start[插件启动] --> LoadConfig[加载配置文件]
    LoadConfig --> InitDebug{调试模式?}
    InitDebug -->|是| DebugOn[启用调试]
    InitDebug -->|否| DebugOff[禁用调试]
    LoadConfig --> LoadItems[加载光源物品列表]
    
    %% 事件处理
    PlayerEvent[玩家事件] --> EventType{事件类型}
    EventType -->|移动| MoveEvent[移动事件]
    EventType -->|交互| InteractEvent[交互事件]
    EventType -->|切换物品| SwapEvent[切换物品事件]
    EventType -->|物品栏操作| InvEvent[物品栏操作事件]
    EventType -->|切换手持物品| HeldEvent[切换手持物品事件]
    EventType -->|退出| QuitEvent[退出事件]
    
    %% 光源检查
    MoveEvent --> CheckLight[检查光源状态]
    InteractEvent --> CheckLight
    SwapEvent --> CheckLight
    InvEvent --> CheckLight
    HeldEvent --> CheckLight
    QuitEvent --> CleanLight[清理光源]
    
    %% 物品检测
    CheckLight --> HasItem{持有光源物品?}
    HasItem -->|否| RemoveLight[移除光源]
    HasItem -->|是| UpdateLight[更新光源]
    
    %% 物品检查
    HasItem --> CheckMain{主手?}
    CheckMain -->|是| HasLight[有光源]
    CheckMain -->|否| CheckOff{副手?}
    CheckOff -->|是| HasLight
    CheckOff -->|否| CheckArmor{装备栏?}
    CheckArmor -->|头盔| IsLight{是光源?}
    CheckArmor -->|胸甲| IsLight
    CheckArmor -->|护腿| IsLight
    CheckArmor -->|靴子| IsLight
    IsLight -->|是| HasLight
    IsLight -->|否| NoLight[无光源]
    
    %% 光源更新
    UpdateLight --> CheckVersion{玩家版本?}
    CheckVersion -->|1.16及以下| PlaceTorch[放置火把]
    CheckVersion -->|1.17及以上| PlaceLight[放置光源方块]
    
    PlaceTorch --> GetTorchPos[获取火把位置]
    GetTorchPos --> BehindPlayer[玩家背后1格]
    BehindPlayer --> SendFakeTorch[发送假火把方块]
    
    PlaceLight --> PlayerPos[玩家位置]
    PlayerPos --> SendFakeLight[发送假光源方块]
    
    %% 假方块处理
    SendFakeTorch --> StorePos[存储位置]
    SendFakeLight --> StorePos
    
    %% 数据存储
    StorePos --> Map[Map存储]
    Map --> UUID[玩家UUID]
    Map --> Loc[位置信息]
    Loc --> X[X坐标]
    Loc --> Y[Y坐标]
    Loc --> Z[Z坐标]
    
    %% 调试输出
    DebugOn --> Log[调试日志]
    Log --> LogPos[位置日志]
    Log --> LogItem[物品日志]
    Log --> LogBlock[方块日志]
    Log --> LogVersion[版本日志]
    
    %% 样式
    classDef event fill:#f9f,stroke:#333,stroke-width:2px
    classDef process fill:#bbf,stroke:#333,stroke-width:2px
    classDef decision fill:#fbb,stroke:#333,stroke-width:2px
    classDef data fill:#bfb,stroke:#333,stroke-width:2px
    
    class PlayerEvent,EventType,MoveEvent,InteractEvent,SwapEvent,InvEvent,HeldEvent,QuitEvent event
    class LoadConfig,LoadItems,CheckLight,GetTorchPos,PlayerPos,SendFakeTorch,SendFakeLight,RemoveLight process
    class InitDebug,EventType,HasItem,CheckMain,CheckOff,CheckArmor,IsLight,CheckVersion decision
    class Map,UUID,Loc,X,Y,Z data
```

