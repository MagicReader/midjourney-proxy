package com.github.novicezk.midjourney.util;

import com.github.novicezk.midjourney.enums.TaskAction;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpsacleUtils {

    public static String getUpKey(TaskAction taskAction) {
        if (taskAction.equals(TaskAction.UPSCALE)){
            return "upsample";
        }
        else if(taskAction.equals(TaskAction.UPSCALE_SUBTLE)){
            return "upsample_v6_2x_subtle";
        }
        else if(taskAction.equals(TaskAction.UPSCALE_CREATIVE)){
            return "upsample_v6_2x_creative";
        }
        else if(taskAction.equals(TaskAction.UPSCALE_2X)){
            return "upsample_v5_2x";
        }
        else if(taskAction.equals(TaskAction.UPSCALE_4X)){
            return "upsample_v5_4x";
        }
        return null;
    }

    public static String getSolo(TaskAction taskAction) {
        if(taskAction.equals(TaskAction.UPSCALE)){
            return "";
        }
        return "::SOLO";
    }

    public static TaskAction getTaskActionByType(String type) {
        if("Subtle".equalsIgnoreCase(type)){
            return TaskAction.UPSCALE_SUBTLE;
        }
        else if("Creative".equalsIgnoreCase(type)){
            return TaskAction.UPSCALE_CREATIVE;
        }
        else if("2x".equalsIgnoreCase(type)){
            return TaskAction.UPSCALE_2X;
        }
        else if("4x".equalsIgnoreCase(type)){
            return TaskAction.UPSCALE_4X;
        }
        return null;
    }
}
