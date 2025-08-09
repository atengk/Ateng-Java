package local.ateng.java.customutils.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Http请求工具类，基于HttpURLConnection实现，支持同步和异步GET/POST请求，
 * 支持设置请求头，文件下载，远程文件流获取。
 *
 * @author 孔余
 * @since 2025-08-09
 */
public class HttpUtil {

    /**
     * 连接超时时间，单位毫秒
     */
    private static final int CONNECT_TIMEOUT = 10000;

    /**
     * 读取超时时间，单位毫秒
     */
    private static final int READ_TIMEOUT = 15000;

    /**
     * 发送同步GET请求
     *
     * @param url     请求地址
     * @param params  请求参数（会自动拼接到url后）
     * @param headers 请求头（允许为null）
     * @return 响应字符串
     * @throws IOException 请求异常
     */
    public static String doGet(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        String fullUrl = buildUrlWithParams(url, params);
        HttpURLConnection conn = createConnection(fullUrl, "GET", headers);
        return getResponse(conn);
    }

    /**
     * 发送异步GET请求
     *
     * @param url     请求地址
     * @param params  请求参数
     * @param headers 请求头
     * @return CompletableFuture包装的响应字符串
     */
    public static CompletableFuture<String> doGetAsync(String url, Map<String, String> params, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doGet(url, params, headers);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * 发送同步POST请求，Content-Type为application/json
     *
     * @param url     请求地址
     * @param json    请求体json字符串
     * @param headers 请求头（允许为null）
     * @return 响应字符串
     * @throws IOException 请求异常
     */
    public static String doPostJson(String url, String json, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST", headers);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        writeRequestBody(conn, json);
        return getResponse(conn);
    }

    /**
     * 发送异步POST请求，Content-Type为application/json
     *
     * @param url     请求地址
     * @param json    请求体json字符串
     * @param headers 请求头
     * @return CompletableFuture包装的响应字符串
     */
    public static CompletableFuture<String> doPostJsonAsync(String url, String json, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doPostJson(url, json, headers);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * 发送同步POST请求，Content-Type为application/x-www-form-urlencoded
     *
     * @param url     请求地址
     * @param params  表单参数
     * @param headers 请求头（允许为null）
     * @return 响应字符串
     * @throws IOException 请求异常
     */
    public static String doPostForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST", headers);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        String formBody = buildFormBody(params);
        writeRequestBody(conn, formBody);
        return getResponse(conn);
    }

    /**
     * 发送异步POST请求，Content-Type为application/x-www-form-urlencoded
     *
     * @param url     请求地址
     * @param params  表单参数
     * @param headers 请求头
     * @return CompletableFuture包装的响应字符串
     */
    public static CompletableFuture<String> doPostFormAsync(String url, Map<String, String> params, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doPostForm(url, params, headers);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * 下载文件到本地
     *
     * @param fileUrl      远程文件URL
     * @param destFilePath 本地保存路径（含文件名）
     * @throws IOException 文件下载异常
     */
    public static void downloadFile(String fileUrl, String destFilePath) throws IOException {
        HttpURLConnection conn = createConnection(fileUrl, "GET", null);
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download file. HTTP code: " + responseCode);
        }
        try (InputStream in = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(destFilePath)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        }
    }

    /**
     * 获取远程文件输入流，调用者负责关闭
     *
     * @param fileUrl 文件URL
     * @return 输入流
     * @throws IOException 获取异常
     */
    public static InputStream getFileInputStream(String fileUrl) throws IOException {
        HttpURLConnection conn = createConnection(fileUrl, "GET", null);
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to get file input stream. HTTP code: " + responseCode);
        }
        return conn.getInputStream();
    }

    /**
     * 构建带参数的URL字符串
     *
     * @param url    基础URL
     * @param params 参数Map
     * @return 带参数的完整URL
     * @throws UnsupportedEncodingException 编码异常
     */
    private static String buildUrlWithParams(String url, Map<String, String> params) throws UnsupportedEncodingException {
        // 定义局部变量，使用小驼峰命名
        final String questionMark = "?";
        final String ampersand = "&";
        final String equalSign = "=";
        final String encoding = "UTF-8";

        if (params == null || params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);

        if (!url.contains(questionMark)) {
            sb.append(questionMark);
        } else if (!url.endsWith(ampersand) && !url.endsWith(questionMark)) {
            sb.append(ampersand);
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), encoding));
            sb.append(equalSign);
            sb.append(URLEncoder.encode(entry.getValue(), encoding));
            sb.append(ampersand);
        }
        // 删除最后一个&
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * 构建application/x-www-form-urlencoded请求体字符串
     *
     * @param params 参数Map
     * @return 表单请求体字符串
     * @throws UnsupportedEncodingException 编码异常
     */
    private static String buildFormBody(Map<String, String> params) throws UnsupportedEncodingException {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * 创建HttpURLConnection实例，设置请求方法、请求头及超时等
     *
     * @param urlStr  请求URL
     * @param method  请求方法（GET/POST等）
     * @param headers 请求头Map（可为null）
     * @return HttpURLConnection实例
     * @throws IOException 连接异常
     */
    private static HttpURLConnection createConnection(String urlStr, String method, Map<String, String> headers) throws IOException {
        final String postMethod = "POST";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setDoInput(true);

        if (postMethod.equalsIgnoreCase(method)) {
            conn.setDoOutput(true);
        }

        // 设置请求头
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                conn.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return conn;
    }

    /**
     * 写请求体到连接的输出流
     *
     * @param conn HttpURLConnection实例
     * @param body 请求体字符串
     * @throws IOException 写入异常
     */
    private static void writeRequestBody(HttpURLConnection conn, String body) throws IOException {
        if (body == null || body.isEmpty()) {
            return;
        }
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    /**
     * 从连接获取响应字符串
     *
     * @param conn HttpURLConnection实例
     * @return 响应内容字符串
     * @throws IOException 读取异常
     */
    private static String getResponse(HttpURLConnection conn) throws IOException {
        // 定义局部变量，避免魔法值
        final int successCodeStart = 200;
        final int successCodeEnd = 300;

        int responseCode = conn.getResponseCode();
        InputStream inputStream;

        if (responseCode >= successCodeStart && responseCode < successCodeEnd) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        return readStreamToString(inputStream);
    }
    /**
     * 将输入流读取为字符串，使用UTF-8编码
     *
     * @param inputStream 输入流
     * @return 读取的字符串
     * @throws IOException 读取异常
     */
    private static String readStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

}
