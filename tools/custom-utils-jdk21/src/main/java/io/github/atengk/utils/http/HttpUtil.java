package io.github.atengk.utils.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 JDK 21 HttpClient 的 HTTP 请求、下载、上传综合工具类。
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class HttpUtil {

    private static final Logger LOGGER = Logger.getLogger(HttpUtil.class.getName());
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 8192;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<String> DEFAULT_SENSITIVE_HEADERS = Set.of("authorization", "cookie", "set-cookie", "x-api-key", "token");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("filename\\*?=([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final String CRLF = "\r\n";
    private static final HttpClient DEFAULT_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    private HttpUtil() {
    }

    /**
     * 创建默认请求配置。
     *
     * @return 默认请求配置
     */
    public static RequestOptions defaultRequestOptions() {
        return RequestOptions.defaults();
    }

    /**
     * 创建默认下载配置构建器。
     *
     * @param url       下载地址
     * @param targetDir 保存目录
     * @return 下载配置构建器
     */
    public static DownloadOptions.Builder downloadOptions(String url, Path targetDir) {
        return DownloadOptions.builder(url, targetDir);
    }

    /**
     * 创建默认上传配置构建器。
     *
     * @param url 上传地址
     * @return 上传配置构建器
     */
    public static UploadOptions.Builder uploadOptions(String url) {
        return UploadOptions.builder(url);
    }

    /**
     * 创建自定义 HttpClient。
     *
     * @param options 客户端配置
     * @return HttpClient
     */
    public static HttpClient newClient(ClientOptions options) {
        ClientOptions opts = options == null ? ClientOptions.defaults() : options;
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(opts.version())
                .followRedirects(opts.redirect())
                .connectTimeout(opts.connectTimeout());
        if (opts.proxy() != null) {
            builder.proxy(ProxySelector.of(opts.proxy()));
        }
        if (opts.authenticator() != null) {
            builder.authenticator(opts.authenticator());
        }
        if (opts.sslContext() != null) {
            builder.sslContext(opts.sslContext());
        }
        if (opts.cookieManager() != null) {
            builder.cookieHandler(opts.cookieManager());
        }
        if (opts.executor() != null) {
            builder.executor(opts.executor());
        }
        return builder.build();
    }

    /**
     * 创建接受全部证书的 SSLContext，仅建议测试环境使用。
     *
     * @return SSLContext
     */
    public static SSLContext insecureSslContext() {
        try {
            TrustManager[] managers = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, managers, new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new HttpSslException("创建 SSLContext 失败", e);
        }
    }

    /**
     * 创建 CookieManager。
     *
     * @return CookieManager
     */
    public static CookieManager cookieManager() {
        return new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    }

    /**
     * 获取 CookieStore。
     *
     * @param manager CookieManager
     * @return CookieStore
     */
    public static CookieStore cookieStore(CookieManager manager) {
        return Objects.requireNonNull(manager, "manager不能为空").getCookieStore();
    }

    /**
     * 发送 GET 请求并返回字符串。
     *
     * @param url 请求地址
     * @return 响应字符串
     */
    public static String get(String url) {
        return get(url, RequestOptions.defaults()).bodyAsString();
    }

    /**
     * 发送 GET 请求。
     *
     * @param url     请求地址
     * @param options 请求配置
     * @return 响应结果
     */
    public static HttpResult<byte[]> get(String url, RequestOptions options) {
        return request("GET", url, HttpRequest.BodyPublishers.noBody(), options);
    }

    /**
     * 发送 GET 请求并返回字节数组。
     *
     * @param url 请求地址
     * @return 响应字节数组
     */
    public static byte[] getBytes(String url) {
        return get(url, RequestOptions.defaults()).body();
    }

    /**
     * 发送 DELETE 请求。
     *
     * @param url 请求地址
     * @return 响应结果
     */
    public static HttpResult<byte[]> delete(String url) {
        return request("DELETE", url, HttpRequest.BodyPublishers.noBody(), RequestOptions.defaults());
    }

    /**
     * 发送 HEAD 请求。
     *
     * @param url 请求地址
     * @return 响应结果
     */
    public static HttpResult<byte[]> head(String url) {
        return request("HEAD", url, HttpRequest.BodyPublishers.noBody(), RequestOptions.defaults());
    }

    /**
     * 发送 OPTIONS 请求。
     *
     * @param url 请求地址
     * @return 响应结果
     */
    public static HttpResult<byte[]> options(String url) {
        return request("OPTIONS", url, HttpRequest.BodyPublishers.noBody(), RequestOptions.defaults());
    }

    /**
     * 发送 JSON POST 请求。
     *
     * @param url  请求地址
     * @param json JSON 字符串
     * @return 响应结果
     */
    public static HttpResult<byte[]> postJson(String url, String json) {
        RequestOptions options = RequestOptions.defaults()
                .withHeader("Content-Type", "application/json; charset=UTF-8")
                .withHeader("Accept", "application/json");
        return request("POST", url, HttpRequest.BodyPublishers.ofString(nullToEmpty(json), DEFAULT_CHARSET), options);
    }

    /**
     * 发送 JSON POST 请求。
     *
     * @param url  请求地址
     * @param body 可基础序列化的对象
     * @return 响应结果
     */
    public static HttpResult<byte[]> postJson(String url, Object body) {
        return postJson(url, toJson(body));
    }

    /**
     * 发送表单 POST 请求。
     *
     * @param url    请求地址
     * @param params 表单参数
     * @return 响应结果
     */
    public static HttpResult<byte[]> postForm(String url, Map<String, ?> params) {
        RequestOptions options = RequestOptions.defaults()
                .withHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return request("POST", url, HttpRequest.BodyPublishers.ofString(toQueryString(params), DEFAULT_CHARSET), options);
    }

    /**
     * 发送 PUT 请求。
     *
     * @param url  请求地址
     * @param body 请求体
     * @return 响应结果
     */
    public static HttpResult<byte[]> put(String url, String body) {
        return request("PUT", url, HttpRequest.BodyPublishers.ofString(nullToEmpty(body), DEFAULT_CHARSET), RequestOptions.defaults());
    }

    /**
     * 发送 PATCH 请求。
     *
     * @param url  请求地址
     * @param body 请求体
     * @return 响应结果
     */
    public static HttpResult<byte[]> patch(String url, String body) {
        return request("PATCH", url, HttpRequest.BodyPublishers.ofString(nullToEmpty(body), DEFAULT_CHARSET), RequestOptions.defaults());
    }

    /**
     * 发送自定义请求。
     *
     * @param method  请求方法
     * @param url     请求地址
     * @param body    请求体发布器
     * @param options 请求配置
     * @return 响应结果
     */
    public static HttpResult<byte[]> request(String method, String url, HttpRequest.BodyPublisher body, RequestOptions options) {
        RequestOptions opts = options == null ? RequestOptions.defaults() : options;
        validateUrlBySecurity(url, opts.securityOptions());
        HttpRequest request = buildRequest(method, url, body, opts);
        return sendWithRetry(DEFAULT_CLIENT, request, opts);
    }

    /**
     * 使用指定客户端发送请求。
     *
     * @param client  HttpClient
     * @param method  请求方法
     * @param url     请求地址
     * @param body    请求体发布器
     * @param options 请求配置
     * @return 响应结果
     */
    public static HttpResult<byte[]> request(HttpClient client, String method, String url, HttpRequest.BodyPublisher body, RequestOptions options) {
        RequestOptions opts = options == null ? RequestOptions.defaults() : options;
        validateUrlBySecurity(url, opts.securityOptions());
        HttpRequest request = buildRequest(method, url, body, opts);
        return sendWithRetry(client == null ? DEFAULT_CLIENT : client, request, opts);
    }

    /**
     * 异步发送 GET 请求。
     *
     * @param url 请求地址
     * @return 异步响应结果
     */
    public static CompletableFuture<HttpResult<byte[]>> getAsync(String url) {
        return requestAsync("GET", url, HttpRequest.BodyPublishers.noBody(), RequestOptions.defaults());
    }

    /**
     * 异步发送 POST JSON 请求。
     *
     * @param url  请求地址
     * @param json JSON 字符串
     * @return 异步响应结果
     */
    public static CompletableFuture<HttpResult<byte[]>> postJsonAsync(String url, String json) {
        RequestOptions options = RequestOptions.defaults()
                .withHeader("Content-Type", "application/json; charset=UTF-8");
        return requestAsync("POST", url, HttpRequest.BodyPublishers.ofString(nullToEmpty(json), DEFAULT_CHARSET), options);
    }

    /**
     * 异步发送自定义请求。
     *
     * @param method  请求方法
     * @param url     请求地址
     * @param body    请求体发布器
     * @param options 请求配置
     * @return 异步响应结果
     */
    public static CompletableFuture<HttpResult<byte[]>> requestAsync(String method, String url, HttpRequest.BodyPublisher body, RequestOptions options) {
        return CompletableFuture.supplyAsync(() -> request(method, url, body, options));
    }

    /**
     * 批量 GET 请求。
     *
     * @param urls           请求地址列表
     * @param maxConcurrency 最大并发数
     * @return 响应结果列表
     */
    public static List<HttpResult<byte[]>> batchGet(Collection<String> urls, int maxConcurrency) {
        return batchRequest(urls, maxConcurrency, url -> HttpUtil.get(url, RequestOptions.defaults()));
    }

    /**
     * 批量执行请求。
     *
     * @param urls           请求地址列表
     * @param maxConcurrency 最大并发数
     * @param requester      请求函数
     * @return 响应结果列表
     */
    public static List<HttpResult<byte[]>> batchRequest(Collection<String> urls, int maxConcurrency, Requester requester) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        int permits = Math.max(1, maxConcurrency);
        Semaphore semaphore = new Semaphore(permits);
        List<CompletableFuture<HttpResult<byte[]>>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return requester.request(url);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new HttpRequestException("批量请求被中断", e);
                    } finally {
                        semaphore.release();
                    }
                }))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * 下载文件到指定目录。
     *
     * @param url       下载地址
     * @param targetDir 保存目录
     * @return 下载结果
     */
    public static DownloadResult download(String url, Path targetDir) {
        return download(DownloadOptions.builder(url, targetDir).build());
    }

    /**
     * 下载文件到指定文件。
     *
     * @param url        下载地址
     * @param targetPath 保存文件
     * @return 下载结果
     */
    public static DownloadResult downloadToFile(String url, Path targetPath) {
        return download(DownloadOptions.builder(url, targetPath.getParent()).targetPath(targetPath).build());
    }

    /**
     * 下载文件并返回字节数组。
     *
     * @param url 下载地址
     * @return 文件字节数组
     */
    public static byte[] downloadBytes(String url) {
        RequestOptions options = RequestOptions.defaults();
        HttpResult<byte[]> result = get(url, options);
        if (!result.success()) {
            throw new HttpDownloadException("下载字节失败，状态码=" + result.statusCode());
        }
        return result.body();
    }

    /**
     * 下载文件。
     *
     * @param options 下载配置
     * @return 下载结果
     */
    public static DownloadResult download(DownloadOptions options) {
        Objects.requireNonNull(options, "options不能为空");
        validateUrlBySecurity(options.url(), options.securityOptions());
        Instant start = Instant.now();
        Path targetPath = null;
        Path tmpPath = null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(options.url()))
                    .GET()
                    .timeout(options.timeout());
            applyHeaders(builder, options.headers());

            boolean resumed = false;
            long existedSize = 0L;
            targetPath = resolveDownloadTarget(options);
            Files.createDirectories(targetPath.getParent());
            handleExistingFile(targetPath, options.overwriteStrategy());
            tmpPath = options.useTempFile() ? targetPath.resolveSibling(targetPath.getFileName() + options.tempSuffix()) : targetPath;
            if (options.resume() && Files.exists(tmpPath)) {
                existedSize = Files.size(tmpPath);
                if (existedSize > 0) {
                    builder.header("Range", "bytes=" + existedSize + "-");
                    resumed = true;
                }
            }

            HttpResponse<InputStream> response = DEFAULT_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (!isSuccessStatus(statusCode) && statusCode != 206) {
                throw new HttpStatusException("下载响应状态异常", statusCode, readAllBytesLimited(response.body(), options.maxResponseErrorBytes()));
            }
            long contentLength = contentLength(response.headers()).orElse(-1L);
            long expectedTotal = resumed && contentLength >= 0 ? existedSize + contentLength : contentLength;
            checkDownloadSize(expectedTotal, options.maxFileSize());
            CopyStats stats = copyToFile(response.body(), tmpPath, resumed, expectedTotal, options.progressListener(), options.maxFileSize());
            if (options.useTempFile() && !tmpPath.equals(targetPath)) {
                Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            String md5 = options.calculateMd5() ? digestHex(targetPath, "MD5") : null;
            String sha256 = options.calculateSha256() ? digestHex(targetPath, "SHA-256") : null;
            Duration duration = Duration.between(start, Instant.now());
            log(options.logEnabled(), Level.INFO, "文件下载完成：url={0}, path={1}, size={2}, durationMs={3}",
                    options.url(), targetPath, stats.totalBytes(), duration.toMillis());
            return new DownloadResult(true, options.url(), targetPath, targetPath.getFileName().toString(), stats.totalBytes(),
                    firstHeader(response.headers(), "Content-Type").orElse(null), md5, sha256, duration, false, resumed, null, statusCode);
        } catch (Exception e) {
            if (options.cleanTempFileOnFailure() && tmpPath != null && !tmpPath.equals(targetPath)) {
                try {
                    Files.deleteIfExists(tmpPath);
                } catch (IOException ignore) {
                    log(options.logEnabled(), Level.WARNING, "清理临时文件失败：{0}", tmpPath);
                }
            }
            throw e instanceof HttpException ? (HttpException) e : new HttpDownloadException("文件下载失败：" + options.url(), e);
        }
    }

    /**
     * 异步下载文件。
     *
     * @param options 下载配置
     * @return 异步下载结果
     */
    public static CompletableFuture<DownloadResult> downloadAsync(DownloadOptions options) {
        return CompletableFuture.supplyAsync(() -> download(options));
    }

    /**
     * 批量下载文件。
     *
     * @param optionsList    下载配置列表
     * @param maxConcurrency 最大并发数
     * @return 下载结果列表
     */
    public static List<DownloadResult> batchDownload(Collection<DownloadOptions> optionsList, int maxConcurrency) {
        if (optionsList == null || optionsList.isEmpty()) {
            return List.of();
        }
        Semaphore semaphore = new Semaphore(Math.max(1, maxConcurrency));
        List<CompletableFuture<DownloadResult>> futures = optionsList.stream()
                .map(options -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return download(options);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new HttpDownloadException("批量下载被中断", e);
                    } finally {
                        semaphore.release();
                    }
                }))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * 上传单个文件。
     *
     * @param url       上传地址
     * @param fieldName 文件字段名
     * @param file      文件路径
     * @return 上传结果
     */
    public static HttpResult<byte[]> uploadFile(String url, String fieldName, Path file) {
        return upload(UploadOptions.builder(url).addFile(fieldName, file).build());
    }

    /**
     * 上传文件和表单参数。
     *
     * @param options 上传配置
     * @return 上传结果
     */
    public static HttpResult<byte[]> upload(UploadOptions options) {
        Objects.requireNonNull(options, "options不能为空");
        validateUrlBySecurity(options.url(), options.securityOptions());
        String boundary = "----HttpUtilBoundary" + UUID.randomUUID().toString().replace("-", "");
        try {
            MultipartBody multipart = buildMultipartBody(boundary, options);
            RequestOptions requestOptions = RequestOptions.defaults()
                    .withTimeout(options.timeout())
                    .withHeaders(options.headers())
                    .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .withMaxResponseBytes(options.maxResponseBytes())
                    .withLogEnabled(options.logEnabled());
            return request("POST", options.url(), HttpRequest.BodyPublishers.ofByteArray(multipart.body()), requestOptions);
        } catch (IOException e) {
            throw new HttpUploadException("文件上传失败：" + options.url(), e);
        }
    }

    /**
     * 异步上传。
     *
     * @param options 上传配置
     * @return 异步上传结果
     */
    public static CompletableFuture<HttpResult<byte[]>> uploadAsync(UploadOptions options) {
        return CompletableFuture.supplyAsync(() -> upload(options));
    }

    /**
     * 获取远程资源元数据。
     *
     * @param url 请求地址
     * @return 元数据
     */
    public static RemoteMetadata metadata(String url) {
        HttpResult<byte[]> result = head(url);
        return new RemoteMetadata(url, result.statusCode(), result.headers(), getContentLength(result.headers()).orElse(-1L),
                firstHeader(result.headers(), "Content-Type").orElse(null),
                firstHeader(result.headers(), "Last-Modified").orElse(null),
                firstHeader(result.headers(), "ETag").orElse(null));
    }

    /**
     * 获取远程资源长度。
     *
     * @param url 请求地址
     * @return 长度
     */
    public static long getContentLength(String url) {
        return metadata(url).contentLength();
    }

    /**
     * 获取响应头中的 Content-Length。
     *
     * @param headers 响应头
     * @return 长度
     */
    public static Optional<Long> getContentLength(Map<String, List<String>> headers) {
        return firstHeader(headers, "Content-Length").map(Long::parseLong);
    }

    /**
     * 获取响应头中的 Content-Type。
     *
     * @param headers 响应头
     * @return Content-Type
     */
    public static Optional<String> getContentType(Map<String, List<String>> headers) {
        return firstHeader(headers, "Content-Type");
    }

    /**
     * 从 URL 或响应头提取文件名。
     *
     * @param url     请求地址
     * @param headers 响应头
     * @return 文件名
     */
    public static String getFileName(String url, Map<String, List<String>> headers) {
        Optional<String> disposition = firstHeader(headers, "Content-Disposition");
        if (disposition.isPresent()) {
            Matcher matcher = FILE_NAME_PATTERN.matcher(disposition.get());
            if (matcher.find()) {
                String value = matcher.group(1).trim().replace("\"", "");
                int idx = value.indexOf("''");
                if (idx >= 0) {
                    value = value.substring(idx + 2);
                }
                return sanitizeFileName(urlDecode(value));
            }
        }
        URI uri = URI.create(url);
        String path = uri.getPath();
        String name = path == null || path.isBlank() ? "download.bin" : path.substring(path.lastIndexOf('/') + 1);
        return sanitizeFileName(name.isBlank() ? "download.bin" : urlDecode(name));
    }

    /**
     * 拼接 Query 参数。
     *
     * @param baseUrl 基础地址
     * @param params  参数
     * @return 拼接后的地址
     */
    public static String appendQuery(String baseUrl, Map<String, ?> params) {
        String query = toQueryString(params);
        if (query.isBlank()) {
            return baseUrl;
        }
        String separator = baseUrl.contains("?") ? (baseUrl.endsWith("?") || baseUrl.endsWith("&") ? "" : "&") : "?";
        return baseUrl + separator + query;
    }

    /**
     * 转换为 Query 字符串。
     *
     * @param params 参数
     * @return Query 字符串
     */
    public static String toQueryString(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .flatMap(entry -> flattenParam(entry.getKey(), entry.getValue()).stream())
                .map(pair -> urlEncode(pair.key()) + "=" + urlEncode(pair.value()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 替换路径参数。
     *
     * @param template 路径模板
     * @param params   路径参数
     * @return 替换后的路径
     */
    public static String replacePathParams(String template, Map<String, ?> params) {
        String result = template;
        if (params == null) {
            return result;
        }
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String value = entry.getValue() == null ? "" : urlEncode(String.valueOf(entry.getValue()));
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    /**
     * 拼接 URL 路径。
     *
     * @param baseUrl 基础地址
     * @param paths   路径片段
     * @return 拼接结果
     */
    public static String joinUrl(String baseUrl, String... paths) {
        String result = trimRight(baseUrl, "/");
        if (paths == null) {
            return result;
        }
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            result += "/" + trimBoth(path, "/");
        }
        return result;
    }

    /**
     * URL 编码。
     *
     * @param value 原始值
     * @return 编码结果
     */
    public static String urlEncode(String value) {
        return URLEncoder.encode(nullToEmpty(value), DEFAULT_CHARSET).replace("+", "%20");
    }

    /**
     * URL 解码。
     *
     * @param value 编码值
     * @return 解码结果
     */
    public static String urlDecode(String value) {
        return URLDecoder.decode(nullToEmpty(value), DEFAULT_CHARSET);
    }

    /**
     * 校验 URL 是否合法。
     *
     * @param url 请求地址
     * @return 是否合法
     */
    public static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 清理文件名非法字符。
     *
     * @param fileName 文件名
     * @return 安全文件名
     */
    public static String sanitizeFileName(String fileName) {
        String name = nullToEmpty(fileName).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return name.isBlank() ? "download.bin" : name;
    }

    /**
     * 创建 Bearer Token Header。
     *
     * @param token Token
     * @return Header Map
     */
    public static Map<String, String> bearerAuth(String token) {
        return Map.of("Authorization", "Bearer " + nullToEmpty(token));
    }

    /**
     * 创建 Basic Auth Header。
     *
     * @param username 用户名
     * @param password 密码
     * @return Header Map
     */
    public static Map<String, String> basicAuth(String username, String password) {
        String raw = nullToEmpty(username) + ":" + nullToEmpty(password);
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(DEFAULT_CHARSET)));
    }

    /**
     * 创建 Cookie Header。
     *
     * @param cookies Cookie Map
     * @return Cookie 字符串
     */
    public static String cookieHeader(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        return cookies.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * 合并 Header。
     *
     * @param first  第一组 Header
     * @param second 第二组 Header
     * @return 合并结果
     */
    public static Map<String, String> mergeHeaders(Map<String, String> first, Map<String, String> second) {
        Map<String, String> result = new LinkedHashMap<>();
        if (first != null) {
            result.putAll(first);
        }
        if (second != null) {
            result.putAll(second);
        }
        return result;
    }

    /**
     * 脱敏 Header。
     *
     * @param headers          Header Map
     * @param sensitiveHeaders 敏感 Header
     * @return 脱敏结果
     */
    public static Map<String, String> maskHeaders(Map<String, String> headers, Set<String> sensitiveHeaders) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Set<String> sensitive = normalizeHeaderNames(sensitiveHeaders == null ? DEFAULT_SENSITIVE_HEADERS : sensitiveHeaders);
        Map<String, String> result = new LinkedHashMap<>();
        headers.forEach((key, value) -> result.put(key, sensitive.contains(key.toLowerCase(Locale.ROOT)) ? "******" : value));
        return result;
    }

    /**
     * 响应体转字符串。
     *
     * @param body    响应字节
     * @param charset 字符集
     * @return 字符串
     */
    public static String bodyToString(byte[] body, Charset charset) {
        return new String(body == null ? new byte[0] : body, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 判断是否 JSON 类型。
     *
     * @param contentType Content-Type
     * @return 是否 JSON
     */
    public static boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json");
    }

    /**
     * 判断是否 XML 类型。
     *
     * @param contentType Content-Type
     * @return 是否 XML
     */
    public static boolean isXml(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("xml");
    }

    /**
     * 判断是否文本类型。
     *
     * @param contentType Content-Type
     * @return 是否文本
     */
    public static boolean isText(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/") || lower.contains("json") || lower.contains("xml") || lower.contains("x-www-form-urlencoded");
    }

    /**
     * 从 Content-Type 推断字符集。
     *
     * @param contentType Content-Type
     * @return 字符集
     */
    public static Charset detectCharset(String contentType) {
        if (contentType == null) {
            return DEFAULT_CHARSET;
        }
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                return Charset.forName(trimmed.substring("charset=".length()).trim());
            }
        }
        return DEFAULT_CHARSET;
    }

    /**
     * 基础 JSON 序列化。
     *
     * @param value 值
     * @return JSON 字符串
     */
    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return "\"" + escapeJson(str) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> toJson(String.valueOf(e.getKey())) + ":" + toJson(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(HttpUtil::toJson).collect(Collectors.joining(",", "[", "]"));
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(value, i));
            }
            return toJson(list);
        }
        return toJson(String.valueOf(value));
    }

    /**
     * JSON 字符串转对象。
     *
     * @param json  JSON 字符串
     * @param codec JSON 编解码器
     * @param type  目标类型
     * @param <T>   目标类型
     * @return 对象
     */
    public static <T> T fromJson(String json, JsonCodec codec, Class<T> type) {
        Objects.requireNonNull(codec, "codec不能为空，请传入 Jackson/Gson/Fastjson 等适配器");
        return codec.fromJson(json, type);
    }

    /**
     * 读取 SSE 事件流。
     *
     * @param url       请求地址
     * @param options   请求配置
     * @param onMessage 消息回调
     */
    public static void readSse(String url, RequestOptions options, Consumer<String> onMessage) {
        RequestOptions opts = options == null ? RequestOptions.defaults() : options;
        HttpRequest request = buildRequest("GET", url, HttpRequest.BodyPublishers.noBody(), opts.withHeader("Accept", "text/event-stream"));
        try {
            HttpResponse<InputStream> response = DEFAULT_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (!isSuccessStatus(response.statusCode())) {
                throw new HttpStatusException("SSE 响应状态异常", response.statusCode(), readAllBytesLimited(response.body(), opts.maxResponseBytes()));
            }
            StringBuilder event = new StringBuilder();
            try (InputStream inputStream = response.body()) {
                String text = new String(inputStream.readAllBytes(), DEFAULT_CHARSET);
                for (String line : text.split("\\R")) {
                    if (line.isBlank()) {
                        if (!event.isEmpty()) {
                            onMessage.accept(event.toString());
                            event.setLength(0);
                        }
                    } else if (line.startsWith("data:")) {
                        if (!event.isEmpty()) {
                            event.append('\n');
                        }
                        event.append(line.substring(5).trim());
                    }
                }
                if (!event.isEmpty()) {
                    onMessage.accept(event.toString());
                }
            }
        } catch (IOException e) {
            throw new HttpRequestException("读取 SSE 失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException("读取 SSE 被中断", e);
        }
    }

    /**
     * 创建 WebSocket 连接。
     *
     * @param uri      WebSocket 地址
     * @param listener 监听器
     * @return WebSocket Future
     */
    public static CompletableFuture<WebSocket> webSocket(URI uri, WebSocket.Listener listener) {
        return DEFAULT_CLIENT.newWebSocketBuilder().buildAsync(uri, listener);
    }

    /**
     * 创建缓存条件 Header。
     *
     * @param etag         ETag
     * @param lastModified Last-Modified
     * @return Header Map
     */
    public static Map<String, String> conditionalHeaders(String etag, String lastModified) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (etag != null && !etag.isBlank()) {
            headers.put("If-None-Match", etag);
        }
        if (lastModified != null && !lastModified.isBlank()) {
            headers.put("If-Modified-Since", lastModified);
        }
        return headers;
    }

    /**
     * 条件 GET 请求。
     *
     * @param url          请求地址
     * @param etag         ETag
     * @param lastModified Last-Modified
     * @return 缓存响应
     */
    public static CacheResult conditionalGet(String url, String etag, String lastModified) {
        RequestOptions options = RequestOptions.defaults().withHeaders(conditionalHeaders(etag, lastModified));
        HttpResult<byte[]> result = get(url, options);
        boolean notModified = result.statusCode() == 304;
        return new CacheResult(notModified, result, firstHeader(result.headers(), "ETag").orElse(etag),
                firstHeader(result.headers(), "Last-Modified").orElse(lastModified));
    }

    /**
     * 校验 URL 安全策略。
     *
     * @param url     请求地址
     * @param options 安全配置
     */
    public static void validateUrlBySecurity(String url, SecurityOptions options) {
        SecurityOptions opts = options == null ? SecurityOptions.defaults() : options;
        if (!isValidUrl(url)) {
            throw new HttpSecurityException("URL 不合法：" + url);
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!opts.allowedSchemes().contains(scheme)) {
            throw new HttpSecurityException("URL 协议不允许：" + scheme);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new HttpSecurityException("URL Host 为空");
        }
        if (!opts.allowedHosts().isEmpty() && opts.allowedHosts().stream().noneMatch(host::equalsIgnoreCase)) {
            throw new HttpSecurityException("URL Host 不在白名单：" + host);
        }
        if (opts.blockedHosts().stream().anyMatch(host::equalsIgnoreCase)) {
            throw new HttpSecurityException("URL Host 被禁止：" + host);
        }
        if (opts.blockPrivateAddress()) {
            checkPrivateAddress(host);
        }
    }

    /**
     * 校验保存路径是否在基础目录内。
     *
     * @param baseDir 基础目录
     * @param target  目标路径
     */
    public static void validatePathInsideBase(Path baseDir, Path target) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path actual = target.toAbsolutePath().normalize();
        if (!actual.startsWith(base)) {
            throw new HttpSecurityException("保存路径不允许越过基础目录：" + target);
        }
    }

    /**
     * 计算文件摘要。
     *
     * @param path      文件路径
     * @param algorithm 摘要算法
     * @return 十六进制摘要
     */
    public static String digestHex(Path path, String algorithm) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            throw new HttpFileException("计算文件摘要失败：" + path, e);
        }
    }

    /**
     * 判断状态码是否成功。
     *
     * @param statusCode 状态码
     * @return 是否成功
     */
    public static boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 获取第一个响应头。
     *
     * @param headers Header Map
     * @param name    Header 名称
     * @return Header 值
     */
    public static Optional<String> firstHeader(Map<String, List<String>> headers, String name) {
        if (headers == null || headers.isEmpty() || name == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return Optional.ofNullable(entry.getValue().getFirst());
            }
        }
        return Optional.empty();
    }

    /**
     * 获取第一个响应头。
     *
     * @param headers HttpHeaders
     * @param name    Header 名称
     * @return Header 值
     */
    public static Optional<String> firstHeader(HttpHeaders headers, String name) {
        return firstHeader(headers.map(), name);
    }

    /**
     * 响应头转 Map。
     *
     * @param headers HttpHeaders
     * @return Header Map
     */
    public static Map<String, List<String>> headersToMap(HttpHeaders headers) {
        return headers == null ? Map.of() : headers.map();
    }

    /**
     * 创建请求构造器。
     *
     * @param method  请求方法
     * @param url     请求地址
     * @param body    请求体
     * @param options 请求配置
     * @return 请求对象
     */
    private static HttpRequest buildRequest(String method, String url, HttpRequest.BodyPublisher body, RequestOptions options) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(options.timeout())
                .method(method.toUpperCase(Locale.ROOT), body == null ? HttpRequest.BodyPublishers.noBody() : body);
        applyHeaders(builder, options.headers());
        return builder.build();
    }

    /**
     * 发送请求并应用重试策略。
     *
     * @param client  HttpClient
     * @param request 请求对象
     * @param options 请求配置
     * @return 响应结果
     */
    private static HttpResult<byte[]> sendWithRetry(HttpClient client, HttpRequest request, RequestOptions options) {
        Instant start = Instant.now();
        RetryOptions retryOptions = options.retryOptions();
        int maxAttempts = Math.max(1, retryOptions.maxRetries() + 1);
        Throwable lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logRequest(options, request, attempt);
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                byte[] bytes = readAllBytesLimited(response.body(), options.maxResponseBytes());
                HttpResult<byte[]> result = new HttpResult<>(isSuccessStatus(response.statusCode()), response.statusCode(), response.headers().map(),
                        bytes, bytes, null, Duration.between(start, Instant.now()), request.uri());
                logResponse(options, request, result);
                if (shouldRetryStatus(response.statusCode(), retryOptions) && attempt < maxAttempts && isIdempotent(request.method(), retryOptions)) {
                    sleepBeforeRetry(retryOptions, attempt);
                    continue;
                }
                return result;
            } catch (IOException e) {
                lastError = e;
                if (attempt < maxAttempts && isIdempotent(request.method(), retryOptions)) {
                    log(options.logEnabled(), Level.WARNING, "请求异常，准备重试：method={0}, uri={1}, attempt={2}", request.method(), request.uri(), attempt);
                    sleepBeforeRetry(retryOptions, attempt);
                    continue;
                }
                throw new HttpRequestException("HTTP 请求失败：" + request.uri(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HttpRequestException("HTTP 请求被中断：" + request.uri(), e);
            }
        }
        throw new HttpRetryException("重试后请求仍失败：" + request.uri(), lastError);
    }

    private static void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, value) -> {
            if (key != null && value != null) {
                builder.header(key, value);
            }
        });
    }

    private static boolean shouldRetryStatus(int statusCode, RetryOptions options) {
        return options.retryStatusCodes().contains(statusCode);
    }

    private static boolean isIdempotent(String method, RetryOptions options) {
        if (options.retryNonIdempotent()) {
            return true;
        }
        return Set.of("GET", "HEAD", "OPTIONS", "DELETE", "PUT").contains(method.toUpperCase(Locale.ROOT));
    }

    private static void sleepBeforeRetry(RetryOptions options, int attempt) {
        try {
            long base = options.interval().toMillis();
            long sleep = options.exponentialBackoff() ? base * (1L << Math.max(0, attempt - 1)) : base;
            if (options.jitter()) {
                sleep += SecureRandom.getInstanceStrong().nextLong(Math.max(1L, base));
            }
            TimeUnit.MILLISECONDS.sleep(Math.min(sleep, options.maxInterval().toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRetryException("重试等待被中断", e);
        } catch (Exception e) {
            throw new HttpRetryException("重试等待失败", e);
        }
    }

    private static byte[] readAllBytesLimited(InputStream inputStream, long maxBytes) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0L;
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (maxBytes > 0 && total > maxBytes) {
                    throw new HttpResponseTooLargeException("响应体超过限制：" + maxBytes + " bytes");
                }
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    private static void logRequest(RequestOptions options, HttpRequest request, int attempt) {
        if (!options.logEnabled()) {
            return;
        }
        log(true, Level.INFO, "HTTP 请求开始：method={0}, uri={1}, attempt={2}, headers={3}",
                request.method(), request.uri(), attempt, maskHeaders(options.headers(), options.sensitiveHeaders()));
    }

    private static void logResponse(RequestOptions options, HttpRequest request, HttpResult<byte[]> result) {
        if (!options.logEnabled()) {
            return;
        }
        Level level = result.success() ? Level.INFO : Level.WARNING;
        log(true, level, "HTTP 请求结束：method={0}, uri={1}, status={2}, durationMs={3}",
                request.method(), request.uri(), result.statusCode(), result.duration().toMillis());
    }

    private static void log(boolean enabled, Level level, String template, Object... args) {
        if (enabled) {
            LOGGER.log(level, template, args);
        }
    }

    private static List<QueryPair> flattenParam(String key, Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().filter(Objects::nonNull).map(item -> new QueryPair(key, String.valueOf(item))).toList();
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<QueryPair> list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                if (item != null) {
                    list.add(new QueryPair(key, String.valueOf(item)));
                }
            }
            return list;
        }
        return List.of(new QueryPair(key, String.valueOf(value)));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trimRight(String value, String target) {
        String result = nullToEmpty(value);
        while (result.endsWith(target)) {
            result = result.substring(0, result.length() - target.length());
        }
        return result;
    }

    private static String trimBoth(String value, String target) {
        String result = nullToEmpty(value);
        while (result.startsWith(target)) {
            result = result.substring(target.length());
        }
        while (result.endsWith(target)) {
            result = result.substring(0, result.length() - target.length());
        }
        return result;
    }

    private static Set<String> normalizeHeaderNames(Set<String> headers) {
        if (headers == null) {
            return DEFAULT_SENSITIVE_HEADERS;
        }
        return headers.stream().filter(Objects::nonNull).map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private static Optional<Long> contentLength(HttpHeaders headers) {
        return firstHeader(headers, "Content-Length").map(Long::parseLong);
    }

    private static Path resolveDownloadTarget(DownloadOptions options) throws IOException {
        Path targetPath = options.targetPath();
        if (targetPath != null) {
            validatePathInsideBase(options.targetDir(), targetPath);
            return targetPath;
        }
        String fileName = options.fileName();
        if (fileName == null || fileName.isBlank()) {
            HttpResult<byte[]> head = head(options.url());
            fileName = getFileName(options.url(), head.headers());
        }
        Path path = options.targetDir().resolve(sanitizeFileName(fileName));
        validatePathInsideBase(options.targetDir(), path);
        return path;
    }

    private static void handleExistingFile(Path path, OverwriteStrategy strategy) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        switch (strategy) {
            case OVERWRITE -> Files.delete(path);
            case SKIP -> throw new HttpFileAlreadyExistsException("文件已存在，跳过下载：" + path);
            case FAIL -> throw new HttpFileAlreadyExistsException("文件已存在：" + path);
            case RENAME -> {
                Path renamed = nextAvailablePath(path);
                Files.move(path, renamed, StandardCopyOption.REPLACE_EXISTING);
            }
            default -> throw new HttpFileException("未知覆盖策略：" + strategy);
        }
    }

    private static Path nextAvailablePath(Path path) {
        String fileName = path.getFileName().toString();
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        for (int i = 1; i < 10000; i++) {
            Path candidate = path.resolveSibling(base + "(" + i + ")" + ext);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new HttpFileException("无法生成可用文件名：" + path);
    }

    private static void checkDownloadSize(long size, long maxSize) {
        if (maxSize > 0 && size > maxSize) {
            throw new HttpResponseTooLargeException("下载文件超过限制：" + maxSize + " bytes");
        }
    }

    private static CopyStats copyToFile(InputStream inputStream, Path targetPath, boolean append, long totalBytes,
                                        ProgressListener listener, long maxSize) throws IOException {
        long current = append && Files.exists(targetPath) ? Files.size(targetPath) : 0L;
        AtomicLong written = new AtomicLong(current);
        try (InputStream in = inputStream; OutputStream out = Files.newOutputStream(targetPath,
                append ? new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND}
                        : new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING})) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            Instant started = Instant.now();
            while ((len = in.read(buffer)) != -1) {
                long after = written.addAndGet(len);
                checkDownloadSize(after, maxSize);
                out.write(buffer, 0, len);
                if (listener != null) {
                    listener.onProgress(new ProgressEvent(after, totalBytes, speed(after - current, started), started));
                }
            }
        }
        return new CopyStats(written.get());
    }

    private static long speed(long bytes, Instant start) {
        long seconds = Math.max(1, Duration.between(start, Instant.now()).toSeconds());
        return bytes / seconds;
    }

    private static MultipartBody buildMultipartBody(String boundary, UploadOptions options) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<String, String> entry : options.formFields().entrySet()) {
            writeTextPart(out, boundary, entry.getKey(), entry.getValue());
        }
        for (UploadFile file : options.files()) {
            writeFilePart(out, boundary, file);
        }
        out.write(("--" + boundary + "--" + CRLF).getBytes(DEFAULT_CHARSET));
        return new MultipartBody(out.toByteArray());
    }

    private static void writeTextPart(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + CRLF).getBytes(DEFAULT_CHARSET));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF + CRLF).getBytes(DEFAULT_CHARSET));
        out.write(nullToEmpty(value).getBytes(DEFAULT_CHARSET));
        out.write(CRLF.getBytes(DEFAULT_CHARSET));
    }

    private static void writeFilePart(ByteArrayOutputStream out, String boundary, UploadFile file) throws IOException {
        String fileName = file.fileName() == null ? file.path().getFileName().toString() : file.fileName();
        String contentType = file.contentType() == null ? "application/octet-stream" : file.contentType();
        out.write(("--" + boundary + CRLF).getBytes(DEFAULT_CHARSET));
        out.write(("Content-Disposition: form-data; name=\"" + file.fieldName() + "\"; filename=\"" + sanitizeFileName(fileName) + "\"" + CRLF).getBytes(DEFAULT_CHARSET));
        out.write(("Content-Type: " + contentType + CRLF + CRLF).getBytes(DEFAULT_CHARSET));
        Files.copy(file.path(), out);
        out.write(CRLF.getBytes(DEFAULT_CHARSET));
    }

    private static void checkPrivateAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new HttpSecurityException("URL 指向内网或本地地址：" + host);
                }
            }
        } catch (IOException e) {
            throw new HttpSecurityException("解析 Host 失败：" + host, e);
        }
    }

    private static String escapeJson(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private record QueryPair(String key, String value) {
    }

    private record CopyStats(long totalBytes) {
    }

    private record MultipartBody(byte[] body) {
    }

    /**
     * HTTP 请求函数。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @FunctionalInterface
    public interface Requester {
        /**
         * 执行请求。
         *
         * @param url 请求地址
         * @return 响应结果
         */
        HttpResult<byte[]> request(String url);
    }

    /**
     * JSON 编解码扩展接口。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public interface JsonCodec {
        /**
         * JSON 转对象。
         *
         * @param json JSON 字符串
         * @param type 目标类型
         * @param <T>  目标类型
         * @return 对象
         */
        <T> T fromJson(String json, Class<T> type);

        /**
         * 对象转 JSON。
         *
         * @param value 对象
         * @return JSON 字符串
         */
        String toJson(Object value);
    }

    /**
     * 进度监听器。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @FunctionalInterface
    public interface ProgressListener {
        /**
         * 进度回调。
         *
         * @param event 进度事件
         */
        void onProgress(ProgressEvent event);
    }

    /**
     * 进度事件。
     *
     * @param transferred 已传输字节数
     * @param total       总字节数，未知时为 -1
     * @param bytesPerSec 每秒字节数
     * @param startedAt   开始时间
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record ProgressEvent(long transferred, long total, long bytesPerSec, Instant startedAt) {
        /**
         * 获取百分比。
         *
         * @return 百分比，未知时为 -1
         */
        public double percent() {
            return total > 0 ? transferred * 100.0 / total : -1D;
        }
    }

    /**
     * HTTP 响应结果。
     * @param success      是否成功
     * @param statusCode   状态码
     * @param headers      响应头
     * @param body         响应体
     * @param rawBody      原始字节
     * @param errorMessage 错误信息
     * @param duration     耗时
     * @param requestUri   请求地址
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record HttpResult<T>(boolean success, int statusCode, Map<String, List<String>> headers, T body, byte[] rawBody,
                                String errorMessage, Duration duration, URI requestUri) {
        /**
         * 响应体转 UTF-8 字符串。
         *
         * @return 字符串
         */
        public String bodyAsString() {
            if (body instanceof byte[] bytes) {
                Charset charset = HttpUtil.firstHeader(headers, "Content-Type").map(HttpUtil::detectCharset).orElse(DEFAULT_CHARSET);
                return new String(bytes, charset);
            }
            return body == null ? "" : String.valueOf(body);
        }

        /**
         * 获取第一个 Header。
         *
         * @param name Header 名称
         * @return Header 值
         */
        public Optional<String> firstHeader(String name) {
            return HttpUtil.firstHeader(headers, name);
        }
    }

    /**
     * 下载结果。
     * @param success      是否成功
     * @param url          下载地址
     * @param filePath     文件路径
     * @param fileName     文件名
     * @param fileSize     文件大小
     * @param contentType  内容类型
     * @param md5          MD5
     * @param sha256       SHA-256
     * @param duration     耗时
     * @param fromCache    是否缓存命中
     * @param resumed      是否断点续传
     * @param errorMessage 错误信息
     * @param statusCode   状态码
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record DownloadResult(boolean success, String url, Path filePath, String fileName, long fileSize,
                                 String contentType, String md5, String sha256, Duration duration,
                                 boolean fromCache, boolean resumed, String errorMessage, int statusCode) {
    }

    /**
     * 远程资源元数据。
     * @param url          请求地址
     * @param statusCode   状态码
     * @param headers      响应头
     * @param contentLength 内容长度
     * @param contentType  内容类型
     * @param lastModified 最后修改时间
     * @param etag         ETag
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record RemoteMetadata(String url, int statusCode, Map<String, List<String>> headers, long contentLength,
                                 String contentType, String lastModified, String etag) {
    }

    /**
     * 缓存条件请求结果。
     * @param notModified  是否未修改
     * @param result       HTTP 响应
     * @param etag         ETag
     * @param lastModified 最后修改时间
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record CacheResult(boolean notModified, HttpResult<byte[]> result, String etag, String lastModified) {
    }

    /**
     * 客户端配置。
     * @param connectTimeout 连接超时
     * @param redirect       重定向策略
     * @param version        HTTP 版本
     * @param proxy          代理地址
     * @param authenticator  认证器
     * @param sslContext     SSL 上下文
     * @param cookieManager  Cookie 管理器
     * @param executor       执行器
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record ClientOptions(Duration connectTimeout, HttpClient.Redirect redirect, HttpClient.Version version,
                                InetSocketAddress proxy, Authenticator authenticator, SSLContext sslContext,
                                CookieManager cookieManager, Executor executor) {
        /**
         * 默认客户端配置。
         *
         * @return 默认配置
         */
        public static ClientOptions defaults() {
            return new ClientOptions(Duration.ofSeconds(10), HttpClient.Redirect.NORMAL, HttpClient.Version.HTTP_2,
                    null, null, null, HttpUtil.cookieManager(), Executors.newVirtualThreadPerTaskExecutor());
        }

        /**
         * 设置代理。
         *
         * @param proxy 代理地址
         * @return 新配置
         */
        public ClientOptions withProxy(InetSocketAddress proxy) {
            return new ClientOptions(connectTimeout, redirect, version, proxy, authenticator, sslContext, cookieManager, executor);
        }

        /**
         * 设置 SSL 上下文。
         *
         * @param sslContext SSL 上下文
         * @return 新配置
         */
        public ClientOptions withSslContext(SSLContext sslContext) {
            return new ClientOptions(connectTimeout, redirect, version, proxy, authenticator, sslContext, cookieManager, executor);
        }
    }

    /**
     * 请求配置。
     * @param timeout           请求超时
     * @param headers           Header
     * @param retryOptions      重试配置
     * @param maxResponseBytes  最大响应体
     * @param logEnabled        是否打印日志
     * @param bodyLogEnabled    是否打印 Body 日志
     * @param sensitiveHeaders  敏感 Header
     * @param securityOptions   安全配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record RequestOptions(Duration timeout, Map<String, String> headers, RetryOptions retryOptions, long maxResponseBytes,
                                 boolean logEnabled, boolean bodyLogEnabled, Set<String> sensitiveHeaders, SecurityOptions securityOptions) {
        /**
         * 默认请求配置。
         *
         * @return 默认配置
         */
        public static RequestOptions defaults() {
            return new RequestOptions(DEFAULT_TIMEOUT, new LinkedHashMap<>(), RetryOptions.defaults(), 20L * 1024 * 1024,
                    true, false, DEFAULT_SENSITIVE_HEADERS, SecurityOptions.defaults());
        }

        /**
         * 增加 Header。
         *
         * @param name  名称
         * @param value 值
         * @return 新配置
         */
        public RequestOptions withHeader(String name, String value) {
            Map<String, String> map = new LinkedHashMap<>(headers);
            map.put(name, value);
            return withHeaders(map);
        }

        /**
         * 设置 Header。
         *
         * @param newHeaders Header
         * @return 新配置
         */
        public RequestOptions withHeaders(Map<String, String> newHeaders) {
            Map<String, String> map = new LinkedHashMap<>();
            if (newHeaders != null) {
                map.putAll(newHeaders);
            }
            return new RequestOptions(timeout, map, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }

        /**
         * 设置超时时间。
         *
         * @param timeout 超时时间
         * @return 新配置
         */
        public RequestOptions withTimeout(Duration timeout) {
            return new RequestOptions(timeout, headers, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }

        /**
         * 设置重试配置。
         *
         * @param retryOptions 重试配置
         * @return 新配置
         */
        public RequestOptions withRetryOptions(RetryOptions retryOptions) {
            return new RequestOptions(timeout, headers, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }

        /**
         * 设置响应体大小限制。
         *
         * @param maxResponseBytes 最大响应体
         * @return 新配置
         */
        public RequestOptions withMaxResponseBytes(long maxResponseBytes) {
            return new RequestOptions(timeout, headers, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }

        /**
         * 设置日志开关。
         *
         * @param logEnabled 是否启用
         * @return 新配置
         */
        public RequestOptions withLogEnabled(boolean logEnabled) {
            return new RequestOptions(timeout, headers, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }

        /**
         * 设置安全配置。
         *
         * @param securityOptions 安全配置
         * @return 新配置
         */
        public RequestOptions withSecurityOptions(SecurityOptions securityOptions) {
            return new RequestOptions(timeout, headers, retryOptions, maxResponseBytes, logEnabled, bodyLogEnabled, sensitiveHeaders, securityOptions);
        }
    }

    /**
     * 重试配置。
     * @param maxRetries          最大重试次数
     * @param interval            初始间隔
     * @param maxInterval         最大间隔
     * @param exponentialBackoff  是否指数退避
     * @param jitter              是否抖动
     * @param retryStatusCodes    可重试状态码
     * @param retryNonIdempotent  是否重试非幂等请求
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record RetryOptions(int maxRetries, Duration interval, Duration maxInterval, boolean exponentialBackoff,
                               boolean jitter, Set<Integer> retryStatusCodes, boolean retryNonIdempotent) {
        /**
         * 默认重试配置。
         *
         * @return 默认配置
         */
        public static RetryOptions defaults() {
            return new RetryOptions(0, Duration.ofMillis(200), Duration.ofSeconds(3), true, false,
                    Set.of(408, 425, 429, 500, 502, 503, 504), false);
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetries 最大重试次数
         * @return 新配置
         */
        public RetryOptions withMaxRetries(int maxRetries) {
            return new RetryOptions(maxRetries, interval, maxInterval, exponentialBackoff, jitter, retryStatusCodes, retryNonIdempotent);
        }

        /**
         * 设置重试间隔。
         *
         * @param interval 重试间隔
         * @return 新配置
         */
        public RetryOptions withInterval(Duration interval) {
            return new RetryOptions(maxRetries, interval, maxInterval, exponentialBackoff, jitter, retryStatusCodes, retryNonIdempotent);
        }
    }

    /**
     * 安全配置。
     * @param allowedSchemes      允许协议
     * @param allowedHosts        Host 白名单
     * @param blockedHosts        Host 黑名单
     * @param blockPrivateAddress 是否阻止内网地址
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record SecurityOptions(Set<String> allowedSchemes, Set<String> allowedHosts, Set<String> blockedHosts, boolean blockPrivateAddress) {
        /**
         * 默认安全配置。
         *
         * @return 默认配置
         */
        public static SecurityOptions defaults() {
            return new SecurityOptions(Set.of("http", "https"), Set.of(), Set.of("169.254.169.254"), false);
        }

        /**
         * 启用 SSRF 基础防护。
         *
         * @return 新配置
         */
        public SecurityOptions enableBlockPrivateAddress() {
            return new SecurityOptions(allowedSchemes, allowedHosts, blockedHosts, true);
        }

        /**
         * 设置 Host 白名单。
         *
         * @param hosts Host 集合
         * @return 新配置
         */
        public SecurityOptions withAllowedHosts(Set<String> hosts) {
            return new SecurityOptions(allowedSchemes, hosts == null ? Set.of() : hosts, blockedHosts, blockPrivateAddress);
        }
    }

    /**
     * 下载覆盖策略。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public enum OverwriteStrategy {
        /** 覆盖旧文件。 */
        OVERWRITE,
        /** 跳过旧文件。 */
        SKIP,
        /** 存在则失败。 */
        FAIL,
        /** 重命名旧文件。 */
        RENAME
    }

    /**
     * 下载配置。
     * @param url                    下载地址
     * @param targetDir              保存目录
     * @param targetPath             保存文件
     * @param fileName               文件名
     * @param headers                Header
     * @param timeout                超时
     * @param maxFileSize            最大文件大小
     * @param maxResponseErrorBytes  最大错误响应体
     * @param overwriteStrategy      覆盖策略
     * @param useTempFile            是否使用临时文件
     * @param tempSuffix             临时文件后缀
     * @param resume                 是否断点续传
     * @param calculateMd5           是否计算 MD5
     * @param calculateSha256        是否计算 SHA-256
     * @param cleanTempFileOnFailure 失败是否清理临时文件
     * @param progressListener       进度监听
     * @param logEnabled             是否打印日志
     * @param securityOptions        安全配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record DownloadOptions(String url, Path targetDir, Path targetPath, String fileName, Map<String, String> headers,
                                  Duration timeout, long maxFileSize, long maxResponseErrorBytes, OverwriteStrategy overwriteStrategy,
                                  boolean useTempFile, String tempSuffix, boolean resume, boolean calculateMd5, boolean calculateSha256,
                                  boolean cleanTempFileOnFailure, ProgressListener progressListener, boolean logEnabled,
                                  SecurityOptions securityOptions) {
        /**
         * 创建构建器。
         *
         * @param url       下载地址
         * @param targetDir 保存目录
         * @return 构建器
         */
        public static Builder builder(String url, Path targetDir) {
            return new Builder(url, targetDir);
        }

        /**
         * 下载配置构建器。
         *
         * @author Ateng
         * @since 2026-05-15
         */
        public static class Builder {
            private final String url;
            private final Path targetDir;
            private Path targetPath;
            private String fileName;
            private Map<String, String> headers = new LinkedHashMap<>();
            private Duration timeout = Duration.ofMinutes(10);
            private long maxFileSize = 1024L * 1024 * 1024;
            private long maxResponseErrorBytes = 1024L * 1024;
            private OverwriteStrategy overwriteStrategy = OverwriteStrategy.OVERWRITE;
            private boolean useTempFile = true;
            private String tempSuffix = ".tmp";
            private boolean resume;
            private boolean calculateMd5;
            private boolean calculateSha256;
            private boolean cleanTempFileOnFailure = true;
            private ProgressListener progressListener;
            private boolean logEnabled = true;
            private SecurityOptions securityOptions = SecurityOptions.defaults();

            private Builder(String url, Path targetDir) {
                this.url = Objects.requireNonNull(url, "url不能为空");
                this.targetDir = Objects.requireNonNull(targetDir, "targetDir不能为空");
            }

            /**
             * 设置保存文件。
             *
             * @param targetPath 保存文件
             * @return 构建器
             */
            public Builder targetPath(Path targetPath) {
                this.targetPath = targetPath;
                return this;
            }

            /**
             * 设置文件名。
             *
             * @param fileName 文件名
             * @return 构建器
             */
            public Builder fileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            /**
             * 设置 Header。
             *
             * @param headers Header
             * @return 构建器
             */
            public Builder headers(Map<String, String> headers) {
                this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
                return this;
            }

            /**
             * 设置超时。
             *
             * @param timeout 超时
             * @return 构建器
             */
            public Builder timeout(Duration timeout) {
                this.timeout = timeout;
                return this;
            }

            /**
             * 设置最大文件大小。
             *
             * @param maxFileSize 最大文件大小
             * @return 构建器
             */
            public Builder maxFileSize(long maxFileSize) {
                this.maxFileSize = maxFileSize;
                return this;
            }

            /**
             * 设置覆盖策略。
             *
             * @param overwriteStrategy 覆盖策略
             * @return 构建器
             */
            public Builder overwriteStrategy(OverwriteStrategy overwriteStrategy) {
                this.overwriteStrategy = overwriteStrategy;
                return this;
            }

            /**
             * 设置是否断点续传。
             *
             * @param resume 是否断点续传
             * @return 构建器
             */
            public Builder resume(boolean resume) {
                this.resume = resume;
                return this;
            }

            /**
             * 设置是否计算 MD5。
             *
             * @param calculateMd5 是否计算 MD5
             * @return 构建器
             */
            public Builder calculateMd5(boolean calculateMd5) {
                this.calculateMd5 = calculateMd5;
                return this;
            }

            /**
             * 设置是否计算 SHA-256。
             *
             * @param calculateSha256 是否计算 SHA-256
             * @return 构建器
             */
            public Builder calculateSha256(boolean calculateSha256) {
                this.calculateSha256 = calculateSha256;
                return this;
            }

            /**
             * 设置进度监听器。
             *
             * @param progressListener 进度监听器
             * @return 构建器
             */
            public Builder progressListener(ProgressListener progressListener) {
                this.progressListener = progressListener;
                return this;
            }

            /**
             * 设置安全配置。
             *
             * @param securityOptions 安全配置
             * @return 构建器
             */
            public Builder securityOptions(SecurityOptions securityOptions) {
                this.securityOptions = securityOptions;
                return this;
            }

            /**
             * 构建下载配置。
             *
             * @return 下载配置
             */
            public DownloadOptions build() {
                return new DownloadOptions(url, targetDir, targetPath, fileName, headers, timeout, maxFileSize,
                        maxResponseErrorBytes, overwriteStrategy, useTempFile, tempSuffix, resume, calculateMd5,
                        calculateSha256, cleanTempFileOnFailure, progressListener, logEnabled, securityOptions);
            }
        }
    }

    /**
     * 上传文件项。
     * @param fieldName   字段名
     * @param path        文件路径
     * @param fileName    文件名
     * @param contentType 内容类型
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record UploadFile(String fieldName, Path path, String fileName, String contentType) {
    }

    /**
     * 上传配置。
     * @param url              上传地址
     * @param headers          Header
     * @param formFields       表单字段
     * @param files            文件列表
     * @param timeout          超时
     * @param maxResponseBytes 最大响应体
     * @param progressListener 进度监听
     * @param logEnabled       是否打印日志
     * @param securityOptions  安全配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record UploadOptions(String url, Map<String, String> headers, Map<String, String> formFields, List<UploadFile> files,
                                Duration timeout, long maxResponseBytes, ProgressListener progressListener, boolean logEnabled,
                                SecurityOptions securityOptions) {
        /**
         * 创建构建器。
         *
         * @param url 上传地址
         * @return 构建器
         */
        public static Builder builder(String url) {
            return new Builder(url);
        }

        /**
         * 上传配置构建器。
         *
         * @author Ateng
         * @since 2026-05-15
         */
        public static class Builder {
            private final String url;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private final Map<String, String> formFields = new LinkedHashMap<>();
            private final List<UploadFile> files = new ArrayList<>();
            private Duration timeout = Duration.ofMinutes(5);
            private long maxResponseBytes = 20L * 1024 * 1024;
            private ProgressListener progressListener;
            private boolean logEnabled = true;
            private SecurityOptions securityOptions = SecurityOptions.defaults();

            private Builder(String url) {
                this.url = Objects.requireNonNull(url, "url不能为空");
            }

            /**
             * 添加 Header。
             *
             * @param name  名称
             * @param value 值
             * @return 构建器
             */
            public Builder header(String name, String value) {
                headers.put(name, value);
                return this;
            }

            /**
             * 添加表单字段。
             *
             * @param name  名称
             * @param value 值
             * @return 构建器
             */
            public Builder formField(String name, String value) {
                formFields.put(name, value);
                return this;
            }

            /**
             * 添加上传文件。
             *
             * @param fieldName 字段名
             * @param path      文件路径
             * @return 构建器
             */
            public Builder addFile(String fieldName, Path path) {
                return addFile(fieldName, path, path.getFileName().toString(), "application/octet-stream");
            }

            /**
             * 添加上传文件。
             *
             * @param fieldName   字段名
             * @param path        文件路径
             * @param fileName    文件名
             * @param contentType 内容类型
             * @return 构建器
             */
            public Builder addFile(String fieldName, Path path, String fileName, String contentType) {
                files.add(new UploadFile(fieldName, path, fileName, contentType));
                return this;
            }

            /**
             * 设置超时。
             *
             * @param timeout 超时
             * @return 构建器
             */
            public Builder timeout(Duration timeout) {
                this.timeout = timeout;
                return this;
            }

            /**
             * 设置进度监听器。
             *
             * @param progressListener 进度监听器
             * @return 构建器
             */
            public Builder progressListener(ProgressListener progressListener) {
                this.progressListener = progressListener;
                return this;
            }

            /**
             * 构建上传配置。
             *
             * @return 上传配置
             */
            public UploadOptions build() {
                return new UploadOptions(url, Map.copyOf(headers), Map.copyOf(formFields), List.copyOf(files),
                        timeout, maxResponseBytes, progressListener, logEnabled, securityOptions);
            }
        }
    }

    /**
     * HTTP 基础异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpException extends RuntimeException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpException(String message) {
            super(message);
        }

        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 请求异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpRequestException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 状态码异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpStatusException extends HttpException {
        private final int statusCode;
        private final byte[] responseBody;

        /**
         * 创建异常。
         *
         * @param message      消息
         * @param statusCode   状态码
         * @param responseBody 响应体
         */
        public HttpStatusException(String message, int statusCode, byte[] responseBody) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody == null ? new byte[0] : responseBody;
        }

        /**
         * 获取状态码。
         *
         * @return 状态码
         */
        public int statusCode() {
            return statusCode;
        }

        /**
         * 获取响应体。
         *
         * @return 响应体
         */
        public byte[] responseBody() {
            return responseBody.clone();
        }
    }

    /**
     * 重试异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpRetryException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpRetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 下载异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpDownloadException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpDownloadException(String message) {
            super(message);
        }

        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 上传异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpUploadException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * SSL 异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpSslException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpSslException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 安全异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpSecurityException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpSecurityException(String message) {
            super(message);
        }

        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpSecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 响应体过大异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpResponseTooLargeException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpResponseTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * 文件异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpFileException extends HttpException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpFileException(String message) {
            super(message);
        }

        /**
         * 创建异常。
         *
         * @param message 消息
         * @param cause   原因
         */
        public HttpFileException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 文件已存在异常。
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class HttpFileAlreadyExistsException extends HttpFileException {
        /**
         * 创建异常。
         *
         * @param message 消息
         */
        public HttpFileAlreadyExistsException(String message) {
            super(message);
        }
    }
}
