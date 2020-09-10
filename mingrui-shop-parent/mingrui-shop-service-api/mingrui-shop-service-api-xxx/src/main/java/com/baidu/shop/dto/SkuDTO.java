package com.baidu.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Table;
import java.util.Date;

/**
 * @ClassName SpuDetailDTO
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/8
 * @Version V1.0
 **/
@Data
@ApiModel(value = "SKU属性数据传输类")
public class SkuDTO {

    @ApiModelProperty(value = "主键",example = "1")
    private Long id;

    @ApiModelProperty(value = "SPU主键",example = "1")
    private Integer spuId;

    @ApiModelProperty(value = "商品标题",example = "1")
    private String title;

    @ApiModelProperty(value = "商品图片，多个图片以','分割")
    private String images;

    @ApiModelProperty(value = "销售价格，以分为单位",example = "1")
    private Integer price;

    @ApiModelProperty(value = "特有规格属性在spu属性模块中的对应下标组合")
    private String indexes;

    @ApiModelProperty(value = "sku的他有的规格参数键值对，json格式，反序列化时请使用inkedHashMap,保证有序")
    private String ownSpec;

    @ApiModelProperty(value = "是否有效，0无效，1有效",example = "1")
    private Boolean enable;

    @ApiModelProperty(value = "添加时间")
    private Date createTime;

    @ApiModelProperty(value = "最后修改时间")
    private Date lastUpdateTime;

    @ApiModelProperty(value = "库存")
    private Integer stock;
}


















