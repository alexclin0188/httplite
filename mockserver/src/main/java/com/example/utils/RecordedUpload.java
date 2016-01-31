package com.example.utils;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * RecordedUpload
 *
 * @author alexclin
 * @date 16/1/17 10:02
 */
public class RecordedUpload extends FileUpload implements RequestContext {
    private RecordedRequest request;
    private String contentType;
    private String charset;

    public RecordedUpload(RecordedRequest request) {
        this.request = request;
        this.contentType = request.getHeader("content-type");
        this.charset = HttpUtil.getChartset(request).name();
    }

    @Override
    public String getCharacterEncoding() {
        return charset;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        return (int) request.getBodySize();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getBody().inputStream();
    }

    public static boolean isMultipartContent(RecordedRequest request){
        String contentType = request.getHeader("content-type");
        return contentType!=null&&"POST".equalsIgnoreCase(request.getMethod()) && contentType.startsWith("multipart/");
    }

    public FileItemIterator getItemIterator() throws FileUploadException,IOException{
        return super.getItemIterator(this);
    }

    public Map<String, List<FileItem>> parseParameterMap() throws FileUploadException {
        return parseParameterMap(this);
    }

    public List<FileItem> parseRequest() throws FileUploadException {
        return parseRequest(this);
    }
}
