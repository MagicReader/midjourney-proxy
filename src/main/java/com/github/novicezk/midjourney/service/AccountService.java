package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.support.DiscordAccountConfig;

import java.util.List;

public interface AccountService {
    List<DiscordAccountConfig> queryAccountConfigs();

    DiscordAccountConfig updateAccountConfig(DiscordAccountConfig discordAccountConfig) throws Exception;
}
