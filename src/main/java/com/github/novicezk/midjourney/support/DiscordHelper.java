package com.github.novicezk.midjourney.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.util.Base64ImgUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DiscordHelper {
	private final ProxyProperties properties;

	private final Base64ImgUtils base64ImgUtils;

	private final StringRedisTemplate stringRedisTemplate;
	/**
	 * SIMPLE_URL_PREFIX.
	 */
	public static final String SIMPLE_URL_PREFIX = "https://s.mj.run/";
	/**
	 * DISCORD_SERVER_URL.
	 */
	public static final String DISCORD_SERVER_URL = "https://discord.com";
	/**
	 * DISCORD_CDN_URL.
	 */
	public static final String DISCORD_CDN_URL = "https://cdn.discordapp.com";
	/**
	 * DISCORD_WSS_URL.
	 */
	public static final String DISCORD_WSS_URL = "wss://gateway.discord.gg";

	public String getServer() {
		if (CharSequenceUtil.isBlank(this.properties.getNgDiscord().getServer())) {
			return DISCORD_SERVER_URL;
		}
		String serverUrl = this.properties.getNgDiscord().getServer();
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
		}
		return serverUrl;
	}

	public String getCdn() {
		if (CharSequenceUtil.isBlank(this.properties.getNgDiscord().getCdn())) {
			return DISCORD_CDN_URL;
		}
		String cdnUrl = this.properties.getNgDiscord().getCdn();
		if (cdnUrl.endsWith("/")) {
			cdnUrl = cdnUrl.substring(0, cdnUrl.length() - 1);
		}
		return cdnUrl;
	}

	public String getWss() {
		if (CharSequenceUtil.isBlank(this.properties.getNgDiscord().getWss())) {
			return DISCORD_WSS_URL;
		}
		String wssUrl = this.properties.getNgDiscord().getWss();
		if (wssUrl.endsWith("/")) {
			wssUrl = wssUrl.substring(0, wssUrl.length() - 1);
		}
		return wssUrl;
	}


	public String getRealPrompt(String prompt) {
		String realPrompt = prompt;
		String regex = "<https?://\\S+>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(realPrompt);
		while (matcher.find()) {
			String url = matcher.group();
			String realUrl = getRealUrl(url.substring(1, url.length() - 1));
			realPrompt = realPrompt.replace(url, realUrl);
		}
		return realPrompt;
	}

	private final String REAL_PROMPT_MD5_PREFIX = "real-prompt-md5:";

	public String getRealPromptMd5(String prompt) {
		String key = REAL_PROMPT_MD5_PREFIX + prompt;
		String realPrompt = stringRedisTemplate.opsForValue().get(key);
		if(StringUtils.isNotBlank(realPrompt)){
			return realPrompt;
		}
		realPrompt = getRealPrompt(prompt);
		String regex = "http[s]?://\\S+";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(realPrompt);
		while (matcher.find()) {
			String url = matcher.group();
			String md5 = base64ImgUtils.generateFileUrlToMd5(url);
			realPrompt = realPrompt.replace(url, md5).trim();
		}

		stringRedisTemplate.opsForValue().set(key, realPrompt);
		stringRedisTemplate.expire(key, 2, TimeUnit.MINUTES);
		return realPrompt;
	}

	public Predicate<Task> taskPredicate(TaskCondition condition, String prompt) {
		String promptMd5 = getRealPromptMd5(prompt);
		return condition.and(t -> {
			if(StringUtils.isBlank(t.getPromptEn())){
				return false;
			}
			String tPrompt = t.getPromptEn();
			String tPromptMd5 = getRealPromptMd5(tPrompt);
			return promptMd5.startsWith(tPromptMd5)  || tPromptMd5.startsWith(promptMd5) || prompt.startsWith(tPrompt) || tPrompt.startsWith(prompt);
		});
	}

	public String getRealUrl(String url) {
		if (!CharSequenceUtil.startWith(url, SIMPLE_URL_PREFIX)) {
			return url;
		}
		ResponseEntity<Void> res = getDisableRedirectRestTemplate().getForEntity(url, Void.class);
		if (res.getStatusCode() == HttpStatus.PERMANENT_REDIRECT) {
			return res.getHeaders().getFirst("Location");
		}
		return url;
	}

	public String findTaskIdWithCdnUrl(String url) {
		if (!CharSequenceUtil.startWith(url, DISCORD_CDN_URL)) {
			return null;
		}
		int hashStartIndex = url.lastIndexOf("/");
		String taskId = CharSequenceUtil.subBefore(url.substring(hashStartIndex + 1), ".", true);
		if (CharSequenceUtil.length(taskId) == 16) {
			return taskId;
		}
		return null;
	}

	private RestTemplate getDisableRedirectRestTemplate() {
		CloseableHttpClient httpClient = HttpClientBuilder.create()
				.disableRedirectHandling()
				.build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		return new RestTemplate(factory);
	}

}
