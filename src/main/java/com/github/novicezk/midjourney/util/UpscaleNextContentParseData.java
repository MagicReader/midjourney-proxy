package com.github.novicezk.midjourney.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpscaleNextContentParseData extends ContentParseData {
	protected Integer index;
	protected String type;
}
