package com.example;

import java.util.List;
import java.util.Map;

/**
 * RequestInfo
 *
 * @author alexclin
 * @date 16/1/31 11:39
 */
public class RequestInfo {
    public String method;
    public String path;
    public Map<String,List<String>> headers;
    public Map<String,String[]> params;
    public String bodyInfo;

    @Override
    public String toString() {
        return "RequestInfo{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", params=" + params +
                ", bodyInfo='" + bodyInfo + '\'' +
                '}';
    }
}
