package com.example;

public class BaseResult<T> {
    public String requestPath;
    public String requestMethod;
    public T data;
    public int code;
    public String errorMsg;

    @Override
    public String toString() {
        return "BaseResult{" +
                "requestPath='" + requestPath + '\'' +
                ", requestMethod='" + requestMethod + '\'' +
                ", data=" + data +
                ", code=" + code +
                ", errorMsg='" + errorMsg + '\'' +
                '}';
    }
}
