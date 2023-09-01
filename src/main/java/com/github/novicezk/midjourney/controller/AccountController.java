package com.github.novicezk.midjourney.controller;

import com.github.novicezk.midjourney.service.AccountService;
import com.github.novicezk.midjourney.support.DiscordAccountConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "账号调度器")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @ApiOperation(value = "查询所有账号配置")
    @GetMapping("/configs")
    public List<DiscordAccountConfig> queryAccountConfigs(){
        return accountService.queryAccountConfigs();
    }

    @ApiOperation(value = "更新其中一个账号配置")
    @PostMapping("/config")
    public DiscordAccountConfig updateAccountConfig(@RequestBody DiscordAccountConfig discordAccountConfig) throws Exception {
        return accountService.updateAccountConfig(discordAccountConfig);
    }

}
