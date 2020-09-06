package com.baidu.shop.exception;

/**
 * @ClassName MingruiException
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/9/3
 * @Version V1.0
 **/
public class MingruiException extends RuntimeException{
    private String msg;

    public MingruiException(String msg) {
        super(msg);
        this.msg = msg;
    }
}
