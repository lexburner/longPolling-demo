package moe.cnkirito.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.cnblogs.com/weifeng1463/p/7353710.html
 * http://localhost:8080/gray
 * nginx原生 灰度测试
 */
@RestController
@Slf4j
public class GayController {

    @Value("${gray.version}")
    private String version;

    Map<String,String> routeTable = new ConcurrentHashMap<>();

    /**
     * 灰度测试入口
     * http://localhost/login/1
     * http://localhost/login/2
     * http://localhost/login/3
     *
     * 1 用户全局认证的时候，根据用户已有的属性，设置cookie值，例如: saas的租户id, 1,2,3的发送到v1
     * @return
     */
    @RequestMapping("/login/{mockUserId}")
    @SneakyThrows
    public ModelAndView publishConfig(HttpServletResponse response, @PathVariable("mockUserId") String mockUserId) {
        if (routeTable.isEmpty()){
            routeTable.put("1","V1");
            routeTable.put("2","V2");
            routeTable.put("3","V3");
        }
        String version = routeTable.get(mockUserId);
        log.info("userId: [{}] -> version: [{}] ",mockUserId,version);
        //Cookie 根据用户的登录属性，或者自定义规则，来实现 和version的匹配,这里为了匹配方便，就在不同的版本里写死了
        Cookie cookie = new Cookie("version", version);
        cookie.setPath("/");
        response.addCookie(cookie);
        //Header
        response.addHeader("version",version);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("gay");
        return modelAndView;
    }

    @RequestMapping("/grayRequest")
    @SneakyThrows
    public String grayRequest(HttpServletResponse response) {
        return version;
    }
}
