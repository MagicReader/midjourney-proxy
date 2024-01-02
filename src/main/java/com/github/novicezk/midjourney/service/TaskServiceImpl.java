package com.github.novicezk.midjourney.service;

import cn.hutool.core.util.IdUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.dto.BaseSubmitDTO;
import com.github.novicezk.midjourney.dto.InfoSubmitDTO;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.support.DiscordAccountConfig;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskQueueHelper;
import com.github.novicezk.midjourney.util.MimeTypeUtils;
import eu.maxschuster.dataurl.DataUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final LoadBalancerService loadBalancerService;
    private final TaskStoreService taskStoreService;
    private final Map<String, DiscordService> discordServiceMap;
    private final TaskQueueHelper taskQueueHelper;

    @Override
    public Task newTask(BaseSubmitDTO base) {
        return newTask(base,null);
    }

    @Override
    public Task newTask(BaseSubmitDTO base,String associationKey) {
        Task task = new Task();
        task.setId(IdUtil.getSnowflakeNextIdStr());
        task.setSubmitTime(System.currentTimeMillis());
        task.setState(base.getState());
        String notifyHook = base.getNotifyHook();
        task.setProperty(Constants.TASK_PROPERTY_NOTIFY_HOOK, notifyHook);
        task.setAssociationKey(StringUtils.isNotBlank(associationKey) ? associationKey : loadBalancerService.getLoadBalancerKey());
        DiscordAccountConfig discordAccountConfig = loadBalancerService.getDiscordAccountConfigByKey(task.getAssociationKey());
        task.setGuildId(discordAccountConfig.getGuildId());
        task.setChannelId(discordAccountConfig.getChannelId());
        return task;
    }

    @Override
    public List<SubmitResultVO> submitInfo(InfoSubmitDTO infoSubmitDTO) {
        List<SubmitResultVO> submitResultVOList = new ArrayList<>(discordServiceMap.size());
        discordServiceMap.forEach((associationKey, discordService)->{
            Task task = newTask(infoSubmitDTO, associationKey);
            task.setAction(TaskAction.INFO);
            String prompt = "/info --guildId ${guildId} --channelId ${channelId}"
                    .replace("${guildId}",task.getGuildId())
                    .replace("${channelId}",task.getChannelId());
            task.setPrompt(prompt);
            SubmitResultVO submitResultVO = this.taskQueueHelper.submitTask(task, () -> discordService.info());
            submitResultVOList.add(submitResultVO);
        });
        return submitResultVOList;
    }

    @Override
    public SubmitResultVO submitImagine(Task task, List<DataUrl> dataUrls) {
        return this.taskQueueHelper.submitTask(task, () -> {
            DiscordService discordService = this.discordServiceMap.get(task.getAssociationKey());
            List<String> imageUrls = new ArrayList<>();
            for (DataUrl dataUrl : dataUrls) {
                String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
                Message<String> uploadResult = discordService.upload(taskFileName, dataUrl);
                if (uploadResult.getCode() != ReturnCode.SUCCESS) {
                    return Message.of(uploadResult.getCode(), uploadResult.getDescription());
                }
                String finalFileName = uploadResult.getResult();
                Message<String> sendImageResult = discordService.sendImageMessage("upload image: " + finalFileName, finalFileName);
                if (sendImageResult.getCode() != ReturnCode.SUCCESS) {
                    return Message.of(sendImageResult.getCode(), sendImageResult.getDescription());
                }
                imageUrls.add(sendImageResult.getResult());
            }
            if (!imageUrls.isEmpty()) {
                task.setPrompt(String.join(" ", imageUrls) + " " + task.getPrompt());
                task.setPromptEn(String.join(" ", imageUrls) + " " + task.getPromptEn());
                task.setDescription("/imagine " + task.getPrompt());
                this.taskStoreService.save(task);
            }
            return discordService.imagine(task.getPromptEn());
        });
    }


    @Override
    public SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags) {
        return this.taskQueueHelper.submitTask(task, () -> this.discordServiceMap.get(task.getAssociationKey()).upscale(targetMessageId, index, targetMessageHash, messageFlags));
    }

    @Override
    public SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags) {
        return this.taskQueueHelper.submitTask(task, () -> this.discordServiceMap.get(task.getAssociationKey()).variation(targetMessageId, index, targetMessageHash, messageFlags));
    }

    @Override
    public SubmitResultVO submitDescribe(Task task, DataUrl dataUrl) {
        return this.taskQueueHelper.submitTask(task, () -> {
            String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
            Message<String> uploadResult = this.discordServiceMap.get(task.getAssociationKey()).upload(taskFileName, dataUrl);
            if (uploadResult.getCode() != ReturnCode.SUCCESS) {
                return Message.of(uploadResult.getCode(), uploadResult.getDescription());
            }
            String finalFileName = uploadResult.getResult();
            return this.discordServiceMap.get(task.getAssociationKey()).describe(finalFileName);
        });
    }

    @Override
    public SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions) {
        return this.taskQueueHelper.submitTask(task, () -> {
            List<String> finalFileNames = new ArrayList<>();
            List<String> finalImageUrls = new ArrayList<>();
            for (DataUrl dataUrl : dataUrls) {
                String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
                Message<String> uploadResult = this.discordServiceMap.get(task.getAssociationKey()).upload(taskFileName, dataUrl);
                if (uploadResult.getCode() != ReturnCode.SUCCESS) {
                    return Message.of(uploadResult.getCode(), uploadResult.getDescription());
                }
                String finalFileName = uploadResult.getResult();
                Message<String> sendImageResult = this.discordServiceMap.get(task.getAssociationKey()).sendImageMessage("upload image: " + finalFileName, finalFileName);
                if (sendImageResult.getCode() != ReturnCode.SUCCESS) {
                    return Message.of(sendImageResult.getCode(), sendImageResult.getDescription());
                }
                finalImageUrls.add(sendImageResult.getResult());
                finalFileNames.add(uploadResult.getResult());
            }
            String prompt = StringUtils.join(finalImageUrls," ");
            task.setPrompt(prompt);
            task.setPromptEn(prompt);
            return this.discordServiceMap.get(task.getAssociationKey()).blend(finalFileNames, dimensions);
        });
    }

}
