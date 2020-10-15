package com.baidu.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName UserDTO
 * @Description: TODO
 * @Author shenyaqi
 * @Date 2020/9/20
 * @Version V1.0
 **/
@Data
@ApiModel(value = "用户DTO")
public class UserDTO {

    @ApiModelProperty(value = "用户主键",example = "1")
    private Integer id;

    @ApiModelProperty(value = "账户")
    private String username;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "手机号")
    private String phone;

    private Date created;

    private String salt;
}











































