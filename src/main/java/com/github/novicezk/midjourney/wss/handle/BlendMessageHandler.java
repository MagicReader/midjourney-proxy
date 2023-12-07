package com.github.novicezk.midjourney.wss.handle;


import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.ContentParseData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * blend消息处理.
 * 开始(create): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (Waiting to start)
 * 进度(update): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (0%) (relaxed)
 * 完成(create): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (relaxed)
 */
@Slf4j
@Component
public class BlendMessageHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";

	private static final HashMap map = new HashMap<>();

	@Override
	public void handle(MessageType messageType, DataObject message) {
		Optional<DataObject> interaction = message.optObject("interaction");
		String content = getMessageContent(message);
		boolean match = CharSequenceUtil.startWith(content, "**<" + DiscordHelper.SIMPLE_URL_PREFIX) || (interaction.isPresent() && "blend".equals(interaction.get().getString("name")));
		if (!match) {
			return;
		}
		ContentParseData parseData = parse(content);
		if (parseData == null) {
			return;
		}
		if (MessageType.CREATE == messageType) {
			if ("Waiting to start".equals(parseData.getStatus())) {
				// 开始
				List<String> urls = CharSequenceUtil.split(parseData.getPrompt(), "> <https");
				if (urls.isEmpty()) {
					return;
				}
				TaskCondition condition = new TaskCondition()
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Predicate<Task> taskPredicate = this.discordHelper.taskPredicate(condition, parseData.getPrompt());
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate).findFirst().orElse(null);
				if (task == null) {
					return;
				}
				map.put(parseData.getPrompt(), task.getId());
				task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, message.getString("id"));
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
			} else {
				String taskId = (String) map.get(parseData.getPrompt());
				if(StringUtils.isBlank(taskId)){
					return;
				}
				// 完成
				TaskCondition condition = new TaskCondition()
						.setId(taskId)
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
				finishTask(task, message);
				task.awake();
				map.remove(parseData.getPrompt());
			}
		} else if (MessageType.UPDATE == messageType) {
			String taskId = (String) map.get(parseData.getPrompt());
			if(StringUtils.isBlank(taskId)){
				return;
			}
			// 进度
			TaskCondition condition = new TaskCondition()
					.setId(taskId)
					.setActionSet(Set.of(TaskAction.BLEND))
					.setStatusSet(Set.of(TaskStatus.IN_PROGRESS));
			Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, message.getString("id"));
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
	}

	private ContentParseData parse(String content) {
		Matcher matcher = Pattern.compile(CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		ContentParseData parseData = new ContentParseData();
		parseData.setPrompt(matcher.group(1));
		parseData.setStatus(matcher.group(2));
		return parseData;
	}

	private String getRealUrl(String url) {
		if (CharSequenceUtil.startWith(url, "<" + DiscordHelper.SIMPLE_URL_PREFIX)) {
			return this.discordHelper.getRealUrl(url.substring(1, url.length() - 1));
		}
		return url;
	}

}
