package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.entity.SpuEntity;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.mapper.SpuMapper;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.PinyinUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.ir.FunctionNode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;


import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName BrandServiceImpl
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/8/31
 * @Version V1.0
 **/
@RestController
public class BrandServiceImpl extends BaseApiService implements BrandService {

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CategoryBrandMapper categoryBrandMapper;
    @Resource
    private SpuMapper spuMapper;

    @Override
    public Result<PageInfo<BrandEntity>> getBrandInfo(BrandDTO brandDTO) {

        //分页
        if (ObjectUtil.isNotNull(brandDTO.getPage()) && ObjectUtil.isNotNull(brandDTO.getRows()))
            PageHelper.startPage(brandDTO.getPage(), brandDTO.getRows());

        //排序
        Example example = new Example(BrandEntity.class);

        if (!StringUtils.isEmpty(brandDTO.getSort())) example.setOrderByClause(brandDTO.getOrderByClause());

        Example.Criteria criteria = example.createCriteria();
        if (ObjectUtil.isNotNull(brandDTO.getId()))
            criteria.andEqualTo("id", brandDTO.getId());

        if (!StringUtils.isEmpty(brandDTO.getName()))
            criteria.andLike("name", "%" + brandDTO.getName() + "%");

        List<BrandEntity> list = brandMapper.selectByExample(example);

        PageInfo<BrandEntity> pageInfo = new PageInfo<>(list);

        return this.setResultSuccess(pageInfo);
    }


    @Override
    public Result<JsonObject> saveBrand(BrandDTO brandDTO) {

        brandMapper.insertSelective(BaiduBeanUtil.copyProperties(brandDTO,BrandEntity.class));

        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO, BrandEntity.class);

        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0))
                , PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        brandMapper.insertSelective(brandEntity);

        if(brandDTO.getCategory().contains(",")){

            List<CategoryBrandEntity> categoryBrandEntities = Arrays.asList(brandDTO.getCategory().split(","))
                    .stream().map(cid -> {

                        CategoryBrandEntity entity = new CategoryBrandEntity();
                        entity.setCategoryId(StringUtil.toInteger(cid));
                        entity.setBrandId(brandEntity.getId());

                        return entity;
                    }).collect(Collectors.toList());

            //批量新增
            categoryBrandMapper.insertList(categoryBrandEntities);

        }else{
            //新增
            CategoryBrandEntity entity = new CategoryBrandEntity();

            entity.setCategoryId(StringUtil.toInteger(brandDTO.getCategory()));
            entity.setBrandId(brandEntity.getId());

            categoryBrandMapper.insertSelective(entity);
        }

        return this.setResultSuccess();
    }
    @Override
    @Transactional
    public Result<JsonObject> editBrand(BrandDTO brandDTO) {
        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO, BrandEntity.class);
        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0))
                , PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        //执行修改操作
        brandMapper.updateByPrimaryKeySelective(brandEntity);

        //通过brandID删除中间表的数据
        this.deleteCategoryAndBrand(brandEntity.getId());

        //新增新的数据
        this.insertCategoryAndBrand(brandDTO, brandEntity);

        return this.setResultSuccess();
    }


    @Override
    @Transactional
    public Result<JsonObject> deleteBrand(Integer id) {

        Example example = new Example(SpuEntity.class);
        example.createCriteria().andEqualTo("brandId", id);
        List<SpuEntity> spuEntityList = spuMapper.selectByExample(example);
        if (spuEntityList.size() > 0) {
            return this.setResultError(HTTPStatus.OPERATION_ERROR, "当前品牌被商品绑定,不能删除");
        }
        brandMapper.deleteByPrimaryKey(id);

        this.deleteCategoryAndBrand(id);

        return this.setResultSuccess();
    }


    @Override
    public Result<List<BrandEntity>> getBrandByCategory(Integer cid) {

        List<BrandEntity> list = brandMapper.getBrandByCategory(cid);

        return this.setResultSuccess(list);
    }

    @Override
    public Result<List<BrandEntity>> getBrandByIdList(String brandIdsStr) {

        List<Integer> brandIdList = Arrays.asList(brandIdsStr.split(","))
                .stream().map(brandIdStr -> Integer.parseInt(brandIdStr))
                .collect(Collectors.toList());

        List<BrandEntity> list = brandMapper.selectByIdList(brandIdList);

        return this.setResultSuccess(list);
    }

    private void deleteCategoryAndBrand(Integer id) {
        //通过brandID删除中间表的数据
        Example example = new Example(CategoryBrandEntity.class);
        example.createCriteria().andEqualTo("brandId",id);
        categoryBrandMapper.deleteByExample(example);
    }


    @Override
    public void insertCategoryAndBrand(BrandDTO brandDTO, BrandEntity brandEntity) {

        if(brandDTO.getCategory().contains(",")){

            List<CategoryBrandEntity> categoryBrandEntities = Arrays.asList(brandDTO.getCategory().split(","))
                    .stream().map(cid -> {

                        CategoryBrandEntity entity = new CategoryBrandEntity();
                        entity.setCategoryId(StringUtil.toInteger(cid));
                        entity.setBrandId(brandEntity.getId());

                        return entity;
                    }).collect(Collectors.toList());

            categoryBrandMapper.insertList(categoryBrandEntities);
        }else{

            CategoryBrandEntity entity = new CategoryBrandEntity();
            entity.setCategoryId(StringUtil.toInteger(brandDTO.getCategory()));
            entity.setBrandId(brandEntity.getId());

            categoryBrandMapper.insertSelective(entity);
        }
    }
}

























