package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.component.MrRabbitMQ;
import com.baidu.shop.constant.MqMessageConstant;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.mapper.*;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/7
 * @Version V1.0
 **/
@RestController
public class GoodsServiceImpl extends BaseApiService implements GoodsService {

    @Resource
    private SpuMapper spuMapper;

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private StockMapper stockMapper;

    @Resource
    private SupDetailMapper supDetailMapper;

    @Autowired
    private MrRabbitMQ mrRabbitMQ;

    @Override
    public Result<List<SpuDTO>> getSpuInfo(SpuDTO spuDTO) {

        //分页
        if(ObjectUtil.isNotNull(spuDTO.getPage())
                && ObjectUtil.isNotNull(spuDTO.getRows()))
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());

        //构建条件查询
        Example example = new Example(SpuEntity.class);
        Example.Criteria criteria = example.createCriteria();

        if(StringUtil.isNotEmpty(spuDTO.getTitle()))
            criteria.andLike("title","%" + spuDTO.getTitle() + "%");
        if(ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2)
            criteria.andEqualTo("saleable",spuDTO.getSaleable());
        if(ObjectUtil.isNotNull(spuDTO.getId()))
            criteria.andEqualTo("id",spuDTO.getId());

        //排序
        if(ObjectUtil.isNotNull(spuDTO.getSort()))
            example.setOrderByClause(spuDTO.getOrderByClause());

        List<SpuEntity> list = spuMapper.selectByExample(example);

        //可以有优化的空间
        List<SpuDTO> dtos = list.stream().map(spu -> {

            BrandEntity brandEntity = brandMapper.selectByPrimaryKey(spu.getBrandId());
            //获取分类id-->转成数组-->通过idList查询出数据,得到数据集合-->调用stream函数-->调用map函数返回一个新的函数
            // -->调用Stream.collect转换集合-->调用Collectors.joining("/")将集合转换成/拼接的字符串
            // String categoryNames = categoryMapper.selectByIdList(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
            //    .stream()
            //    .map(category -> category.getName())
            //    .collect(Collectors.joining("/"));

            //得到id集合
            List<Integer> idArr = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());

            //通过Id集合查询数据
            List<CategoryEntity> categoryEntities = categoryMapper.selectByIdList(idArr);

            //map函数返回一个新的数组<String>category.getName()是string类型
            //Stream.collect()//集合的转换功能
            String categoryNames = categoryEntities.stream().map(category -> {
                return category.getName();
            }).collect(Collectors.joining("/")); //将集合转换成/拼接的字符串


            SpuDTO dto = BaiduBeanUtil.copyProperties(spu, SpuDTO.class);
            dto.setBrandName(brandEntity.getName());

            dto.setCategoryName(categoryNames);

            return dto;
        }).collect(Collectors.toList());

        PageInfo<SpuEntity> pageInfo = new PageInfo<>(list);

        //要返回的是dto的数据,但是pageinfo中没有总条数
        long total = pageInfo.getTotal();
        //借用一下message属性
        return this.setResult(HTTPStatus.OK,total + "",dtos);
    }

    @Transactional//jvm 虚拟机栈 -->入栈和出栈的问题
    @Override
    public Result<JSONObject> addInfo(SpuDTO spuDTO) {

        Integer spuId = addInfoTransaction(spuDTO);

        //@feign search template
        //发送消息
        mrRabbitMQ.send(spuId + "", MqMessageConstant.SPU_ROUT_KEY_SAVE);

        return this.setResultSuccess();
    }

    @Transactional
    public Integer addInfoTransaction(SpuDTO spuDTO){

        //JDK的动态代理
        //cglib动态代理
        //aspectj动态代理

        Date date = new Date();

        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setSaleable(1);
        spuEntity.setValid(1);
        spuEntity.setCreateTime(date);
        spuEntity.setLastUpdateTime(date);
        //新增spu
        spuMapper.insertSelective(spuEntity);

        Integer spuId = spuEntity.getId();
        //新增spudetail
        SpuDetailEntity spuDetailEntity = BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(), SpuDetailEntity.class);
        spuDetailEntity.setSpuId(spuId);
        supDetailMapper.insertSelective(spuDetailEntity);

        this.addSkusAndStocks(spuDTO.getSkus(),spuId,date);

        return spuEntity.getId();
    }

    private void addSkusAndStocks(List<SkuDTO> skus, Integer spuId, Date date){
        skus.stream().forEach(skuDTO -> {
            //新增sku!!!!!
            SkuEntity skuEntity = BaiduBeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuId);
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);
        });
    }


    @Override
    public Result<JSONObject> editGoodsInfo(SpuDTO spuDTO) {

        this.editInfoTransaction(spuDTO);

        mrRabbitMQ.send(spuDTO.getId() + "", MqMessageConstant.SPU_ROUT_KEY_UPDATE);

        return this.setResultSuccess();
    }

    @Transactional
    public void editInfoTransaction(SpuDTO spuDTO){


        Date date = new Date();
        //修改spu信息
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setLastUpdateTime(date);//!!!!!!!!!设置最后更新时间

        spuMapper.updateByPrimaryKeySelective(spuEntity);
        //修改spudetail
        supDetailMapper.updateByPrimaryKeySelective(BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(),SpuDetailEntity.class));

        this.delSkusAndStocks(spuDTO.getId());

        this.addSkusAndStocks(spuDTO.getSkus(),spuDTO.getId(),date);
    }

    @Override
    public Result<List<SkuDTO>> getSkuBySpuId(Integer spuId) {

        List<SkuDTO> list = skuMapper.selectSkuAndStockBySpuId(spuId);

        return this.setResultSuccess(list);
    }

    @Override
    public Result<SpuDetailEntity> getDetailBySpuId(Integer spuId) {

        SpuDetailEntity spuDetailEntity = supDetailMapper.selectByPrimaryKey(spuId);

        return this.setResultSuccess(spuDetailEntity);
    }

    @Override
    public Result<JSONObject> delGoods(Integer spuId) {

        this.delInfoTransaction(spuId);

        mrRabbitMQ.send(spuId + "", MqMessageConstant.SPU_ROUT_KEY_DELETE);

        return this.setResultSuccess();
    }

    @Transactional
    public void delInfoTransaction(Integer spuId){
        //删除spu
        spuMapper.deleteByPrimaryKey(spuId);
        //删除detail
        supDetailMapper.deleteByPrimaryKey(spuId);

        this.delSkusAndStocks(spuId);
    }

    @Transactional
    @Override
    public Result<JSONObject> saleableEdit(SpuDTO spuDTO) {
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setId(spuDTO.getId());
        if (spuDTO.getSaleable() == 1){
            spuEntity.setSaleable(0);
        }else{
            spuEntity.setSaleable(1);
        }
        spuMapper.updateByPrimaryKeySelective(spuEntity);
        return this.setResultSuccess();
    }

    public void delSkusAndStocks (Integer spuId){

        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuId);
        //通过spuId查询出来将要被删除的Sku
       //List<SkuEntity> skuEntities = skuMapper.selectByExample(example);
        List<Long> skuIdList = skuMapper.selectByExample(example)
                .stream()
                .map(sku -> sku.getId())
                .collect(Collectors.toList());
        //通过skuId集合删除sku
        skuMapper.deleteByIdList(skuIdList);
        //通过skuId集合删除stock
        stockMapper.deleteByIdList(skuIdList);
    }

}