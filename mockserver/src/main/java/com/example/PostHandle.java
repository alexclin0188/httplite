package com.example;

import com.alibaba.fastjson.JSON;
import com.example.util.EncryptUtil;
import com.example.utils.HttpUtil;
import com.example.utils.RecordedUpload;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import okio.Buffer;
import okio.Okio;
import okio.Sink;
import okio.Source;

/**
 * com.example
 *
 * @author alexclin
 * @date 16/1/4 22:00
 */
public class PostHandle implements MethodHandle{
    private File tmpDir;

    public MockResponse handle(RecordedRequest request,String root){
        if(tmpDir==null){
            tmpDir = new File(new File(root),"tmp");
            if(!tmpDir.exists()){
                tmpDir.mkdirs();
            }
        }
        String path = request.getPath();
        Result<Object> result = new Result<>();
        result.requestPath = path;
        result.requestMethod = "POST";
        if(HttpUtil.getMimeType(request).equals("application/json")){
            Charset charset = HttpUtil.getChartset(request);
            String json = request.getBody().readString(charset);
            System.out.println(String.format("JsonBody charSet:%s,body:%s",charset.displayName(),json));
        }else if(HttpUtil.getMimeType(request).equals("application/x-www-form-urlencoded")){
            System.out.println("FormBody");
            String s;
            try {
                while ((s = request.getBody().readUtf8Line())!=null){
                    System.out.println(URLDecoder.decode(s, Util.UTF_8.name()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(RecordedUpload.isMultipartContent(request)){
            handleMultipart(request,result);
        }else if(HttpUtil.getMimeType(request).equals("application/octet-stream")){
            int index = path.indexOf("?");
            if (index > -1) {
                String queryString = path.substring(index + 1);
                Map<String, String[]> paramMap = HttpUtil.getParamsMap(queryString, Util.UTF_8.name());
                System.out.println("Params:" + JSON.toJSONString(paramMap));
                try {
                    File file = saveFile(request.getBody().inputStream(),"octet-stream");
                    String hash = paramMap.get("hash")[0];
                    String rHash = EncryptUtil.hash(file);
                    result.data = String.format("file-%s-%s-%s","octet-stream",hash,rHash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return new MockResponse().setStatus("HTTP/1.1 200").addHeader("content-type: application/json; charset=utf-8")
                .setBody(JSON.toJSONString(result));
    }

    private void handleMultipart(RecordedRequest request,Result<Object> outResult) {
        RecordedUpload upload = new RecordedUpload(request);
        try {
            Map<String,String> params = new HashMap<>();
            Map<String,File> files = new HashMap<>();
            FileItemIterator iter = upload.getItemIterator();
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (item.isFormField()) {
                    String value = Streams.asString(stream);
                    System.out.println("Form field " + name + " with value "
                            + value + " detected.");
                    params.put(name,value);
                } else {
                    System.out.println("File field " + name + " with file name "
                            + item.getName() + " detected.");
                    File file = saveFile(item.openStream(), name);
                    files.put(name, file);
                }
            }
            StringBuilder builder = new StringBuilder();
            for(String key:files.keySet()){
                File file = files.get(key);
                String hash = params.get(key);
                String rHash = EncryptUtil.hash(file);
                builder.append(String.format("file-%s-%s-%s",key,hash,rHash));
            }
            outResult.data = builder.toString();
        } catch (FileUploadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File saveFile(InputStream stream, String name) throws IOException {
        String fileName = System.currentTimeMillis()+name;
        File file = new File(tmpDir,fileName);
        Sink sink = Okio.sink(file);
        Source source = Okio.source(stream);
        long count;
        Buffer buffer = new Buffer();
        while ((count=source.read(buffer,1024))!=-1){
            sink.write(buffer,count);
        }
        sink.close();
        source.close();
        return file;
    }
}
