package com.example.utils;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by apple on 16/1/6.
 */
public class HttpUtil {
    public static boolean isEmpty(String str){
        return str==null||str.matches(" *");
    }

    public static String getLocalIp(){
        InetAddress address = getHostAddress();
        return address == null ? null:address.getHostAddress();
    }

    public static InetAddress getHostAddress(){
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress returnIp = null;
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()){
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()){
                    ip = (InetAddress) addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address && !ip.isLoopbackAddress() &&
                            !ip.isAnyLocalAddress()  && !ip.isLinkLocalAddress()){
                        returnIp = ip;
                    }
                }
            }
            return returnIp;
        }catch (SocketException e){
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String[]> getParamsMap(String queryString, String enc) {
        Map<String, String[]> paramsMap = new HashMap<String, String[]>();
        if (queryString != null && queryString.length() > 0) {
            int ampersandIndex, lastAmpersandIndex = 0;
            String subStr, param, value;
            String[] paramPair, values, newValues;
            do {
                ampersandIndex = queryString.indexOf('&', lastAmpersandIndex) + 1;
                if (ampersandIndex > 0) {
                    subStr = queryString.substring(lastAmpersandIndex, ampersandIndex - 1);
                    lastAmpersandIndex = ampersandIndex;
                } else {
                    subStr = queryString.substring(lastAmpersandIndex);
                }
                paramPair = subStr.split("=");
                param = paramPair[0];
                value = paramPair.length == 1 ? "" : paramPair[1];
                try {
                    value = URLDecoder.decode(value, enc);
                } catch (UnsupportedEncodingException ignored) {
                }
                if (paramsMap.containsKey(param)) {
                    values = paramsMap.get(param);
                    int len = values.length;
                    newValues = new String[len + 1];
                    System.arraycopy(values, 0, newValues, 0, len);
                    newValues[len] = value;
                } else {
                    newValues = new String[] { value };
                }
                paramsMap.put(param, newValues);
            } while (ampersandIndex > 0);
        }
        return paramsMap;
    }

    public static String paramsMapToString(Map<String, String[]> map){
        StringBuilder sb = new StringBuilder("{");
        for(String key:map.keySet()){
            sb.append(key).append(":[");
            for(String value:map.get(key)){
                sb.append(value).append(",");
            }
            sb.deleteCharAt(sb.length()-1).append("],");
        }
        return sb.append("}").toString();
    }

    public static Charset getChartset(RecordedRequest request){
        Headers headers = request.getHeaders();
        String value = headers.get("content-type");
        String[] array = value.split(";");
        Charset charset = null;
        try {
            if(array.length>1&&array[1].startsWith("charset=")){
                String charSetStr = array[1].split("=")[1];
                charset = Charset.forName(charSetStr);
            }
        } catch (Exception e) {
            System.out.println("ContentType:"+value);
            e.printStackTrace();
        }
        if(charset==null){
            charset = Charset.forName("utf-8");
        }
        return charset;
    }

    public static String getMimeType(RecordedRequest request){
        Headers headers = request.getHeaders();
        String value = headers.get("content-type");
        String[] array = value.split(";");
        return array[0];
    }
}
