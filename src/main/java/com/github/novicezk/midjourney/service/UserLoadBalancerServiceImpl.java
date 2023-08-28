package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.ProxyProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author NpcZZZZZZ
 * @version 1.0
 * @email 946123601@qq.com
 * @date 2023/6/28
 **/
@RequiredArgsConstructor
public class UserLoadBalancerServiceImpl implements LoadBalancerService {
    private final ProxyProperties properties;
    private volatile Map<String, ProxyProperties.DiscordConfig.DiscordAccountConfig> discordAccountConfigMap;

    @Override
    public String getLoadBalancerKey() {
        List<ProxyProperties.DiscordConfig.DiscordAccountConfig> discordAccountConfigList = properties.getDiscord().getDiscordAccountConfigList();
        int size = discordAccountConfigList.size();
        int i = getAndIncrement() % size;
        ProxyProperties.DiscordConfig.DiscordAccountConfig discordAccountConfig = discordAccountConfigList.get(i);
        return discordAccountConfig.getGuildId() + ":" + discordAccountConfig.getChannelId();
    }

    /**
     * 根据key获取配置
     * @param key
     * @return DiscordAccountConfig
     */
    @Override
    public ProxyProperties.DiscordConfig.DiscordAccountConfig getDiscordAccountConfigByKey(String key) {
        if(Objects.isNull(discordAccountConfigMap)){
            this.initDiscordAccountConfigMap();
        }
        return discordAccountConfigMap.get(key);
    }

    /**
     * 初始化map：key->config
     * @return String
     */
    private synchronized void initDiscordAccountConfigMap() {
        if(Objects.nonNull(discordAccountConfigMap)){
            return;
        }
        List<ProxyProperties.DiscordConfig.DiscordAccountConfig> discordAccountConfigList = properties.getDiscord().getDiscordAccountConfigList();
        discordAccountConfigMap = discordAccountConfigList.stream().collect(Collectors.toMap(x -> x.getGuildId() + ":" + x.getChannelId(), Function.identity()));
    }
}
