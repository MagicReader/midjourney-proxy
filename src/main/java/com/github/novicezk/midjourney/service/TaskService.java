package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.dto.BaseSubmitDTO;
import com.github.novicezk.midjourney.dto.InfoSubmitDTO;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.support.Task;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface TaskService {
	Task newTask(BaseSubmitDTO base);

	Task newTask(BaseSubmitDTO base,String associationKey);

	List<SubmitResultVO> submitInfo(InfoSubmitDTO infoSubmitDTO);

	SubmitResultVO submitImagine(Task task, List<DataUrl> dataUrls);

	SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index,  int messageFlags);

	SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags);

	SubmitResultVO submitDescribe(Task task, DataUrl dataUrl);

	SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions);
}
