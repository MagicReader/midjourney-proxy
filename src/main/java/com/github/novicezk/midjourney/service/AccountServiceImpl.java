package com.github.novicezk.midjourney.service;

import cn.hutool.core.io.resource.ResourceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.support.DiscordAccountConfig;
import com.github.novicezk.midjourney.support.DiscordAccountConfigPool;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.TaskQueueHelper;
import com.github.novicezk.midjourney.wss.WebSocketStarter;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import com.github.novicezk.midjourney.wss.user.UserMessageListener;
import com.github.novicezk.midjourney.wss.user.UserWebSocketStarter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{
    private final RestTemplate restTemplate;
    private final ProxyProperties properties;
    private final DiscordAccountConfigPool discordAccountConfigPool;
    private final DiscordHelper discordHelper;
    private final TaskQueueHelper taskQueueHelper;
    private final Map<String, DiscordService> discordServiceMap;
    private final Map<String, WebSocketStarter> webSocketStarterMap;
    private final List<MessageHandler> messageHandlerList;

    @Override
    public List<DiscordAccountConfig> queryAccountConfigs() {
        return discordAccountConfigPool.getDiscordAccountConfigList();
    }

    @Override
    public DiscordAccountConfig updateAccountConfig(DiscordAccountConfig newAccountConfig) throws Exception {
        DiscordAccountConfig oldAccountConfig = discordAccountConfigPool.getDiscordAccountConfigList()
                .stream()
                .filter(x-> StringUtils.equals(x.getGuildId(),newAccountConfig.getGuildId()))
                .filter(x-> StringUtils.equals(x.getChannelId(),newAccountConfig.getChannelId()))
                .findFirst().orElse(null);

        if(Objects.nonNull(oldAccountConfig)){
            // config存在->更新or重置
            if(StringUtils.equals(newAccountConfig.getUserToken(), oldAccountConfig.getUserToken())){
                log.info("MJ账号配置更新->{}",newAccountConfig);
                oldAccountConfig = newAccountConfig;
                return oldAccountConfig;
            }
            log.info("MJ账号配置重置->{}",newAccountConfig);
            discordAccountConfigPool.getDiscordAccountConfigList().remove(oldAccountConfig);
            removeDiscordService(oldAccountConfig);
            removeUserWebSocketStarter(oldAccountConfig);
        }else{
            // config不存在->新增
            log.info("MJ账号配置新增->{}",newAccountConfig);
        }
        addDiscordService(newAccountConfig);
        addUserWebSocketStarter(newAccountConfig);
        discordAccountConfigPool.getDiscordAccountConfigList().add(newAccountConfig);
        taskQueueHelper.updateThreadPoolTaskExecutorConfig();

        return newAccountConfig;
    }

    public DiscordService removeDiscordService(DiscordAccountConfig discordAccountConfig) {
        String key = discordAccountConfig.getGuildId() + ":" + discordAccountConfig.getChannelId();
        return discordServiceMap.remove(key);
    }

    public DiscordService addDiscordService(DiscordAccountConfig discordAccountConfig) {
        ProxyProperties.DiscordConfig discord = properties.getDiscord();
        String serverUrl = discordHelper.getServer();
        String key = discordAccountConfig.getGuildId() + ":" + discordAccountConfig.getChannelId();
        DiscordServiceImpl discordService = new DiscordServiceImpl(
                discordAccountConfig.getGuildId(),
                discordAccountConfig.getChannelId(),
                discordAccountConfig.getUserToken(),
                discordAccountConfig.getSessionId(),
                discord.getUserAgent(),
                serverUrl + "/api/v9/interactions",
                serverUrl + "/api/v9/channels/" + discordAccountConfig.getChannelId() + "/attachments",
                serverUrl + "/api/v9/channels/" + discordAccountConfig.getChannelId() + "/messages",
                ResourceUtil.readUtf8Str("api-params/imagine.json"),
                ResourceUtil.readUtf8Str("api-params/upscale.json"),
                ResourceUtil.readUtf8Str("api-params/variation.json"),
                ResourceUtil.readUtf8Str("api-params/reroll.json"),
                ResourceUtil.readUtf8Str("api-params/describe.json"),
                ResourceUtil.readUtf8Str("api-params/blend.json"),
                ResourceUtil.readUtf8Str("api-params/message.json"),
                ResourceUtil.readUtf8Str("api-params/info.json"),
                restTemplate);
        return discordServiceMap.put(key,discordService);
    }

    public WebSocketStarter removeUserWebSocketStarter(DiscordAccountConfig discordAccountConfig){
        String key = discordAccountConfig.getUserToken();
        WebSocketStarter webSocketStarter = webSocketStarterMap.remove(key);
        if(Objects.nonNull(webSocketStarter)){
            webSocketStarter.close("update config");
        }
        return webSocketStarter;
    }

    public WebSocketStarter addUserWebSocketStarter(DiscordAccountConfig discordAccountConfig) throws Exception {
        String key = discordAccountConfig.getUserToken();
        ProxyProperties.DiscordConfig discord = properties.getDiscord();
        ProxyProperties.ProxyConfig proxy = properties.getProxy();
        UserWebSocketStarter userWebSocketStarter = new UserWebSocketStarter(
                proxy.getHost(),
                proxy.getPort(),
                discordAccountConfig.getUserToken(),
                discord.getUserAgent(),
                new UserMessageListener(discordAccountConfig.getChannelId(), messageHandlerList),
                discordHelper);
        userWebSocketStarter.start();
        return webSocketStarterMap.put(key,userWebSocketStarter);
    }
}
