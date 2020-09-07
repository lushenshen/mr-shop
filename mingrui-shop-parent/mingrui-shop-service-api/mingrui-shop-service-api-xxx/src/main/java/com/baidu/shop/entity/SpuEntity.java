package com.baidu.shop.entity;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @ClassName SupEntity
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/7
 * @Version V1.0
 **/
@Data
@Table(name = "tb_spu")
public class SpuEntity {
    @Id
    private Integer id;

    private String title;

    private String subTitle;

    private Integer cid1;

    private Integer cid2;

    private Integer cid3;

    private Integer brandId;

    private Integer saleable;

    private Integer valid;

    private Date createTime;

    private Date lastUpdateTime;


}
