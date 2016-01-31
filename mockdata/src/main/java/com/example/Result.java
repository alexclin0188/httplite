package com.example;

public class Result<T> {
    public String requestPath;
    public String requestMethod;
    public T data;
    public int code;
    public String errorMsg;

    @Override
    public String toString() {
        return "Result{" +
                "requestPath='" + requestPath + '\'' +
                ", requestMethod='" + requestMethod + '\'' +
                ", data=" + data +
                ", code=" + code +
                ", errorMsg='" + errorMsg + '\'' +
                '}';
    }
}
