package com.zhuoyue.platform.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * 全局 HTTP 客户端配置。
 * 使用信任所有证书的 OkHttp，解决 JDK cacerts 为空导致的 TLS 握手失败问题。
 * 注意：仅用于开发/内网环境，生产环境应配置正确的证书信任链。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sc.getSocketFactory(), (X509TrustManager) trustAll[0])
                    .hostnameVerifier((h, s) -> true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)  // 流式输出需要长超时
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("OkHttpClient 初始化失败", e);
        }
    }

    @Bean
    public OkHttp3ClientHttpRequestFactory okHttp3RequestFactory(OkHttpClient okHttpClient) {
        return new OkHttp3ClientHttpRequestFactory(okHttpClient);
    }
}
