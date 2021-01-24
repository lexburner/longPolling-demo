package moe.cnkirito.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;

/**
 * @author jingfeng.xjf
 * @date 2021-01-23
 */
@Slf4j
public class ConfigClient {

    private CloseableHttpClient httpClient;
    private RequestConfig requestConfig;

    public ConfigClient() {
        this.httpClient = HttpClientBuilder.create().build();
        this.requestConfig = RequestConfig.custom().setSocketTimeout(30000).build();
    }

    @SneakyThrows
    public void longPolling(String url, String dataId) {
        url += "?dataId=" + dataId;
        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(request);
        switch (response.getStatusLine().getStatusCode()) {
            case 200: {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                response.close();
                String configInfo = result.toString();
                log.info("dataId: [{}] changed, receive configInfo: {}", dataId, configInfo);
                longPolling(url, dataId);
                break;
            }
            case 304: {
                log.info("longPolling dataId: [{}] once finished, configInfo is unchanged, longPolling again", dataId);
                longPolling(url, dataId);
                break;
            }
            default: {
                throw new RuntimeException("unExcepted HTTP status code");
            }
        }

    }

    public static void main(String[] args) {

        Logger logger = (Logger)LoggerFactory.getLogger("org.apache.http");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);

        ConfigClient configClient = new ConfigClient();
        configClient.longPolling("http://127.0.0.1:8080/listener", "user");
    }

}
