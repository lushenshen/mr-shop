package com.baidu.shop.service;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.document.GoodsDoc;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpecParamDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecParamEntity;
import com.baidu.shop.entity.SpuDetailEntity;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.response.GoodsResponse;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.ESHighLightUtil;
import com.baidu.shop.utils.JSONUtil;
import com.baidu.shop.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;


import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName ShopElasticsearchServiceImpl
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/16
 * @Version V1.0
 **/
@RestController
@Slf4j
public class ShopElasticsearchServiceImpl extends BaseApiService implements ShopElasticsearchService {

    @Autowired
    private GoodsFeign goodsFeign;

    @Autowired
    private SpecificationFeign specificationFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Autowired
    private BrandFeign brandFeign;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    public Result<JSONObject> initEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if(!indexOperations.exists()){
            indexOperations.createMapping();
            log.info("映射创建成功");
        }

        //批量新增数据
        List<GoodsDoc> goodsDocs = this.esGoodsInfo(new SpuDTO());
        elasticsearchRestTemplate.save(goodsDocs);

        return this.setResultSuccess();

    }

    @Override
    public Result<JSONObject> clearEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDoc.class);
        if(indexOperations.exists()){
            indexOperations.delete();
            log.info("索引删除成功");
        }

        return this.setResultSuccess();
    }


    private List<GoodsDoc> esGoodsInfo(SpuDTO spuDTO)  {
        Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(spuDTO);

        List<SpuDTO> data = spuInfo.getData();

        List<GoodsDoc> goodsDocs = new ArrayList<>();

        if (spuInfo.getCode() == HTTPStatus.OK){

            data.stream().forEach(spu ->{

                GoodsDoc goodsDoc = new GoodsDoc();
                goodsDoc.setId(spu.getId().longValue());
                goodsDoc.setCid1(spu.getCid1().longValue());
                goodsDoc.setCid2(spu.getCid2().longValue());
                goodsDoc.setCid3(spu.getCid3().longValue());
                goodsDoc.setBrandId(spu.getBrandId().longValue());
                goodsDoc.setBrandName(spu.getBrandName());
                goodsDoc.setCategoryName(spu.getCategoryName());
                goodsDoc.setSubTitle(spu.getSubTitle());
                goodsDoc.setTitle(spu.getTitle());
                Map<List<Long>, List<Map<String, Object>>> skuList = this.getSkusAndPriceList(spu.getId());
                skuList.forEach((key,value)->{
                    goodsDoc.setPrice(key);
                    goodsDoc.setSkus(JSONUtil.toJsonString(value));
                });




                Map<String, Object> paramSpec = this.getSpecMap(spu);

                goodsDoc.setSpecs(paramSpec);
                goodsDocs.add(goodsDoc);

            });
        }

        return goodsDocs;
    }

    /**
     * 搜索
     * @param search
     * @param page
     * @return
     */
    @Override
    public GoodsResponse search(String search, Integer page, String filter)  {

        //判断搜索的内容不为空
        if (StringUtil.isEmpty(search)) throw new RuntimeException("搜索的内容不能为空");

        SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate
                .search(this.getSearchQueryBuilder(search,page,filter)
                        .build(), GoodsDoc.class);


        List<SearchHit<GoodsDoc>> highLightHits = ESHighLightUtil.getHighLightHit(searchHits.getSearchHits());
        //返回的商品集合
        List<GoodsDoc> goodsList = highLightHits.stream()
                .map(searchHit -> searchHit.getContent())
                .collect(Collectors.toList());
        //总条数&总页数
        long total = searchHits.getTotalHits();
        long totalPage = Double.valueOf(Math.ceil(Long.valueOf(total).doubleValue() / 10)).longValue();

        Map<String, Long> messageMap = new HashMap<>();
        messageMap.put("total",total);
        messageMap.put("totalPage",totalPage);

        Aggregations aggregations = searchHits.getAggregations();
        //List<CategoryEntity> categoryList = this.getCategoryList(aggregations);//获取分类集合

        Map<Integer,List<CategoryEntity>> map = this.getCategoryList(aggregations);

        List<CategoryEntity> categoryList = null;
        Integer hotCid = 0;

        //遍历map集合的方式
        for (Map.Entry<Integer,List<CategoryEntity>> mapEntry : map.entrySet()){
            hotCid = mapEntry.getKey();
            categoryList = mapEntry.getValue();
        }

        //通过cid去查规格参数
        //通过cid去查询规格参数
        Map<String, List<String>> specParamValueMap = this.getspecParam(hotCid, search);


        List<BrandEntity> brandList = this.getBrandList(aggregations);//获取品牌集合

        return new GoodsResponse(total, totalPage, brandList, categoryList, goodsList,specParamValueMap);
    }

    @Override
    public Result<JSONObject> saveData(Integer spuId) {
        //通过spuId查询数据
        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);

        List<GoodsDoc> goodsDocs = this.esGoodsInfo(spuDTO);//在这行代码有可能是查询不到数据的

        elasticsearchRestTemplate.save(goodsDocs.get(0));

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> delData(Integer spuId) {
        GoodsDoc goodsDoc = new GoodsDoc();
        goodsDoc.setId(spuId.longValue());
        elasticsearchRestTemplate.delete(goodsDoc);
        return this.setResultSuccess();
    }

    private Map<String, List<String>> getspecParam(Integer hotCid, String search) {

        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(hotCid);
        specParamDTO.setSearching(true);//之搜索有查询属性的规格

        Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
        if (specParamResult.getCode() == 200){
            List<SpecParamEntity> specParamList = specParamResult.getData();
            //聚合查询
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));

            //分页必须的查询一条数据
            queryBuilder.withPageable(PageRequest.of(0,1));

            specParamList.stream().forEach(specParam -> {
                queryBuilder.addAggregation
                        (AggregationBuilders.terms(specParam.getName()).field("specs." + specParam.getName() + ".keyword"));
            });
            SearchHits<GoodsDoc> searchHits = elasticsearchRestTemplate.search(queryBuilder.build(),GoodsDoc.class);

            Map<String, List<String>> map = new HashMap<>();
            Aggregations aggregations = searchHits.getAggregations();

            specParamList.stream().forEach(specParam -> {
                Terms terms = aggregations.get(specParam.getName());
                List<? extends Terms.Bucket> buckets = terms.getBuckets();
                List<String> valueList = buckets.stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());

                map.put(specParam.getName(),valueList);
            });

            return map;
        }

        return null;
    }

    /**
     * 获取品牌集合
     * @param aggregations
     * @return
     */
    private List<BrandEntity> getBrandList(Aggregations aggregations){

        Terms brand_agg = aggregations.get("brand_agg");
        List<String> brandIdList = brand_agg.getBuckets()
                .stream().map(brandBucket -> brandBucket.getKeyAsString()).collect(Collectors.toList());
        //通过品牌id集合去查询数据
        Result<List<BrandEntity>> brandResult = brandFeign
                .getBrandByIdList(String.join(",", brandIdList));
        return brandResult.getData();
    }

    /**
     * 构建查询条件
     * @param search
     * @param page
     * @param filter
     * @return
     */
    private NativeSearchQueryBuilder getSearchQueryBuilder(String search, Integer page, String filter){

        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        if(StringUtil.isNotEmpty(filter) && filter.length() > 2){
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            Map<String, String> filterMap = JSONUtil.toMapValueString(filter);

            filterMap.forEach((key,value) -> {
                MatchQueryBuilder matchQueryBuilder = null;

                //分类 品牌和 规格参数的查询方式不一样
                if(key.equals("cid3") || key.equals("brandId")){
                    matchQueryBuilder = QueryBuilders.matchQuery(key, value);
                }else{
                    matchQueryBuilder = QueryBuilders.matchQuery("specs." + key + ".keyword",value);
                }
                boolQueryBuilder.must(matchQueryBuilder);
            });
            searchQueryBuilder.withFilter(boolQueryBuilder);
        }
        //match通过值只能查询一个字段 和 multiMatch 通过值查询多个字段???
        searchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));
        //品牌
        //分类 --> cid3g
        searchQueryBuilder.addAggregation(AggregationBuilders.terms("cid_agg").field("cid3"));
        searchQueryBuilder.addAggregation(AggregationBuilders.terms("brand_agg").field("brandId"));
        //高亮
        searchQueryBuilder.withHighlightBuilder(ESHighLightUtil.getHighlightBuilder("title"));
        //分页
        searchQueryBuilder.withPageable(PageRequest.of(page-1,10));

        return searchQueryBuilder;
    }

    /**
     * 获取分类集合
     * @param aggregations
     * @return
     */
    private Map<Integer, List<CategoryEntity>> getCategoryList(Aggregations aggregations){

        Terms cid_agg = aggregations.get("cid_agg");
        List<? extends Terms.Bucket> cidBuckets = cid_agg.getBuckets();

        //热度最高cid
        List<Integer> hotCidArr = Arrays.asList(0);

        List<Long> maxCount = Arrays.asList(0L);

        Map<Integer,List<CategoryEntity>> map = new HashMap<>();
        //返回一个id集合 --> 通过id集合去查询数据
        //StringBuffer
        List<String> cidList = cidBuckets.stream().map(cidbucket -> {
            Number keyAsNumber = cidbucket.getKeyAsNumber();
            //cidbucket.getKeyAsString()
            if (cidbucket.getDocCount() > maxCount.get(0)){
                maxCount.set(0,cidbucket.getDocCount());
                hotCidArr.set(0,keyAsNumber.intValue());
            }
            return keyAsNumber.intValue() + "";
        }).collect(Collectors.toList());


        String cidsStr = String.join(",", cidList);
        Result<List<CategoryEntity>> caterogyResult = categoryFeign.getCategoryByIdList(cidsStr);

        map.put(hotCidArr.get(0),caterogyResult.getData());
        return map;
    }

    //通过esGoodsInfo 中的 getSkusAndPriceList集合得到 getSkusAndPriceList方法
    private Map<List<Long>, List<Map<String, Object>>> getSkusAndPriceList(Integer spuId) {

        Map<List<Long>,List<Map<String,Object>>> hashMap = new HashMap<>();

        Result<List<SkuDTO>> skuResult = goodsFeign.getSkuBySpuId(spuId);
        List<Long> priceList = new ArrayList<>();
        List<Map<String, Object>> skuMap = null;

        if(skuResult.getCode() == HTTPStatus.OK){
            List<SkuDTO> skuList = skuResult.getData();
            skuMap = skuList.stream().map(sku -> {
                Map<String,Object> map = new HashMap<>();
                map.put("id",sku.getId());
                map.put("title",sku.getTitle());
                map.put("images",sku.getImages());
                map.put("price",sku.getPrice());

                priceList.add(sku.getPrice().longValue());
                return map;
            }).collect(Collectors.toList());
        }

        hashMap.put(priceList,skuMap);
        return hashMap;
    }

    //通过esGoodsInfo 中的 getSpecMap 集合得到 getSpecMap 方法
    private Map<String, Object> getSpecMap(SpuDTO spuDTO){

        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(spuDTO.getCid3());
        Result<List<SpecParamEntity>> specParamInfo = specificationFeign.getSpecParamInfo(specParamDTO);

        Map<String, Object> specMap = new HashMap<>();

        if (specParamInfo.getCode() == HTTPStatus.OK) {
            //只有规格参数的id和规格参数的名字
            List<SpecParamEntity> paramList =  specParamInfo.getData();

            //通过spuid去查询spuDetail,detail里面有通用和特殊规格参数的值
            Result<SpuDetailEntity> spuDetailResult = goodsFeign.getDetailBySpuId(spuDTO.getId());

            //因为spu和spuDetail one --> one
            if(spuDetailResult.getCode() == HTTPStatus.OK){
                SpuDetailEntity spuDetaiInfo = spuDetailResult.getData();

                //通用规格参数的值
                String genericSpecStr = spuDetaiInfo.getGenericSpec();
                Map<String, String> genericSpecMap = JSONUtil.toMapValueString(genericSpecStr);

                //特有规格参数的值
                String specialSpecStr = spuDetaiInfo.getSpecialSpec();
                Map<String, List<String>> specialSpecMap = JSONUtil.toMapValueStrList(specialSpecStr);

                paramList.stream().forEach(param -> {

                    if (param.getGeneric()) {

                        if(param.getNumeric() && param.getSearching()){
                            specMap.put(param.getName(), this.chooseSegment(genericSpecMap.get(param.getId() + ""),param.getSegments(),param.getUnit()));
                        }else{
                            specMap.put(param.getName(), genericSpecMap.get(param.getId() + ""));
                        }
                    } else {
                        specMap.put(param.getName(), specialSpecMap.get(param.getId() + ""));
                    }
                });
            }
        }
        return specMap;
    }

    private Object chooseSegment(String value, String segments, String unit) {

        double val = NumberUtils.toDouble(value);
        String result = "其他";
        //保存数值段
        for (String segment : segments.split(",")){
            String[] segs = segment.split("-");
            //获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + unit + "以上";
                }else if(begin == 0){
                    result = segs[1] + unit + "以下";
                }else{
                    result = segment + unit;
                }
                break;
            }
        }
        return  result ;
    }
}
































