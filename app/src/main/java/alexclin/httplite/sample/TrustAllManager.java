package alexclin.httplite.sample;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
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
//            // Load CAs from an InputStream
//            // (could be from a resource or ByteArrayInputStream or ...)
//            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
//            InputStream caInput = new BufferedInputStream(new FileInputStream("load-der.crt"));
//            Certificate ca;
//            try {
//                ca = cf.generateCertificate(caInput);
//                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
//            } finally {
//                caInput.close();
//            }
//
//            // Create a KeyStore containing our trusted CAs
//            String keyStoreType = KeyStore.getDefaultType();
//            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//            keyStore.load(null, null);
//            keyStore.setCertificateEntry("ca", ca);
//
//            // Create a TrustManager that trusts the CAs in our KeyStore
//            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//            tmf.init(keyStore);
//
//            // Create an SSLContext that uses our TrustManager
//            SSLContext context = SSLContext.getInstance("TLS");
//            context.init(null, tmf.getTrustManagers(), null);
//            return context.getSocketFactory();
        }catch (Exception e){
            return null;
        }
    }
}
