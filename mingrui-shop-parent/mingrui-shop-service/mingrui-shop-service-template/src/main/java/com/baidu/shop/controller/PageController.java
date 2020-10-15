package com.baidu.shop.controller;

import com.baidu.shop.service.PageService;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @ClassName PageController
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/23
 * @Version V1.0
 **/
//@Controller
//@RequestMapping(value = "/item")
public class PageController {

    //@Autowired
    private PageService pageService;

    //@Autowired
    private HttpServletRequest httpServletRequest;

    //@GetMapping(value = "/{spuId}.html")
    public String test(@PathVariable(value = "spuId") Integer spuId , ModelMap modelMap, HttpServletRequest httpServletRequest){

        Map<String,Object> map = pageService.getPageInfoBySpuId(spuId);
        modelMap.putAll(map);

        return "item";
    }
}
