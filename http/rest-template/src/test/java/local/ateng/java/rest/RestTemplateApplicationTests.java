package local.ateng.java.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RestTemplateApplicationTests {
    private final RestTemplate restTemplate;

    @Test
    void getForObject() {
        //getForObject方法，获取响应体，将其转换为第二个参数指定的类型
        JSONObject jsonObject = restTemplate.getForObject("https://www.wanandroid.com/article/list/0/json", JSONObject.class);
        String string = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
        System.out.println(string);
    }

    @Test
    void getForEntity() {
        //getForEntity方法，返回值为ResponseEntity类型
        // ResponseEntity中包含了响应结果中的所有信息，比如头、状态、body
        ResponseEntity<JSONObject> response = restTemplate.getForEntity("https://www.wanandroid.com/article/list/0/json", JSONObject.class);
        // 获取状态对象
        HttpStatusCode httpStatusCode = response.getStatusCode();
        System.out.println("StatusCode=" + httpStatusCode);
        // 获取状态码
        int statusCodeValue = response.getStatusCodeValue();
        System.out.println("StatusCodeValue=" + statusCodeValue);
        // 获取headers
        HttpHeaders httpHeaders = response.getHeaders();
        System.out.println("Headers=" + httpHeaders);
        // 获取body
        JSONObject body = response.getBody();
        System.out.println(body);
    }

    @Test
    void getForObject2() {
        //url中有动态参数
        String url = "https://www.wanandroid.com/article/list/{id}/json";
        HashMap<String, String> map = new HashMap<>();
        map.put("id", "3");
        JSONObject jsonObject = restTemplate.getForObject(url, JSONObject.class, map);
        System.out.println(jsonObject);
    }

    @Test
    void getForObjectWithQueryParams() {
        // url中有动态路径参数
        String url = "https://www.wanandroid.com/article/list/{id}/json";

        // 设置路径参数
        HashMap<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "3");

        // 使用UriComponentsBuilder添加查询参数
        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .encode() // 强制使用UTF-8编码
                .toUriString();
        System.out.println(finalUrl);

        // 发起请求并获取响应体
        JSONObject jsonObject = restTemplate.getForObject(finalUrl, JSONObject.class, pathParams);
        System.out.println(jsonObject);
    }

    @Test
    void getForObject3() {
        // get请求，带请求头的方式，使用exchange
        String url = "https://www.wanandroid.com/article/list/{id}/json";
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("header-1", "V1");
        headers.add("header-2", "Spring");
        headers.add("header-3", "SpringBoot");
        // url的参数
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("id", "3");
        // HttpEntity：HTTP实体，内部包含了请求头和请求体
        HttpEntity requestEntity = new HttpEntity(
                null,//body部分，get请求没有body，所以为null
                headers //头
        );
        // 使用exchange发送请求
        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(
                url, //url
                HttpMethod.GET, //请求方式
                requestEntity, //请求实体（头、body）
                JSONObject.class,//返回的结果类型
                uriVariables //url中的占位符对应的值
        );
        System.out.println(responseEntity.getStatusCodeValue());
        System.out.println(responseEntity.getHeaders());
        System.out.println(responseEntity.getBody());
    }

    @Test
    void postForObject() {
        // 发送form-data表单post请求
        String url = "https://api.apiopen.top/api/login";
        //①：表单信息，需要放在MultiValueMap中，MultiValueMap相当于Map<String,List<String>>
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        //调用add方法填充表单数据(表单名称:值)
        body.add("account", "309324904@qq.com");
        body.add("password", "123456");
        //②：发送请求(url,请求体，返回值需要转换的类型)
        JSONObject jsonObject = restTemplate.postForObject(url, body, JSONObject.class);
        System.out.println(jsonObject);
    }

    @Test
    public void postRequestUsingPostForObject() {
        String url = "https://jsonplaceholder.typicode.com/posts";

        // 创建请求体
        String requestBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        // 使用 postForObject 发送 POST 请求并返回响应体
        String response = restTemplate.postForObject(url, requestBody, String.class);

        // 打印响应
        System.out.println(response);
    }

    @Test
    public void postRequestUsingExchange() {
        String url = "https://jsonplaceholder.typicode.com/posts";

        // 构建请求体
        String requestBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        // 创建HttpHeaders并设置Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HttpEntity，将请求体和请求头封装在一起
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 使用 exchange 发送 POST 请求
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,  // 请求方法为 POST
                entity,           // 请求体和头
                String.class      // 响应类型
        );

        // 打印响应
        System.out.println(response.getBody());
    }

}
