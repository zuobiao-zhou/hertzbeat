---
id: alert_feishu
title: 告警飞书机器人通知      
sidebar_label: 告警飞书机器人通知     
keywords: [告警飞书机器人通知, 开源告警系统, 开源监控告警系统]
---

> 阈值触发后发送告警信息，通过飞书机器人通知到接收人。

### 操作步骤

1. **【飞书客户端】-> 【群设置】-> 【群机器人】-> 【添加机器人】 -> 【自定义机器人】 -> 【设置机器人名称头像】-> 【添加成功后复制其WebHook地址】**

2. **【保存机器人的WebHook地址的KEY值】**

    > 例如： webHook地址：`https://open.feishu.cn/open-apis/bot/v2/hook/3adafc96-23d0-4cd5-8feb-17f6e0b5fcs4`
    >
    > 其机器人KEY值为 `3adafc96-23d0-4cd5-8feb-17f6e0b5fcs4`

3. **【告警通知】->【新增接收人】 ->【选择飞书机器人通知方式】->【设置飞书机器人KEY】-> 【确定】**

4. **配置关联的告警通知策略⚠️ 【新增通知策略】-> 【将刚设置的接收人关联】-> 【确定】**

    > **注意⚠️ 新增了接收人并不代表已经生效可以接收告警信息，还需配置关联的告警通知策略，即指定哪些消息发给哪些接收人**。

    ![email](/img/docs/help/alert-notice-4.png)

### 飞书机器人通知常见问题

1. 飞书群未收到机器人告警通知

    > 请排查在告警中心是否已有触发的告警信息
    > 请排查是否配置正确机器人KEY，是否已配置告警策略关联

2. 如何在告警通知中@某人

    > 在新增接收人的表单中，填写 `用户ID` 。如果需要 @所有人，可以在 `用户ID` 字段中填入 `all`。同时支持填写多个用户id，用逗号 `,` 分隔。获取飞书用户id的具体方法，请参考：[获取飞书用户ID](https://open.feishu.cn/document/faq/trouble-shooting/how-to-get-internal-user-id)

其它问题可以通过交流群ISSUE反馈哦！
