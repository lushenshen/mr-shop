package com.baidu.shop.mapper;
import com.baidu.shop.entity.CategoryEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CategoryMapper extends Mapper<CategoryEntity> {

    @Select(value = "Select c.id,c.name from tb_category c where c.id in(select cb.category_id from tb_category_brand cb where cb.brand_id=#{brandId})")
    List<CategoryEntity> getByBrandId(Integer brandId);

    @Select(value ="Select count(1) from tb_category_brand where category_id = #{id}")
    Integer getByCategoryId(Integer id);

    @Select(value = "Select count(1) from tb_supc_group where cid = #{id    }")
    Integer getSepcGroup(Integer id);

}
