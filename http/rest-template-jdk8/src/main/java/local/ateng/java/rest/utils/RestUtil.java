package local.ateng.java.rest.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * RestUtil 工具类
 *
 * <p>基于自定义 {@code myRestTemplate} 封装常用的 HTTP 接口调用方法，
 * 旨在简化服务间 RESTful 接口的调用流程，提高开发效率和代码可读性。</p>
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>快速调用常用 HTTP 方法：GET、POST、PUT、DELETE</li>
 *     <li>通用 {@code exchange} 方法，支持自定义 HTTP 方法、请求体和泛型返回值</li>
 *     <li>支持多种请求体格式：JSON、XML、表单（x-www-form-urlencoded）、文件上传（multipart/form-data）</li>
 *     <li>统一异常捕获与日志记录，便于排查接口调用问题</li>
 *     <li>支持动态 URL 参数和占位符替换</li>
 * </ul>
 *
 * <h2>使用示例：</h2>
 * <pre>
 *     // JSON POST 请求
 *     Map&lt;String, Object&gt; payload = new HashMap&lt;&gt;();
 *     payload.put("id", 123);
 *     payload.put("name", "张三");
 *     HttpEntity&lt;Map&lt;String, Object&gt;&gt; entity = RestUtil.jsonBodyWithHeaders(payload, null);
 *
 *     ResponseEntity&lt;String&gt; response = RestUtil.exchange(
 *         "https://api.example.com/user",
 *         HttpMethod.POST,
 *         entity,
 *         String.class
 *     );
 *
 *     // GET 请求示例
 *     ResponseEntity&lt;User&gt; resp = RestUtil.get("https://api.example.com/user/{id}", User.class, 123);
 *
 *     // 文件上传示例
 *     File file = new File("D:/test.png");
 *     HttpEntity&lt;MultiValueMap&lt;String, Object&gt;&gt; fileEntity =
 *         RestUtil.multipartBodyWithHeaders(file, "file", null);
 *     ResponseEntity&lt;String&gt; uploadResp = RestUtil.exchange(
 *         "https://api.example.com/upload",
 *         HttpMethod.POST,
 *         fileEntity,
 *         String.class
 *     );
 * </pre>
 *
 * <h2>适用场景：</h2>
 * <ul>
 *     <li>微服务间 RESTful 接口调用</li>
 *     <li>第三方 API 调用（JSON / XML / Form / File）</li>
 *     <li>需要统一接口调用日志和异常处理</li>
 * </ul>
 *
 * <h2>注意事项：</h2>
 * <ul>
 *     <li>本工具类依赖于注入的 {@code myRestTemplate} Bean</li>
 *     <li>POJO 请求体需保证 RestTemplate 配置了对应的 HttpMessageConverter（如 Jackson）</li>
 *     <li>文件上传需服务端支持 multipart/form-data</li>
 * </ul>
 *
 * <p>通过 RestUtil，开发者可快速发起接口请求，无需重复构建 HttpEntity 或处理异常，提高了接口调用的一致性和可维护性。</p>
 *
 * @author 孔余
 * @since 2025-09-09
 */
public class RestUtil {

    private static final Logger log = LoggerFactory.getLogger(RestUtil.class);

    /**
     * Spring 容器中注入的自定义 RestTemplate
     */
    private static final RestTemplate restTemplate = SpringUtil.getBean("atengRestTemplate", RestTemplate.class);

    // ================= GET =================

    /**
     * 发送 GET 请求，返回响应体对象。
     *
     * <p>适用于直接获取目标类型的响应场景。
     *
     * <p>使用示例：
     * <pre>
     *     String result = RestUtil.getForObject("http://example.com/api/{id}", String.class, 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 响应体对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.getForObject(url, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("GET 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    /**
     * 发送 GET 请求，返回完整的 {@link ResponseEntity}。
     *
     * <p>适用于需要获取响应头、状态码等额外信息的场景。
     *
     * <p>使用示例：
     * <pre>
     *     ResponseEntity&lt;String&gt; resp = RestUtil.getForEntity("http://example.com/api/{id}", String.class, 123);
     *     if (resp.getStatusCode().is2xxSuccessful()) {
     *         System.out.println(resp.getBody());
     *     }
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.getForEntity(url, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("GET 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    // ================= POST =================

    /**
     * 发送 POST 请求，返回响应体对象。
     *
     * <p>适用于需要提交请求体并直接获取结果的场景。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     String result = RestUtil.postForObject("http://example.com/save", user, String.class);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 响应体对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.postForObject(url, request, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("POST 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    /**
     * 发送 POST 请求，返回完整的 {@link ResponseEntity}。
     *
     * <p>适用于需要获取响应头、状态码等额外信息的场景。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.postForEntity("http://example.com/save", user, String.class);
     *     if (resp.getStatusCode().is2xxSuccessful()) {
     *         System.out.println(resp.getBody());
     *     }
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.postForEntity(url, request, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("POST 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    // ================= PUT =================

    /**
     * 发送 PUT 请求，不返回响应体。
     *
     * <p>适用于更新资源的场景，通常 REST 接口返回 204 No Content。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 20);
     *     RestUtil.put("http://example.com/update/{id}", user, 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param uriVariables URL 中的占位符参数
     * @throws RestClientException 请求失败时抛出
     */
    public static void put(String url, Object request, Object... uriVariables) {
        try {
            restTemplate.put(url, request, uriVariables);
        } catch (RestClientException e) {
            log.error("PUT 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    // ================= DELETE =================

    /**
     * 发送 DELETE 请求。
     *
     * <p>适用于删除资源的场景。
     *
     * <p>使用示例：
     * <pre>
     *     RestUtil.delete("http://example.com/delete/{id}", 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param uriVariables URL 中的占位符参数
     * @throws RestClientException 请求失败时抛出
     */
    public static void delete(String url, Object... uriVariables) {
        try {
            restTemplate.delete(url, uriVariables);
        } catch (RestClientException e) {
            log.error("DELETE 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    // ================= Exchange =================

    /**
     * 通用请求方法，支持指定 HTTP 方法、请求体、响应类型。
     *
     * <p>适用于需要高度自定义请求头或复杂请求场景。
     *
     * <p>使用示例：
     * <pre>
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setContentType(MediaType.APPLICATION_JSON);
     *     HttpEntity&lt;User&gt; entity = new HttpEntity&lt;&gt;(new User("Tom", 18), headers);
     *
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(
     *         "http://example.com/save",
     *         HttpMethod.POST,
     *         entity,
     *         String.class
     *     );
     * </pre>
     *
     * @param url           请求 URL
     * @param method        HTTP 方法（GET, POST, PUT, DELETE 等）
     * @param requestEntity 封装了请求头和请求体的实体对象
     * @param responseType  响应体类型
     * @param uriVariables  URL 中的占位符参数
     * @param <T>           返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            Class<T> responseType,
            Object... uriVariables) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: url={}, method={}, 请求体={}", url, method, requestEntity, e);
            throw e;
        }
    }

    /**
     * 通用请求方法，支持复杂的泛型返回值（如 List&lt;Map&lt;String,Object&gt;&gt;）。
     *
     * <p>适用于需要反序列化为复杂集合或泛型对象的场景。
     *
     * <p>使用示例：
     * <pre>
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
     *     HttpEntity&lt;Void&gt; entity = new HttpEntity&lt;&gt;(headers);
     *
     *     ResponseEntity&lt;List&lt;User&gt;&gt; resp = RestUtil.exchange(
     *         "http://example.com/users",
     *         HttpMethod.GET,
     *         entity,
     *         new ParameterizedTypeReference&lt;List&lt;User&gt;&gt;() {}
     *     );
     * </pre>
     *
     * @param url           请求 URL
     * @param method        HTTP 方法（GET, POST, PUT, DELETE 等）
     * @param requestEntity 封装了请求头和请求体的实体对象
     * @param responseType  响应体类型的泛型引用
     * @param <T>           返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType);
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: url={}, method={}, 请求体={}", url, method, requestEntity, e);
            throw e;
        }
    }

    /**
     * 构建仅包含请求头的 {@link HttpEntity}。
     *
     * <p>使用示例：
     * <pre>
     *     HttpEntity&lt;Void&gt; entity = RestEntityBuilder.headersOnly(Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.GET, entity, String.class);
     * </pre>
     *
     * @param headersMap 请求头键值对
     * @return HttpEntity 实例
     */
    public static HttpEntity<Void> headersOnly(Map<String, String> headersMap) {
        HttpHeaders headers = new HttpHeaders();
        if (headersMap != null) {
            headersMap.forEach(headers::set);
        }
        return new HttpEntity<>(headers);
    }

    /**
     * 构建包含 JSON 请求体的 {@link HttpEntity}。
     *
     * <p>Content-Type 自动设置为 {@code application/json}。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.jsonBody(user);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param body 请求体对象
     * @param <T>  请求体类型
     * @return HttpEntity 实例
     */
    public static <T> HttpEntity<T> jsonBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 构建包含 XML 请求体的 {@link HttpEntity}。
     *
     * <p>Content-Type 自动设置为 {@code application/xml}。
     * <p>适用于调用需要 XML 格式请求体的接口，例如部分 SOAP/老系统接口。
     *
     * <p>使用示例：
     * <pre>
     *     String xml = "&lt;user&gt;&lt;name&gt;Tom&lt;/name&gt;&lt;age&gt;18&lt;/age&gt;&lt;/user&gt;";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.xmlBody(xml);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param xmlBody XML 格式的字符串
     * @return HttpEntity 实例
     */
    public static HttpEntity<String> xmlBody(String xmlBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        return new HttpEntity<>(xmlBody, headers);
    }

    /**
     * 通用构建方法：根据传入的请求体、请求头 map 和 Content-Type 构建 {@link HttpEntity}。
     *
     * <p>使用示例（POJO -> JSON）：
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.bodyWithHeaders(user,
     *             Collections.singletonMap("Authorization", "Bearer x"),
     *             MediaType.APPLICATION_JSON);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * <p>使用示例（已经是 JSON 字符串）：
     * <pre>
     *     String json = "{\"id\":123,\"name\":\"张三\"}";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.bodyWithHeaders(json,
     *             Collections.singletonMap("X-Custom","v"),
     *             MediaType.APPLICATION_JSON);
     * </pre>
     *
     * @param body       请求体，可以是 {@code String}（已序列化）或 POJO（由 RestTemplate 的 converter 序列化）
     * @param headersMap 请求头键值对（可为 null）
     * @param contentType Content-Type（可为 null，null 时不会设置 Content-Type）
     * @param <T>        请求体类型
     * @return 构建好的 {@link HttpEntity}
     */
    public static <T> HttpEntity<T> bodyWithHeaders(T body, Map<String, String> headersMap, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (headersMap != null && !headersMap.isEmpty()) {
            headersMap.forEach(headers::set);
        }
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * 快捷：构建 JSON 请求体的 {@link HttpEntity}（委托给 {@link #bodyWithHeaders}）。
     *
     * <p>说明：
     * <ul>
     *   <li>当 body 为 POJO 时，RestTemplate 会使用 Jackson 等 converter 自动序列化为 JSON（前提是相关 converter 已配置）。</li>
     *   <li>当 body 为 String 时，方法会把该字符串原样作为请求体发送（不会再做序列化）。</li>
     * </ul>
     * </p>
     *
     * <p>使用示例（POJO）：</p>
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.jsonBodyWithHeaders(user,
     *             Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param body       请求体（POJO 或已序列化的 JSON 字符串）
     * @param headersMap 请求头（可为 null）
     * @param <T>        请求体类型
     * @return HttpEntity
     */
    public static <T> HttpEntity<T> jsonBodyWithHeaders(T body, Map<String, String> headersMap) {
        return bodyWithHeaders(body, headersMap, MediaType.APPLICATION_JSON);
    }

    /**
     * 快捷：构建 XML 请求体的 {@link HttpEntity}（委托给 {@link #bodyWithHeaders}）。
     *
     * <p>通常场景：向需要 application/xml 的老系统或 SOAP-like 接口发送 XML 字符串。</p>
     *
     * <p>使用示例（XML 字符串）：</p>
     * <pre>
     *     String xml = "&lt;user&gt;&lt;name&gt;Tom&lt;/name&gt;&lt;age&gt;18&lt;/age&gt;&lt;/user&gt;";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.xmlBodyWithHeaders(xml,
     *             Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param xmlBody    XML 字符串（建议传 String）
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity&lt;String&gt;
     */
    public static HttpEntity<String> xmlBodyWithHeaders(String xmlBody, Map<String, String> headersMap) {
        return bodyWithHeaders(xmlBody, headersMap, MediaType.APPLICATION_XML);
    }

    // ================= 表单（application/x-www-form-urlencoded） ================= //

    /**
     * 构建 application/x-www-form-urlencoded 的表单请求体。
     *
     * <p>使用示例：</p>
     * <pre>
     *     Map&lt;String, String&gt; formData = new HashMap&lt;&gt;();
     *     formData.put("username", "tom");
     *     formData.put("password", "123456");
     *
     *     HttpEntity&lt;MultiValueMap&lt;String, String&gt;&gt; entity =
     *         RestEntityBuilder.formBodyWithHeaders(formData, null);
     * </pre>
     *
     * @param formData   表单数据（键值对）
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, String>> formBodyWithHeaders(Map<String, String> formData,
                                                                                Map<String, String> headersMap) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (formData != null) {
            formData.forEach(body::add);
        }
        return bodyWithHeaders(body, headersMap, MediaType.APPLICATION_FORM_URLENCODED);
    }

    // ================= 文件上传（multipart/form-data） ================= //

    /**
     * 构建 multipart/form-data 的文件上传请求体。
     *
     * <p>使用示例：</p>
     * <pre>
     *     File file = new File("D:/test.png");
     *     HttpEntity&lt;MultiValueMap&lt;String, Object&gt;&gt; entity =
     *         RestEntityBuilder.multipartBodyWithHeaders(file, "file", null);
     * </pre>
     *
     * @param file       文件对象
     * @param fieldName  表单字段名，例如 "file"
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, Object>> multipartBodyWithHeaders(File file,
                                                                                     String fieldName,
                                                                                     Map<String, String> headersMap) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(fieldName, file);

        return bodyWithHeaders(body, headersMap, MediaType.MULTIPART_FORM_DATA);
    }

    /**
     * 构建 multipart/form-data 的多文件上传请求体。
     *
     * @param files      多个文件（字段名相同）
     * @param fieldName  表单字段名
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, Object>> multipartBodyWithHeaders(File[] files,
                                                                                     String fieldName,
                                                                                     Map<String, String> headersMap) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (files != null) {
            for (File file : files) {
                body.add(fieldName, file);
            }
        }
        return bodyWithHeaders(body, headersMap, MediaType.MULTIPART_FORM_DATA);
    }

}
