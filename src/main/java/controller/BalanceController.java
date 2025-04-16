import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/balance")
public class BalanceController {
    private final HttpsBalancer balancer;

    @Autowired
    public BalanceController(HttpsBalancer balancer) {
        this.balancer = balancer;
    }

    @GetMapping("/**")
    public String balanceGet(HttpServletRequest request) throws IOException {
        String target = balancer.getTarget(request.getSession().getId());
        String path = request.getRequestURI().replace("/balance", "");
        String url = target + path;

        CloseableHttpClient client = balancer.getHttpClient();
        HttpUriRequest httpGet = new HttpGet(url);

        try (CloseableHttpResponse response = client.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }

    @PostMapping("/strategy/{strategy}")
    public void setStrategy(@PathVariable String strategy) {
        balancer.setStrategy(HttpsBalancer.Strategy.valueOf(strategy.toUpperCase()));
    }

    @PostMapping("/session/reset")
    public void resetSession(HttpServletRequest request) {
        balancer.resetSession(request.getSession().getId());
    }
}