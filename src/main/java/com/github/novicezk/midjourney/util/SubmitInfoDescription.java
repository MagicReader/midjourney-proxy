package com.github.novicezk.midjourney.util;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SubmitInfoDescription {
    private Float fastTimeRemainHours;
    private Float fastTimeTotalHours;
    private Float fastTimeRemainPercent;
    private Integer lifetimeUsageImages;
    private Float lifetimeUsageHours;
    private Boolean stealthMode;
}
