package com.github.novicezk.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.UVContentParseData;
import com.github.novicezk.midjourney.util.UpsacleUtils;
import com.github.novicezk.midjourney.util.UpscaleNextContentParseData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * upscale消息处理.
 * 开始(create): Upscaling image #1 with **apple tree --version 6.0** - <@1129339974626586644> (Waiting to start)
 * 进度(update): **apple tree --version 6.0** - Upscaling (Creative) by <@1129339974626586644> (15%) (fast, stealth)
 * 完成(create): **apple tree --version 6.0** - Upscaled (Creative) by <@1129339974626586644> (fast, stealth)
 */
@Slf4j
@Component
public class UpscaleNextMessageHandler extends MessageHandler {
	private static final String START_CONTENT_REGEX = "Upscaling image #(\\d) with \\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - Upscaling \\((.*?)\\) by <@\\d+> \\((.*?)\\)";
	private static final String END_CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - Upscaled \\((.*?)\\) by <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		if (MessageType.CREATE == messageType) {
			UpscaleNextContentParseData start = parseStart(content);
			if (start != null) {
				TaskCondition condition = new TaskCondition()
						.setActionSet(Set.of(TaskAction.UPSCALE_SUBTLE, TaskAction.UPSCALE_CREATIVE, TaskAction.UPSCALE_2X, TaskAction.UPSCALE_4X))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Predicate<Task> taskPredicate = this.discordHelper.taskPredicate(condition, start.getPrompt());
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate)
						.min(Comparator.comparing(Task::getSubmitTime))
						.orElse(null);
				if (task == null) {
					return;
				}
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
				return;
			}
			UpscaleNextContentParseData end = parseEnd(content);
			if (end != null) {
				TaskAction taskActionByType = UpsacleUtils.getTaskActionByType(end.getType());
				TaskCondition condition = new TaskCondition()
						.setActionSet(Set.of(taskActionByType))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Predicate<Task> taskPredicate = this.discordHelper.taskPredicate(condition, end.getPrompt());
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate)
						.min(Comparator.comparing(Task::getSubmitTime))
						.orElse(null);
				if (task == null) {
					return;
				}
				finishTask(task, message);
				task.awake();
				return;
			}
		} else if (MessageType.UPDATE == messageType) {
			UpscaleNextContentParseData parseData = parse(content);
			if (parseData == null || CharSequenceUtil.equalsAny(parseData.getStatus(), "relaxed")) {
				return;
			}
			TaskAction taskActionByType = UpsacleUtils.getTaskActionByType(parseData.getType());
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(taskActionByType))
					.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
			Predicate<Task> taskPredicate = this.discordHelper.taskPredicate(condition, parseData.getPrompt());
			Task task = this.taskQueueHelper.findRunningTask(taskPredicate)
					.findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, message.getString("id"));
			task.setStatus(TaskStatus.IN_PROGRESS);
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
	}

	private UpscaleNextContentParseData parseStart(String content) {
		Matcher matcher = Pattern.compile(START_CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UpscaleNextContentParseData parseData = new UpscaleNextContentParseData();
		parseData.setIndex(Integer.parseInt(matcher.group(1)));
		parseData.setPrompt(matcher.group(2));
		parseData.setStatus(matcher.group(3));
		return parseData;
	}

	private UpscaleNextContentParseData parseEnd(String content) {
		Matcher matcher = Pattern.compile(END_CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UpscaleNextContentParseData parseData = new UpscaleNextContentParseData();
		parseData.setPrompt(matcher.group(1));
		parseData.setType(matcher.group(2));
		parseData.setStatus(matcher.group(3));
		return parseData;
	}

	private UpscaleNextContentParseData parse(String content) {
		Matcher matcher = Pattern.compile(CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UpscaleNextContentParseData parseData = new UpscaleNextContentParseData();
		parseData.setPrompt(matcher.group(1));
		parseData.setType(matcher.group(2));
		parseData.setStatus(matcher.group(3));
		return parseData;
	}


}
