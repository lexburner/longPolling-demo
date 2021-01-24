package moe.cnkirito.demo;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jingfeng.xjf
 * @date 2021-01-23
 */
@RestController
@Slf4j
@SpringBootApplication
public class ConfigController {

    @Data
    private static class AsyncTask {
        private AsyncContext asyncContext;
        private boolean timeout;

        public AsyncTask(AsyncContext asyncContext, boolean timeout) {
            this.asyncContext = asyncContext;
            this.timeout = timeout;
        }
    }

    // guava 提供的多值 Map，一个 key 可以对应多个 value
    private Multimap<String, AsyncTask> dataIdContext = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("longPolling-timeout-checker-%d")
        .build();
    private ScheduledExecutorService timeoutChecker = new ScheduledThreadPoolExecutor(1, threadFactory);

    @RequestMapping("/listener")
    public void addListener(HttpServletRequest request, HttpServletResponse response) {

        String dataId = request.getParameter("dataId");
        AsyncContext asyncContext = request.startAsync(request, response);

        AsyncTask asyncTask = new AsyncTask(asyncContext, true);

        dataIdContext.put(dataId, asyncTask);

        timeoutChecker.schedule(() -> {
            if (asyncTask.isTimeout()) {
                dataIdContext.remove(dataId, asyncTask);
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                asyncContext.complete();
            }
        }, 29500, TimeUnit.MILLISECONDS);
    }

    @RequestMapping("/publishConfig")
    @SneakyThrows
    public String publishConfig(String dataId, String configInfo) {
        log.info("publish configInfo dataId: [{}], configInfo: {}", dataId, configInfo);
        Collection<AsyncTask> asyncTasks = dataIdContext.removeAll(dataId);
        for (AsyncTask asyncTask : asyncTasks) {
            asyncTask.setTimeout(false);
            HttpServletResponse response = (HttpServletResponse)asyncTask.getAsyncContext().getResponse();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(configInfo);
            asyncTask.getAsyncContext().complete();
        }
        return "success";
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }

}
