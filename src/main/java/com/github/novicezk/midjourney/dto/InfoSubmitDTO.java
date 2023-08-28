package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@ApiModel("Info提交参数")
@EqualsAndHashCode(callSuper = true)
public class InfoSubmitDTO extends BaseSubmitDTO {

}
