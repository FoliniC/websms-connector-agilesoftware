package de.ub0r.android.websms.connector.agilesoftware;

import android.os.Process;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.ub0r.android.websms.connector.common.Log;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Created by FoliniC on 12/5/2016.
 */
public class CustomOkHttpClient {
    private static final String TAG = "CustomOkHttpClient";
    public static OkHttpClient getOkHttpClient() {

            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                X509TrustManager trustManager = (X509TrustManager) trustAllCerts[0];
                Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", " useFiddler " + BuildConfig.USE_FIDDLER);
                OkHttpClient okHttpClient;
                if (BuildConfig.USE_FIDDLER) {
                    okHttpClient = new OkHttpClient.Builder()
                            .sslSocketFactory(sslSocketFactory, trustManager)
                            .protocols(Arrays.asList(Protocol.HTTP_1_1))
                            .hostnameVerifier(new HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, SSLSession session) {
                                    return true;
                                }
                            }).build();
                } else {
                    okHttpClient = new OkHttpClient.Builder().build();
                }
                return okHttpClient;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

}