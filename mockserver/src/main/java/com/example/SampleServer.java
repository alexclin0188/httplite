package com.example;

import com.example.utils.HttpUtil;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SampleServer extends Dispatcher {
    private final SSLContext sslContext;
    private final String root;
    private final int port;

    private HashMap<String,MethodHandle> mHandleMap;

    public SampleServer(SSLContext sslContext, String root, int port) {
        this.sslContext = sslContext;
        this.root = root;
        this.port = port;
        mHandleMap = new HashMap<>();
        mHandleMap.put("GET", new GetHandle());
        mHandleMap.put("POST", new PostHandle());
        mHandleMap.put("MISC", new MiscHandle());
    }

    public void run() throws IOException {
        MockWebServer server = new MockWebServer();
        server.useHttps(sslContext.getSocketFactory(), false);
        server.setDispatcher(this);
        InetAddress address = HttpUtil.getHostAddress();
        server.start(address,port);
        System.out.println(String.format("Started server for: http://%s:%d/", address.getHostAddress(), port));
    }

    @Override public MockResponse dispatch(RecordedRequest request) {
        System.out.println("Headers:"+request.getHeaders().toMultimap());
        String handleParam = request.getHeaders().get("handle");
        String method = request.getMethod().toUpperCase();
        MethodHandle handle = null;
        if(handleParam!=null){
            handle = mHandleMap.get(handleParam.toUpperCase());
        }
        System.out.printf("Path:%s,Method:%s\n",request.getPath(),method);
        if(handle==null){
            handle = mHandleMap.get(method);
        }
        if(handle!=null){
            return handle.handle(request,root);
        }else{
            return new MockResponse()
                    .setStatus("HTTP/1.1 500")
                    .addHeader("content-type: text/plain; charset=utf-8")
                    .setBody("NO method handle for: " + method);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: SampleServer <keystore> <password> <root file> <port>");
            return;
        }

        String keystoreFile = args[0];
        String password = args[1];
        String root = args[2];
        int port = Integer.parseInt(args[3]);

        SSLContext sslContext = sslContext(keystoreFile, password);
        SampleServer server = new SampleServer(sslContext, root, port);
        server.run();
    }

    private static SSLContext sslContext(String keystoreFile, String password)
            throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = new FileInputStream(keystoreFile);
        try {
            keystore.load(in, password.toCharArray());
        } finally {
            Util.closeQuietly(in);
        }
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, password.toCharArray());

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());

        return sslContext;
    }
}
