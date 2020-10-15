package com.baidu.shop.service;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Api
public interface TemplateService {

    @ApiOperation(value = "通过spuId生成静态HTML文件")
    @GetMapping(value = "template/createStaticHTMLTemplate")
    Result<JSONObject> createStaticHTMLTemplate(Integer spuId);

    @ApiOperation(value = "初始化静态HTML文件")
    @GetMapping(value = "template/initStaticHTMLTemplate")
    Result<JSONObject> initStaticHTMLTemplate();

    @ApiOperation(value = "通过spuId删除文件")
    @DeleteMapping(value = "template/delHTMLBySpuId")
    Result<JSONObject> delHTMLBySpuId(Integer spuId);

}
