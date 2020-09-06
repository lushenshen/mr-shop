package com.baidu.shop.entity;

import com.baidu.shop.validate.group.MingruiOperation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NonNull;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @ClassName CategoryEntity
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/8/27
 * @Version V1.0
 **/
@ApiModel(value = "分类实体类")
@Data
@Table(name = "tb_category")
public class CategoryEntity {

    @Id
    @ApiModelProperty(value = "分类主题",example = "1")
    @NotNull(message = "主键不能为空", groups = {MingruiOperation.Update.class})
    private Integer id;

    @ApiModelProperty(value = "类目名称",example = "1")
    @NotEmpty(message = "类目名称", groups = {MingruiOperation.Add.class,MingruiOperation.Update.class})
    private String name;

    @ApiModelProperty(value = "父类目id",example = "1")
    @NotNull(message = "父类目id不能为空", groups = {MingruiOperation.Add.class})
    private Integer parentId;

    @ApiModelProperty(value = "是否为父类节点,0为否,1为是",example = "0")
    @NotNull(message = "父类目id不能为空", groups = {MingruiOperation.Add.class})
    private  Integer isParent;

    @ApiModelProperty(value = "排序指数,越小越靠前",example = "0")
    @NotNull(message = "排序指数不能为空", groups = {MingruiOperation.Add.class})
    private Integer sort;
}
