# Jade box

本仓库基于 [mlabalabala/box](https://github.com/mlabalabala/box) 修改；其上游源项目为 [CatVodTVOfficial/TVBoxOSC](https://github.com/CatVodTVOfficial/TVBoxOSC)。本次修改继续兼容 Android 4.4（`minSdk 16`），并补充 ARM64 构建与可选开机自启功能。

## 本次改动

- 设置页新增“开机自启”开关，默认关闭。
- 开启后在开机完成的第 3 秒尝试启动；若应用尚未成功进入主页，再依次等待 5、10、15 秒重试，即触发时间约为开机后的第 3、8、18、33 秒。
- 应用成功进入主页后会取消剩余重试；重复 Intent 使用 `CLEAR_TOP` 和 `SINGLE_TOP`，不会重复创建多个主页实例。
- Android 10 及以上版本使用悬浮窗权限辅助从后台启动。首次开启时请允许 Jade“显示在其他应用上层”；Android 4.4 无需此权限。
- 增加 `arm64-v8a` 原生库和按 ABI 构建能力，同时保留 `armeabi-v7a` 支持。

## 安装包

| 用途 | 架构与类型 | 下载 | SHA-256 |
| --- | --- | --- | --- |
| Android 4.4 电视及 32 位设备 | ARM32 Release | [Jade-v31-TV-Android4.4-ARM32-release.apk](https://github.com/2334336/box/releases/download/v0.0.4-autostart/Jade-v31-TV-Android4.4-ARM32-release.apk) | `dc6d59a1893108317fb6fe7885997fd03ec7db1ce9c7b56f862e2a37ad000f0d` |
| BlueStacks 等 64 位模拟器调试 | ARM64 Debug | [Jade-v31-BlueStacks-ARM64-debug.apk](https://github.com/2334336/box/releases/download/v0.0.4-autostart/Jade-v31-BlueStacks-ARM64-debug.apk) | `f33f03402d593f97b2db947049d900bdb80c5fc084681138ade59312e00c2e1f` |

两个安装包均为版本 `31 / 0.0.4-autostart`，最低支持 Android 4.1（API 16）。电视优先安装 ARM32 Release；ARM64 Debug 主要用于模拟器验证。

### 竖屏手机版
#### [项目地址 https://github.com/mlabalabala/TVBoxOS-Mobile](https://github.com/mlabalabala/TVBoxOS-Mobile)
#### [源项目地址 https://github.com/XiaoRanLiu3119/TVBoxOS-Mobile](https://github.com/XiaoRanLiu3119/TVBoxOS-Mobile)
####  [Jade_SHJ_Mob_[时间戳] 码：6111](https://bunny6111.lanzouq.com/b04whyfwf)
### 
### 横屏TV版
#### [项目地址 https://github.com/mlabalabala/box](https://github.com/mlabalabala/box)
###
#### 源项目
#### https://github.com/CatVodTVOfficial/TVBoxOSC

## 推荐：[FongMi的okjack分支安卓4.4版本tv版直链](https://raw.bunnyxyz.eu.org/https://github.com/FongMi/Release/blob/okjack/apk/kitkat/leanback.apk)

---
#### 参考项目
#### https://github.com/CatVodTVOfficial/TVBoxOSC
#### https://github.com/q215613905/TVBoxOS
#### https://github.com/takagen99/Box

---
#### 数据源参考
</br>[饭太硬主页](http://饭太硬.top)</br></br>

---
#### 简介
TVBox 简易修改 多源版本 支持安卓4.4

除box本来功能之外，其他主要修改实现的功能：
- 直播源配置支持m3u格式和tvbox默认格式
- 对于触屏设备，在某些线路首页加载过慢，在卡主页时点击app名字可以直接进入设置调整线路选择；已经加载完成之后点击app名字进入应用列表。设置按钮移到右上角，可以在加载时通过电机设置按钮进入设置界面。
- 增加简单更新功能，目的是为了提醒有可用的新版本，有可用更新时，在设置界面的“检测更新”会显示小红点提醒，点击忽略更新后不在用小红点提醒，但是可以通过点击检测更新来实现更新。**永远不会强制更新**。此版本（玉幂草_SHJ_202311080127.apk）之前的都没有更新模块，如果不喜欢提醒的话可以选择其他之前版本。
- 其他默认功能的小修小补
- ~~添加自定义GITHUB加速站，可以选择速度快的镜像站来获取基础信息~~
- 添加多仓多线路的处理逻辑，例如：
```
{
    "urls": [
        {
            "url": "http://www.饭太硬.com/tv/",
            "name": "🚀饭太硬线路"
        },
        {
            "url": "http://ok321.top/tv/",
            "name": "🚀OK线路"
        }
    ]
}
```
```
{
  "storeHouse": [
    {
      "sourceName": "默认",
      "sourceUrl": "https://raw.githubusercontent.com/mlabalabala/TVResource/main/boxCfg/ori_source.json"
    },
    {
      "sourceName": "起飞",
      "sourceUrl": "https://raw.githubusercontent.com/mlabalabala/TVResource/main/boxCfg/sp_source.json"
    }
  ]
}
```

- 应用名更改位置```app/src/main/res/values/strings.xml```
- 默认线路修改：
  - 在 ```app/src/main/java/com/github/tvbox/osc/bbox/constant/URL.java``` 中修改 ```DEFAULT_API_URL``` 为自己的线路配置URL
  - 在 ```app/src/main/java/com/github/tvbox/osc/bbox/base/App.java```中```putDefaultApis()```方法中添加代码（或者解开注释）
      ```
      // 添加默认线路
      putDefault(HawkConfig.API_URL, defaultApi);
      putDefault(HawkConfig.API_NAME, defaultApiName);
      putDefault(HawkConfig.API_NAME_HISTORY, defaultApiHistory);
      putDefault(HawkConfig.API_MAP, defaultApiMap);
      ```

- [自建仓库](https://raw.githubusercontent.com/mlabalabala/TVResource/main/boxCfg/default) (需要魔砝，或者自己根据加速站拼接URL)
- 蓝奏云限制分享apk文件，大家自行打包吧 。放一个链接，有办法的同学可以自己下载吧 [**码：6111**](https://bunny6111.lanzouq.com/b04whwgwj)
- 蓝奏云中LIVE版自用即可，可开机自启，直播资源来源于网络，可以自定义配置直播源（配置完需要重启应用生效）
### 测试可能不太够，有BUG请提issue
### Actions中有生成脚本

## Star History

<a href="https://star-history.com/#mlabalabala/box&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=mlabalabala/box&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=mlabalabala/box&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=mlabalabala/box&type=Date" />
 </picture>
</a>
