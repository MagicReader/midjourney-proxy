package com.github.novicezk.midjourney.support;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 账号池
 */
@Component
@Data
public class DiscordAccountConfigPool {

    private List<DiscordAccountConfig> discordAccountConfigList = new ArrayList<>();
}
