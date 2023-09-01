package com.github.novicezk.midjourney.support;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ApiModel("账号配置")
public class DiscordAccountConfig {
    @ApiModelProperty(value = "服务器id", required = true)
    private String guildId;

    @ApiModelProperty(value = "频道id", required = true)
    private String channelId;

    @ApiModelProperty(value = "登录token", required = true)
    private String userToken;

    @ApiModelProperty("是否允许账号出图")
    private Boolean openFlag = false;

    @ApiModelProperty("会话id")
    private String sessionId = "9c4055428e13bcbf2248a6b36084c5f3";
}
