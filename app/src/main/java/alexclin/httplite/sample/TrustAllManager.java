package alexclin.httplite.sample;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * TrustAllManager
 *
 * @author alexclin  16/5/7 09:34
 */
public class TrustAllManager implements X509TrustManager,HostnameVerifier{
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        //TODO
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }

    public SSLSocketFactory getSocketFactory(){
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,new X509TrustManager[]{this},null);
            return sslContext.getSocketFactory();
        }catch (Exception e){
            return null;
        }
    }
}
