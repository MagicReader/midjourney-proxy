package spring.config;

import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.service.LoadBalancerService;
import com.github.novicezk.midjourney.service.UserLoadBalancerServiceImpl;
import com.github.novicezk.midjourney.support.DiscordAccountConfigPool;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.wss.WebSocketStarter;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import com.github.novicezk.midjourney.wss.user.UserMessageListener;
import com.github.novicezk.midjourney.wss.user.UserWebSocketStarter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author NpcZZZZZZ
 * @version 1.0
 * @email 946123601@qq.com
 * @date 2023/6/28
 **/
@Configuration
@EnableConfigurationProperties(ProxyProperties.class)
@ConditionalOnProperty(prefix = "mj.discord", name = "user-wss", havingValue = "true")
public class UserWebSocketStarterConfig {

    @Bean
    public Map<String, WebSocketStarter> userWebSocketStarter(DiscordAccountConfigPool discordAccountConfigPool,
                                                              List<MessageHandler> messageHandlerList,
                                                              ProxyProperties properties,
                                                              DiscordHelper discordHelper) {
        ProxyProperties.DiscordConfig discord = properties.getDiscord();
        ProxyProperties.ProxyConfig proxy = properties.getProxy();
        return discordAccountConfigPool.getDiscordAccountConfigList().
                stream()
                .map(x -> new UserWebSocketStarter(
                        proxy.getHost(),
                        proxy.getPort(),
                        x.getUserToken(),
                        discord.getUserAgent(),
                        new UserMessageListener(x.getChannelId(), messageHandlerList), discordHelper))
                .collect(Collectors.toMap(UserWebSocketStarter::getUserToken, Function.identity()));
    }

    @Bean
    public LoadBalancerService userLoadBalancerService(DiscordAccountConfigPool discordAccountConfigPool) {
        return new UserLoadBalancerServiceImpl(discordAccountConfigPool);
    }
}
