package com.mr.test;

import com.baidu.shop.dto.UserInfo;
import com.baidu.shop.utils.JwtUtils;
import com.baidu.shop.utils.RsaUtils;
import org.junit.*;

import java.security.PrivateKey;
import java.security.PublicKey;
/**
 * @ClassName JwtTokenTest
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/10/15
 * @Version V1.0
 **/
public class JwtTokenTest {
    //公钥位置
    private static final String pubKeyPath = "D:\\secret\\rea.pub";
    //私钥位置
    private static final String priKeyPath = "D:\\secret\\rea.pri";
    //公钥对象
    private PublicKey publicKey;
    //私钥对象
    private PrivateKey privateKey;


    /**
     * 生成公钥私钥 根据密文
     * @throws Exception
     */
//    @Test
//    public void genRsaKey() throws Exception {
//        RsaUtils.generateKey(pubKeyPath, priKeyPath, "mingrui");
//    }


    /**
     * 从文件中读取公钥私钥
     * @throws Exception
     */
    @Before
    public void getKeyByRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    /**
     * 根据用户信息结合私钥生成token
     * @throws Exception
     */
    @Test
    public void genToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateToken(new UserInfo(1, "lipangpang"), privateKey, 2);
        System.out.println("user-token = " + token);
    }


    /**
     * 结合公钥解析token
     * @throws Exception
     */
    @Test
    public void parseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJsaXBhbmdwYW5nIiwiZXhwIjoxNjAyNzQ0MjMxfQ.KBgP0i7poBMI8rMRvnhjKNwkqdXf54UMGRTW7v_MEFKTcbP0RXwl8xlZWk66FlbhplpYbfoQfZKBxnlaWHTYR6co7_3I0F5u5g6Yb8grwXBMrQS8XoBXYDJXcnMxWqP9VcX5OPt55QpZBLiX06q4uw12r_rdfUJJNNleT79knFA";
        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }
}
