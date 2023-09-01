package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.support.DiscordAccountConfig;
import com.github.novicezk.midjourney.support.DiscordAccountConfigPool;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author NpcZZZZZZ
 * @version 1.0
 * @email 946123601@qq.com
 * @date 2023/6/28
 **/
@RequiredArgsConstructor
public class UserLoadBalancerServiceImpl implements LoadBalancerService {
    private final DiscordAccountConfigPool discordAccountConfigPool;

    /**
     * 获取轮询的key
     * @return String
     */
    @Override
    public String getLoadBalancerKey() {
        List<DiscordAccountConfig> discordAccountOpenList = discordAccountConfigPool.getDiscordAccountConfigList()
                .stream()
                .filter(item->BooleanUtils.isTrue(item.getOpenFlag()))
                .collect(Collectors.toList());
        int size = discordAccountOpenList.size();
        int i = getAndIncrement() % size;
        DiscordAccountConfig discordAccountConfig = discordAccountOpenList.get(i);
        return discordAccountConfig.getGuildId() + ":" + discordAccountConfig.getChannelId();
    }

    /**
     * 根据key获取配置
     * @param key
     * @return DiscordAccountConfig
     */
    @Override
    public DiscordAccountConfig getDiscordAccountConfigByKey(String key) {
        return discordAccountConfigPool.getDiscordAccountConfigList()
                .stream()
                .filter(x-> StringUtils.equals(key,x.getGuildId() + ":" + x.getChannelId()))
                .findFirst().orElse(null);
    }
}
