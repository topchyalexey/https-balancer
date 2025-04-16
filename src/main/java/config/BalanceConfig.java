import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BalanceConfig {

    @Bean
    @ConfigurationProperties(prefix = "balance")
    public BalanceProperties balanceProperties() {
        return new BalanceProperties();
    }

    public static class BalanceProperties {
        private List<String> targets;
        private String strategy;
        private SslConfig ssl;

        // Getters and Setters
        public List<String> getTargets() {
            return targets;
        }

        public void setTargets(List<String> targets) {
            this.targets = targets;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public SslConfig getSsl() {
            return ssl;
        }

        public void setSsl(SslConfig ssl) {
            this.ssl = ssl;
        }
    }

    public static class SslConfig {
        private boolean trustAll;
        private String trustStore;
        private String trustStorePassword;

        // Getters and Setters
        public boolean isTrustAll() {
            return trustAll;
        }

        public void setTrustAll(boolean trustAll) {
            this.trustAll = trustAll;
        }

        public String getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(String trustStore) {
            this.trustStore = trustStore;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }
    }
}