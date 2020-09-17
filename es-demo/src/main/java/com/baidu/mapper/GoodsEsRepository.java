package com.baidu.mapper;

import com.baidu.entity.GoodsEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface GoodsEsRepository extends ElasticsearchRepository<GoodsEntity,Long> {

    List<GoodsEntity> findAllByAndTitle(String title);

    List<GoodsEntity> findByAndPriceBetween(Double start,Double end);

}
