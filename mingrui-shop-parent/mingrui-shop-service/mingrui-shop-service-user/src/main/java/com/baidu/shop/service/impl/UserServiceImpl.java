package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.constant.UserConstant;
import com.baidu.shop.dto.UserDTO;
import com.baidu.shop.entity.UserEntity;
import com.baidu.shop.mapper.UserMapper;
import com.baidu.shop.redis.repository.RedisRepository;
import com.baidu.shop.service.UserService;
import com.baidu.shop.utils.BCryptUtil;
import com.baidu.shop.utils.BaiduBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @ClassName UserServiceImpl
 * @Description: TODO
 * @Author shenyaqi
 * @Date 2020/9/20
 * @Version V1.0
 **/
@RestController
@Slf4j
public class UserServiceImpl extends BaseApiService implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisRepository redisRepository;

    @Override
    public Result<JSONObject> register(UserDTO userDTO) {

        UserEntity userEntity = BaiduBeanUtil.copyProperties(userDTO, UserEntity.class);
        userEntity.setPassword(BCryptUtil.hashpw(userEntity.getPassword(),BCryptUtil.gensalt()));
        userEntity.setCreated(new Date());

        userMapper.insertSelective(userEntity);
        return this.setResultSuccess();
    }

    @Override
    public Result<List<UserEntity>> checkUserNameOrPhone(String value, Integer type) {
        Example example = new Example(UserEntity.class);
        Example.Criteria criteria = example.createCriteria();

        if (type == UserConstant.USER_TYPE_USERNAME){

            criteria.andEqualTo("username",value);
        }else if(type == UserConstant.USER_TYPE_PHONE){
            criteria.andEqualTo("phone",value);
        }
        List<UserEntity> userEntities = userMapper.selectByExample(example);


        return this.setResultSuccess(userEntities);
    }


        @Override
    public Result<JSONObject> sendValidCode(UserDTO userDTO) {

        //随机生成六位数验证码
        String code = (int)((Math.random() * 9 + 1) * 100000) + "";

        //第三方SDK 支付宝 微信 短信
        log.debug("发送短信验证码-->手机号 : {} , 验证码 : {}",userDTO.getPhone(),code);

        redisRepository.set("valid-code-" + userDTO.getPhone(), code);

        redisRepository.expire("valid-code-" + userDTO.getPhone(),60);

        //发送短信验证码
       // LuosimaoDuanxinUtil.sendSpeak(userDTO.getPhone(),code);

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> checkValidCode(String phone, String validcode) {

        String redisValidCode = redisRepository.get("valid-code-" + phone);
         if (!validcode.equals(redisValidCode)){
             return this.setResultError("验证码错误");
         }
        return this.setResultSuccess();
    }
}














