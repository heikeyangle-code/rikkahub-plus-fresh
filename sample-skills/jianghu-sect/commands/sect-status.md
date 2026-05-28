---
description: 显示当前江湖门派的完整状态卡
allowed-tools: Read, Write
argument-hint: 无参数
disable-model-invocation: false
---

# /sect-status — 门派状态

读取 `~/.rikkahub/skills/jianghu-sect/` 下的门派数据文件，显示当前门派的完整信息卡。

格式：
```
┌─ 【门派名】 ─────────────────────┐
│  地位：xxx                        │
│  位置：xxx                        │
│  绝学：Lv.x                       │
│  弟子：N人（列出重要弟子）         │
│  声望：N                          │
│  财力：N                          │
│  分舵：N处                        │
│  盟友：xxx                        │
│  仇敌：xxx                        │
│  江湖纪事：                       │
│  · 最近发生的事                   │
│  · ...                            │
└──────────────────────────────────┘
```

如果没有门派数据，提示用户先创建门派。
