package com.github.novicezk.midjourney.controller;

import com.github.novicezk.midjourney.dto.Url2base64DTO;
import com.github.novicezk.midjourney.util.Base64ImgUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;



@Api(tags = "自定义接口")
@RestController
@RequestMapping("/my")
public class MyController {
    @Resource
    private Base64ImgUtils base64ImgUtils;

    @ApiOperation(value = "查询URL转Base64")
    @PostMapping("/url2base64")
    public Url2base64DTO url2base64(@RequestBody Url2base64DTO dto) throws Exception {
        String base64 = base64ImgUtils.generateImgUrlToBase64(dto.getUrl());
        dto.setBase64(base64);
        return dto;
    }

}
