package com.github.novicezk.midjourney.util;

import com.github.novicezk.midjourney.enums.TaskAction;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VariationUtils {

    public static String getVarKey(TaskAction taskAction) {
        if (taskAction.equals(TaskAction.VARIATION)){
            return "variation";
        }
        else if(taskAction.equals(TaskAction.VARIATION_SUBTLE)){
            return "low_variation";
        }
        else if(taskAction.equals(TaskAction.VARIATION_STRONG)){
            return "high_variation";
        }
        return null;
    }

    public static String getSolo(TaskAction taskAction) {
        if(taskAction.equals(TaskAction.VARIATION)){
            return "";
        }
        return "::SOLO";
    }

    public static TaskAction getTaskActionByType(String type) {
        if("Subtle".equalsIgnoreCase(type)){
            return TaskAction.VARIATION_SUBTLE;
        }
        else if("Strong".equalsIgnoreCase(type)){
            return TaskAction.VARIATION_STRONG;
        }
        return null;
    }
}
