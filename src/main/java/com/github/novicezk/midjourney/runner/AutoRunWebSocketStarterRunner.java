package com.github.novicezk.midjourney.runner;

import com.github.novicezk.midjourney.wss.WebSocketStarter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import spring.config.ArtAiCenterApiProperties;

import java.util.Map;

/**
 * @author NpcZZZZZZ
 * @version 1.0
 * @email 946123601@qq.com
 * @date 2023/6/28
 **/
@Component
@RequiredArgsConstructor
public class AutoRunWebSocketStarterRunner implements ApplicationRunner {
    private final Map<String, WebSocketStarter> webSocketStarterMap;
    private final RestTemplate restTemplate;
    private final ArtAiCenterApiProperties artAiCenterApiProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (Map.Entry<String, WebSocketStarter> entry : webSocketStarterMap.entrySet()) {
            entry.getValue().start();
        }
        String pullRequestUrl = artAiCenterApiProperties.getHost()+artAiCenterApiProperties.getPullRequest();
        if(StringUtils.isNotBlank(pullRequestUrl)){
            JSONObject res = restTemplate.getForObject(pullRequestUrl, JSONObject.class);
        }
    }
}
