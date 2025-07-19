package local.ateng.java.rest.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MyRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // 在请求之前执行的操作，比如添加请求头、日志记录等
        System.out.println("请求 URL: " + request.getURI());
        System.out.println("请求方法: " + request.getMethod());

        // 继续执行请求
        return execution.execute(request, body);
    }
}