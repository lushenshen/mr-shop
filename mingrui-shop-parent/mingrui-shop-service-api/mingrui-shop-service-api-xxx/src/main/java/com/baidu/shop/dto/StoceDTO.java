package com.baidu.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName StoceDTO
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/8
 * @Version V1.0
 **/
@Data
@ApiModel(value = "库存数据传输类")
public class StoceDTO {

    @ApiModelProperty(value = "sku主键",example = "1")
    private Long skuId;

    @ApiModelProperty(value = "可秒杀库存",example = "1")
    private Integer seckillStock;

    @ApiModelProperty(value = "秒杀总数量",example = "1")
    private Integer seckillTotal;

    @ApiModelProperty(value = "库存数量",example = "1")
    private Integer stock;
}