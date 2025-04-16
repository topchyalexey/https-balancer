package service;

import config.BalanceConfig;
import jakarta.annotation.PostConstruct;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HttpsBalancer {
    public enum Strategy {
        ROUND_ROBIN,
        STICKY_SESSION
    }

    private final BalanceConfig.BalanceProperties properties;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final Map<String, String> sessionTargetMap = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private CloseableHttpClient httpClient;

    @Autowired
    public HttpsBalancer(BalanceConfig.BalanceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();

        if (properties.getSsl() != null && properties.getSsl().isTrustAll()) {
            sslContextBuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
        } else if (properties.getSsl() != null && properties.getSsl().getTrustStore() != null) {
            sslContextBuilder.loadTrustMaterial(
                    getClass().getResource(properties.getSsl().getTrustStore()),
                    properties.getSsl().getTrustStorePassword().toCharArray()
            );
        }

        SSLContext sslContext = sslContextBuilder.build();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );

        this.httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();
    }

    public String getTarget() {
        return getTarget(null);
    }

    public String getTarget(String sessionId) {
        Strategy strategy = Strategy.valueOf(properties.getStrategy().toUpperCase());

        switch (strategy) {
            case STICKY_SESSION:
                return getStickyTarget(sessionId);
            case ROUND_ROBIN:
            default:
                return getRoundRobinTarget();
        }
    }

    private String getRoundRobinTarget() {
        int index = currentIndex.getAndUpdate(
                i -> (i + 1) % properties.getTargets().size()
        );
        return properties.getTargets().get(index);
    }

    private String getStickyTarget(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return getRoundRobinTarget();
        }

        return sessionTargetMap.computeIfAbsent(sessionId, k -> {
            int randomIndex = random.nextInt(properties.getTargets().size());
            return properties.getTargets().get(randomIndex);
        });
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void resetSession(String sessionId) {
        sessionTargetMap.remove(sessionId);
    }

    public void setStrategy(Strategy strategy) {
        properties.setStrategy(strategy.name());
        if (strategy == Strategy.ROUND_ROBIN) {
            sessionTargetMap.clear();
        }
    }
}