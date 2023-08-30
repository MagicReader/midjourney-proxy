package com.github.novicezk.midjourney.wss.handle;

import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.SubmitInfoDescription;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
public class InfoMessageHandler extends MessageHandler{
    private static final String VISIBILITY_MODE_REGEX = "Visibility Mode\\*\\*:\\s*Stealth";
    private static final String FAST_TIME_REMAIN_REGEX = "Fast Time Remaining\\*\\*:\\s*(.*?)/(.*?)\\s*hours\\s*\\((.*?)%\\)";
    private static final String LIFETIME_USAGE_REGEX = "Lifetime Usage\\*\\*:\\s*(.*?)\\s*images\\s*\\((.*?)\\s*hours\\)";

    @Override
    public void handle(MessageType messageType, DataObject message) {
        Optional<DataObject> interaction = message.optObject("interaction");
        if (interaction.isEmpty() || !"info".equals(interaction.get().getString("name"))) {
            return;
        }
        DataArray d2Embeds = message.getArray("embeds");
        if (d2Embeds.isEmpty()) {
            return;
        }
        DataObject InfoObject = d2Embeds.getObject(0);
        String description = InfoObject.getString("description");
        String channelId = message.getString("channel_id");
        log.info("[/info] channel_id->{},description->{}",channelId,description);
        if(StringUtils.isAnyBlank(channelId, description)){
            log.error("[处理info信息-失败]channelId或description为空，channel_id->{},description->{}",channelId,description);
            return;
        }
        TaskCondition condition = new TaskCondition()
                .setActionSet(Set.of(TaskAction.INFO))
                .setStatusSet(Set.of(TaskStatus.NOT_START,TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS))
                .setChannelId(channelId);
        Task task = this.taskQueueHelper.findRunningTask(condition)
                .min(Comparator.comparing(Task::getSubmitTime))
                .orElse(null);
        if (task == null) {
            log.error("[处理info信息-失败]task查询为空，condition->{}",condition);
            return;
        }
        log.info("[处理info信息-成功]task.awake()，task->{}",task);
        task.setDescription(description);
        task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, message.getString("id"));
        task.setProperty(Constants.TASK_PROPERTY_SUBMIT_INFO_DESCRIPTION, parseDescription(description));
        task.success();
        task.awake();
    }

    private SubmitInfoDescription parseDescription(String description){
        Pattern patternVisibility = Pattern.compile(VISIBILITY_MODE_REGEX);
        Matcher matcherVisibility = patternVisibility.matcher(description);
        boolean stealthMode = matcherVisibility.find();
        Pattern patternRemain = Pattern.compile(FAST_TIME_REMAIN_REGEX);
        Matcher matcherRemain = patternRemain.matcher(description);
        if (!matcherRemain.find()) {
            log.error("info-description正则匹配异常->{}",description);
        }
        Pattern patternUsage = Pattern.compile(LIFETIME_USAGE_REGEX);
        Matcher matcherUsage = patternUsage.matcher(description);
        if (!matcherUsage.find()) {
            log.error("info-description正则匹配异常->{}",description);
        }
        SubmitInfoDescription submitInfoDescription = new SubmitInfoDescription()
                .setFastTimeRemainHours(Float.valueOf(matcherRemain.group(1)))
                .setFastTimeTotalHours(Float.valueOf(matcherRemain.group(2)))
                .setFastTimeRemainPercent(Float.valueOf(matcherRemain.group(3)))
                .setLifetimeUsageImages(Integer.valueOf(matcherUsage.group(1)))
                .setLifetimeUsageHours(Float.valueOf(matcherUsage.group(2)))
                .setStealthMode(stealthMode);
        return submitInfoDescription;
    }

}
