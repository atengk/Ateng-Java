package io.github.atengk;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.atengk.utils.CommonUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for CommonUtil. Each test method maps to one tool batch and prints results.
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CommonUtilTest {

    @Test
    @DisplayName("Batch 01 - String tools")
    void testBatch01StringTools() {
        printTitle("Batch 01 - String tools");
        System.out.println("isBlank: " + CommonUtil.isBlank("  "));
        System.out.println("isNotBlank: " + CommonUtil.isNotBlank("ateng"));
        System.out.println("defaultIfBlank: " + CommonUtil.defaultIfBlank(" ", "default"));
        System.out.println("firstNotBlank: " + CommonUtil.firstNotBlank(null, " ", "first"));
        System.out.println("trimToEmpty: " + CommonUtil.trimToEmpty("  hello  "));
        System.out.println("cleanBlank: " + CommonUtil.cleanBlank("a b c"));
        System.out.println("removeLineBreak: " + CommonUtil.removeLineBreak("a\nb\rc"));
        System.out.println("normalizeBlank: " + CommonUtil.normalizeBlank("a   b   c"));
        System.out.println("equalsIgnoreCase: " + CommonUtil.equalsIgnoreCase("ABC", "abc"));
        System.out.println("containsIgnoreCase: " + CommonUtil.containsIgnoreCase("Hello Ateng", "ateng"));
        System.out.println("sub: " + CommonUtil.sub("abcdef", 1, 4));
        System.out.println("left: " + CommonUtil.left("abcdef", 3));
        System.out.println("right: " + CommonUtil.right("abcdef", 3));
        System.out.println("ellipsis: " + CommonUtil.ellipsis("abcdefghijklmnopqrstuvwxyz", 10));
        System.out.println("addPrefixIfAbsent: " + CommonUtil.addPrefixIfAbsent("api/user", "/"));
        System.out.println("addSuffixIfAbsent: " + CommonUtil.addSuffixIfAbsent("/api", "/"));
        System.out.println("toCamelCase: " + CommonUtil.toCamelCase("user_name"));
        System.out.println("toUnderlineCase: " + CommonUtil.toUnderlineCase("userName"));
        System.out.println("format: " + CommonUtil.format("hello {}", "ateng"));
        System.out.println("formatByMap: " + CommonUtil.formatByMap("name={name}, age=${age}", Map.of("name", "ateng", "age", 18)));
        System.out.println("joinIgnoreBlank: " + CommonUtil.joinIgnoreBlank(",", List.of("a", " ", "b")));
        System.out.println("splitTrimIgnoreBlank: " + CommonUtil.splitTrimIgnoreBlank("a, b, , c", ","));
        System.out.println("isNumber: " + CommonUtil.isNumber("123.45"));
        System.out.println("containsChinese: " + CommonUtil.containsChinese("hello\u4f60\u597d"));
        System.out.println("removeSpecialChar: " + CommonUtil.removeSpecialChar("a@b#c_1-2"));
        System.out.println("retainNumber: " + CommonUtil.retainNumber("a1b2c3"));
        System.out.println("mask: " + CommonUtil.mask("13800138000", 3, 4));
        System.out.println("desensitizedMobile: " + CommonUtil.desensitizedMobile("13800138000"));
        System.out.println("simpleUuid: " + CommonUtil.simpleUuid());
        System.out.println("randomLettersNumbers: " + CommonUtil.randomLettersNumbers(8));
    }

    @Test
    @DisplayName("Batch 02 - Object and Bean tools")
    void testBatch02ObjectAndBeanTools() {
        printTitle("Batch 02 - Object and Bean tools");
        UserSource source = new UserSource(1L, "Ateng", 18, "ateng@example.com");
        System.out.println("isNull: " + CommonUtil.isNull(null));
        System.out.println("isObjectNotEmpty: " + CommonUtil.isObjectNotEmpty(source));
        System.out.println("defaultObjectIfNull: " + CommonUtil.defaultObjectIfNull(null, "default"));
        System.out.println("firstNotNull: " + CommonUtil.firstNotNull(null, null, "value"));
        System.out.println("equalsAny: " + CommonUtil.equalsAny("A", "B", "A", "C"));
        System.out.println("toStr: " + CommonUtil.toStr(123));
        System.out.println("toInt: " + CommonUtil.toInt("123"));
        System.out.println("toLong: " + CommonUtil.toLong("123456"));
        System.out.println("toBool: " + CommonUtil.toBool("true"));
        System.out.println("toBigDecimal: " + CommonUtil.toBigDecimal("12.34"));
        System.out.println("convert: " + CommonUtil.convert("123", Long.class));
        System.out.println("convertList: " + CommonUtil.convertList(List.of("1", "2", "3"), Integer.class));
        System.out.println("isInstanceOf: " + CommonUtil.isInstanceOf(source, UserSource.class));
        System.out.println("safeCast: " + CommonUtil.safeCast(source, UserSource.class));
        UserTarget target = CommonUtil.copyBean(source, UserTarget.class);
        System.out.println("copyBean: " + target);
        UserTarget targetIgnoreNull = CommonUtil.copyBeanIgnoreNull(source, UserTarget.class);
        System.out.println("copyBeanIgnoreNull: " + targetIgnoreNull);
        System.out.println("copyBeanList: " + CommonUtil.copyBeanList(List.of(source), UserTarget.class));
        System.out.println("beanToMap: " + CommonUtil.beanToMap(source));
        System.out.println("beanToUnderlineMap: " + CommonUtil.beanToUnderlineMap(source));
        System.out.println("mapToBean: " + CommonUtil.mapToBean(Map.of("id", 2L, "name", "Tony", "age", 20), UserTarget.class));
        System.out.println("hasField: " + CommonUtil.hasField(UserSource.class, "name"));
        System.out.println("getFieldValue: " + CommonUtil.getFieldValue(source, "name"));
        CommonUtil.setFieldValue(source, "name", "Blair");
        System.out.println("setFieldValue: " + source);
        System.out.println("enumByName: " + CommonUtil.enumByName(UserStatus.class, "ENABLE"));
        System.out.println("enumByFieldValue: " + CommonUtil.enumByFieldValue(UserStatus.class, "code", 1));
    }

    @Test
    @DisplayName("Batch 03 - Collection and Map tools")
    void testBatch03CollectionAndMapTools() {
        printTitle("Batch 03 - Collection and Map tools");
        List<UserSource> users = List.of(
                new UserSource(1L, "Ateng", 18, "a@example.com"),
                new UserSource(2L, "Blair", 20, "b@example.com"),
                new UserSource(3L, "Ateng", 22, "c@example.com")
        );
        System.out.println("isCollNotEmpty: " + CommonUtil.isCollNotEmpty(users));
        System.out.println("emptyListIfNull: " + CommonUtil.emptyListIfNull(null));
        System.out.println("toList: " + CommonUtil.toList("a", "b", "c"));
        System.out.println("toSet: " + CommonUtil.toSet("a", "b", "a"));
        System.out.println("filter: " + CommonUtil.filter(users, item -> item.getAge() >= 20));
        System.out.println("filterNotNull: " + CommonUtil.filterNotNull(List.of("a", "b")));
        System.out.println("map: " + CommonUtil.map(users, UserSource::getName));
        System.out.println("mapNotNull: " + CommonUtil.mapNotNull(users, UserSource::getEmail));
        System.out.println("first: " + CommonUtil.first(users));
        System.out.println("first predicate: " + CommonUtil.first(users, item -> item.getId() == 2L));
        System.out.println("last: " + CommonUtil.last(new ArrayList<>(users)));
        System.out.println("anyMatch: " + CommonUtil.anyMatch(users, item -> item.getAge() > 21));
        System.out.println("allMatch: " + CommonUtil.allMatch(users, item -> item.getAge() >= 18));
        System.out.println("distinct: " + CommonUtil.distinct(List.of("a", "b", "a")));
        System.out.println("distinctBy: " + CommonUtil.distinctBy(users, UserSource::getName));
        System.out.println("sort: " + CommonUtil.sort(users, (a, b) -> a.getAge().compareTo(b.getAge())));
        System.out.println("sortDesc: " + CommonUtil.sortDesc(List.of(1, 3, 2)));
        System.out.println("reverse: " + CommonUtil.reverse(List.of("a", "b", "c")));
        System.out.println("groupBy: " + CommonUtil.groupBy(users, UserSource::getName));
        System.out.println("groupCountBy: " + CommonUtil.groupCountBy(users, UserSource::getName));
        System.out.println("toMap: " + CommonUtil.toMap(users, UserSource::getId, UserSource::getName));
        System.out.println("toMapOverwrite: " + CommonUtil.toMapOverwrite(users, UserSource::getName, UserSource::getAge));
        System.out.println("subList: " + CommonUtil.subList(new ArrayList<>(users), 0, 2));
        System.out.println("page: " + CommonUtil.page(users, 1, 2));
        System.out.println("splitList: " + CommonUtil.splitList(users, 2));
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", "Ateng");
        map.put("age", 18);
        map.put("empty", "");
        map.put("nullValue", null);
        System.out.println("isMapNotEmpty: " + CommonUtil.isMapNotEmpty(map));
        System.out.println("mapGetStr: " + CommonUtil.mapGetStr(map, "name"));
        System.out.println("mapGetInt: " + CommonUtil.mapGetInt(map, "age"));
        System.out.println("containsAllKeys: " + CommonUtil.containsAllKeys(map, "name", "age"));
        System.out.println("putIfNotNull: " + CommonUtil.putIfNotNull(map, "city", "Changsha"));
        System.out.println("removeNullValue: " + CommonUtil.removeNullValue(map));
        System.out.println("removeBlankValue: " + CommonUtil.removeBlankValue(map));
        System.out.println("mapKeyList: " + CommonUtil.mapKeyList(map));
        System.out.println("mergeMap: " + CommonUtil.mergeMap(Map.of("a", 1), Map.of("a", 2, "b", 3), false));
    }

    @Test
    @DisplayName("Batch 04 - Date time tools")
    void testBatch04DateTimeTools() {
        printTitle("Batch 04 - Date time tools");
        LocalDate today = CommonUtil.today();
        LocalDateTime now = CommonUtil.now();
        System.out.println("now: " + now);
        System.out.println("today: " + today);
        System.out.println("currentTimestamp: " + CommonUtil.currentTimestamp());
        System.out.println("formatDate: " + CommonUtil.formatDate(today));
        System.out.println("formatDateTime: " + CommonUtil.formatDateTime(now));
        System.out.println("parseLocalDate: " + CommonUtil.parseLocalDate("2026-04-29"));
        System.out.println("parseLocalDateTime: " + CommonUtil.parseLocalDateTime("2026-04-29 12:30:00"));
        System.out.println("toDate: " + CommonUtil.toDate(now));
        System.out.println("toLocalDateTime: " + CommonUtil.toLocalDateTime(CommonUtil.currentDate()));
        System.out.println("timestampToLocalDateTime: " + CommonUtil.timestampToLocalDateTime(CommonUtil.currentTimestamp()));
        System.out.println("startOfToday: " + CommonUtil.startOfToday());
        System.out.println("endOfToday: " + CommonUtil.endOfToday());
        System.out.println("startOfCurrentWeek: " + CommonUtil.startOfCurrentWeek());
        System.out.println("endOfCurrentMonth: " + CommonUtil.endOfCurrentMonth());
        System.out.println("plusDays: " + CommonUtil.plusDays(now, 3));
        System.out.println("minusHours: " + CommonUtil.minusHours(now, 2));
        System.out.println("isBefore: " + CommonUtil.isBefore(now.minusDays(1), now));
        System.out.println("isBetween: " + CommonUtil.isBetween(now, now.minusDays(1), now.plusDays(1)));
        System.out.println("isToday: " + CommonUtil.isToday(today));
        System.out.println("betweenDays: " + CommonUtil.betweenDays(today.minusDays(3), today));
        System.out.println("betweenMinutes: " + CommonUtil.betweenMinutes(now.minusMinutes(30), now));
        System.out.println("age: " + CommonUtil.age(LocalDate.of(2000, 1, 1)));
        System.out.println("lengthOfMonth: " + CommonUtil.lengthOfMonth(today));
        System.out.println("listDaysBetween: " + CommonUtil.listDaysBetween(today, today.plusDays(2)));
        System.out.println("todayStr: " + CommonUtil.todayStr());
        System.out.println("nowStr: " + CommonUtil.nowStr());
    }

    @Test
    @DisplayName("Batch 05 - Number and amount tools")
    void testBatch05NumberAndAmountTools() {
        printTitle("Batch 05 - Number and amount tools");
        System.out.println("isNumeric: " + CommonUtil.isNumeric("123.45"));
        System.out.println("isZero: " + CommonUtil.isZero(0));
        System.out.println("isPositive: " + CommonUtil.isPositive(10));
        System.out.println("isBetween: " + CommonUtil.isBetween(5, 1, 10));
        System.out.println("toBigDecimalOrZero: " + CommonUtil.toBigDecimalOrZero("12.30"));
        System.out.println("toAmount: " + CommonUtil.toAmount("12.345"));
        System.out.println("yuanToFen: " + CommonUtil.yuanToFen(new BigDecimal("12.34")));
        System.out.println("fenToYuan: " + CommonUtil.fenToYuan(1234));
        System.out.println("limitBetween: " + CommonUtil.limitBetween(120, 0, 100));
        System.out.println("compare: " + CommonUtil.compare(1, 2));
        System.out.println("min: " + CommonUtil.min(1, 2));
        System.out.println("max: " + CommonUtil.max(1, 2));
        System.out.println("add: " + CommonUtil.add(1, 2, 3));
        System.out.println("subtract: " + CommonUtil.subtract(10, 2, 3));
        System.out.println("multiply: " + CommonUtil.multiply(2, 3, 4));
        System.out.println("divide: " + CommonUtil.divide(10, 3));
        System.out.println("safeDivide: " + CommonUtil.safeDivide(10, 0, BigDecimal.ZERO));
        System.out.println("round: " + CommonUtil.round(new BigDecimal("12.345")));
        System.out.println("stripTrailingZeros: " + CommonUtil.stripTrailingZeros(new BigDecimal("12.3400")));
        System.out.println("addAmount: " + CommonUtil.addAmount(new BigDecimal("1.10"), new BigDecimal("2.20")));
        System.out.println("discountAmount: " + CommonUtil.discountAmount(new BigDecimal("100"), new BigDecimal("0.8")));
        System.out.println("taxAmount: " + CommonUtil.taxAmount(new BigDecimal("100"), new BigDecimal("0.13")));
        System.out.println("amountWithTax: " + CommonUtil.amountWithTax(new BigDecimal("100"), new BigDecimal("0.13")));
        System.out.println("ratio: " + CommonUtil.ratio(1, 3));
        System.out.println("percent: " + CommonUtil.percent(1, 3));
        System.out.println("percentStr: " + CommonUtil.percentStr(1, 3));
        System.out.println("formatAmountWithComma: " + CommonUtil.formatAmountWithComma(new BigDecimal("1234567.89")));
        System.out.println("randomInt: " + CommonUtil.randomInt(1, 10));
        System.out.println("randomAmount: " + CommonUtil.randomAmount(1, 10));
    }

    @Test
    @DisplayName("Batch 06 - JSON tools")
    void testBatch06JsonTools() {
        printTitle("Batch 06 - JSON tools");
        UserSource source = new UserSource(1L, "Ateng", 18, "ateng@example.com");
        String json = CommonUtil.toJsonStr(source);
        String jsonArray = CommonUtil.toJsonStr(List.of(source, new UserSource(2L, "Blair", 20, "b@example.com")));
        System.out.println("newJsonObject: " + CommonUtil.newJsonObject());
        System.out.println("newJsonArray: " + CommonUtil.newJsonArray());
        System.out.println("isJson: " + CommonUtil.isJson(json));
        System.out.println("isValidJsonObject: " + CommonUtil.isValidJsonObject(json));
        System.out.println("toJsonStr: " + json);
        System.out.println("toJsonPrettyStr: " + CommonUtil.toJsonPrettyStr(source));
        System.out.println("compactJson: " + CommonUtil.compactJson(CommonUtil.toJsonPrettyStr(source)));
        JSONObject jsonObject = CommonUtil.parseJsonObject(json);
        JSONArray array = CommonUtil.parseJsonArray(jsonArray);
        System.out.println("parseJsonObject: " + jsonObject);
        System.out.println("parseJsonArray: " + array);
        System.out.println("jsonToBean: " + CommonUtil.jsonToBean(json, UserTarget.class));
        System.out.println("jsonToList: " + CommonUtil.jsonToList(jsonArray, UserTarget.class));
        System.out.println("jsonToMap: " + CommonUtil.jsonToMap(json));
        System.out.println("jsonToMapList: " + CommonUtil.jsonToMapList(jsonArray));
        System.out.println("jsonGetStr: " + CommonUtil.jsonGetStr(jsonObject, "name"));
        System.out.println("jsonGetInt: " + CommonUtil.jsonGetInt(jsonObject, "age"));
        System.out.println("jsonPut: " + CommonUtil.jsonPut(jsonObject, "city", "Changsha"));
        System.out.println("jsonArrayAdd: " + CommonUtil.jsonArrayAdd(array, Map.of("id", 3, "name", "Tony")));
        System.out.println("jsonQuote: " + CommonUtil.jsonQuote("hello\"ateng"));
        System.out.println("jsonEscape: " + CommonUtil.jsonEscape("hello\nworld"));
    }

    @Test
    @DisplayName("Batch 07 - File and IO tools")
    void testBatch07FileAndIoTools() {
        printTitle("Batch 07 - File and IO tools");
        File tempDir = CommonUtil.createTempDir("ateng-util-test");
        File file = CommonUtil.toFile(CommonUtil.pathJoin(tempDir.getAbsolutePath(), "sample.txt"));
        try {
            System.out.println("mkdir: " + tempDir);
            System.out.println("touchFile: " + CommonUtil.touchFile(file));
            System.out.println("fileExists: " + CommonUtil.fileExists(file));
            System.out.println("isFile: " + CommonUtil.isFile(file));
            System.out.println("isDirectory: " + CommonUtil.isDirectory(tempDir));
            System.out.println("getAbsolutePath: " + CommonUtil.getAbsolutePath(file));
            System.out.println("getFileName: " + CommonUtil.getFileName(file));
            System.out.println("getMainName: " + CommonUtil.getMainName(file));
            System.out.println("getExtName: " + CommonUtil.getExtName(file));
            System.out.println("isExtName: " + CommonUtil.isExtName(file.getName(), "txt"));
            CommonUtil.writeUtf8String("hello ateng", file);
            System.out.println("readUtf8String: " + CommonUtil.readUtf8String(file));
            CommonUtil.appendUtf8String("\nappend", file);
            System.out.println("readUtf8Lines: " + CommonUtil.readUtf8Lines(file));
            byte[] bytes = CommonUtil.readBytes(file);
            System.out.println("readBytes length: " + bytes.length);
            System.out.println("readUtf8 InputStream: " + CommonUtil.readUtf8(CommonUtil.toInputStream("stream text")));
            File copyFile = CommonUtil.toFile(CommonUtil.pathJoin(tempDir.getAbsolutePath(), "copy.txt"));
            System.out.println("copyFile: " + CommonUtil.copyFile(file, copyFile, true));
            System.out.println("fileSize: " + CommonUtil.fileSize(copyFile));
            System.out.println("formatFileSize: " + CommonUtil.formatFileSize(copyFile));
            System.out.println("listFiles: " + CommonUtil.listFiles(tempDir));
            System.out.println("listOnlyFiles: " + CommonUtil.listOnlyFiles(tempDir));
            System.out.println("loopFiles: " + CommonUtil.loopFiles(tempDir));
            System.out.println("getFileType: " + CommonUtil.getFileType(file));
            System.out.println("isDocumentFile: " + CommonUtil.isDocumentFile(file));
            System.out.println("uniqueFileName: " + CommonUtil.uniqueFileName(file.getName()));
            System.out.println("timestampFileName: " + CommonUtil.timestampFileName(file.getName()));
            System.out.println("cleanFileName: " + CommonUtil.cleanFileName("a:b*c?.txt"));
            System.out.println("limitFileName: " + CommonUtil.limitFileName("very-long-file-name.txt", 12));
        } finally {
            System.out.println("deleteFile tempDir: " + CommonUtil.deleteFile(tempDir));
        }
    }

    @Test
    @DisplayName("Batch 08 - Crypto and encode tools")
    void testBatch08CryptoAndEncodeTools() {
        printTitle("Batch 08 - Crypto and encode tools");
        String text = "hello ateng";
        String key = "ateng-secret-key";
        System.out.println("md5: " + CommonUtil.md5(text));
        System.out.println("md5Equals: " + CommonUtil.md5Equals(text, CommonUtil.md5(text)));
        System.out.println("sha1: " + CommonUtil.sha1(text));
        System.out.println("sha256: " + CommonUtil.sha256(text));
        System.out.println("sha384: " + CommonUtil.sha384(text));
        System.out.println("sha512: " + CommonUtil.sha512(text));
        String base64 = CommonUtil.base64Encode(text);
        System.out.println("base64Encode: " + base64);
        System.out.println("base64DecodeStr: " + CommonUtil.base64DecodeStr(base64));
        System.out.println("isBase64: " + CommonUtil.isBase64(base64));
        String hex = CommonUtil.hexEncode(text);
        System.out.println("hexEncode: " + hex);
        System.out.println("hexDecodeStr: " + CommonUtil.hexDecodeStr(hex));
        System.out.println("isHex: " + CommonUtil.isHex(hex));
        String encoded = CommonUtil.urlEncode("name=Ateng & city=Changsha");
        System.out.println("urlEncode: " + encoded);
        System.out.println("urlDecode: " + CommonUtil.urlDecode(encoded));
        System.out.println("generateAes128KeyBase64: " + CommonUtil.generateAes128KeyBase64());
        byte[] aesKey = CommonUtil.aes128Key(key);
        System.out.println("isValidAesKey: " + CommonUtil.isValidAesKey(aesKey));
        String cipherText = CommonUtil.aesEncryptBase64(text, aesKey);
        System.out.println("aesEncryptBase64: " + cipherText);
        System.out.println("aesDecryptBase64: " + CommonUtil.aesDecryptBase64(cipherText, aesKey));
        String cipherHex = CommonUtil.aesEncryptHex(text, key);
        System.out.println("aesEncryptHex: " + cipherHex);
        System.out.println("aesDecryptHex: " + CommonUtil.aesDecryptHex(cipherHex, key));
        System.out.println("uuidSimple: " + CommonUtil.uuidSimple());
        System.out.println("uuidWithDash: " + CommonUtil.uuidWithDash());
        System.out.println("randomToken: " + CommonUtil.randomToken(16));
        System.out.println("randomCode: " + CommonUtil.randomCode(6));
        System.out.println("randomSalt: " + CommonUtil.randomSalt(12));
    }

    @Test
    @DisplayName("Batch 09 - Web request tools")
    void testBatch09WebRequestTools() throws Exception {
        printTitle("Batch 09 - Web request tools");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/users");
        request.setServerName("example.com");
        request.setServerPort(443);
        request.setScheme("https");
        request.setSecure(true);
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.2");
        request.addHeader("Authorization", "Bearer test-token");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36");
        request.setContentType("application/json");
        request.addParameter("name", "Ateng");
        request.addParameter("age", "18");
        request.setCookies(new Cookie("token", "cookie-token"));
        System.out.println("getRequestMethod: " + CommonUtil.getRequestMethod(request));
        System.out.println("isPostRequest: " + CommonUtil.isPostRequest(request));
        System.out.println("getRequestUri: " + CommonUtil.getRequestUri(request));
        System.out.println("getRequestUrl: " + CommonUtil.getRequestUrl(request));
        System.out.println("getFullRequestUrl: " + CommonUtil.getFullRequestUrl(request));
        System.out.println("isAjaxRequest: " + CommonUtil.isAjaxRequest(request));
        System.out.println("isHttpsRequest: " + CommonUtil.isHttpsRequest(request));
        System.out.println("getParam: " + CommonUtil.getParam(request, "name"));
        System.out.println("getParamInt: " + CommonUtil.getParamInt(request, "age"));
        System.out.println("getParamMap: " + CommonUtil.getParamMap(request));
        System.out.println("getHeader: " + CommonUtil.getHeader(request, "Authorization"));
        System.out.println("getHeaderMap: " + CommonUtil.getHeaderMap(request));
        System.out.println("getBearerToken: " + CommonUtil.getBearerToken(request));
        System.out.println("isJsonRequest: " + CommonUtil.isJsonRequest(request));
        System.out.println("getCookieValue: " + CommonUtil.getCookieValue(request, "token"));
        System.out.println("getClientIp: " + CommonUtil.getClientIp(request));
        System.out.println("isInnerClientIp: " + CommonUtil.isInnerClientIp(request));
        System.out.println("isIpv4: " + CommonUtil.isIpv4("127.0.0.1"));
        System.out.println("getUserAgent: " + CommonUtil.getUserAgent(request));
        System.out.println("getBrowserName: " + CommonUtil.getBrowserName(request));
        System.out.println("getOsName: " + CommonUtil.getOsName(request));
        System.out.println("isMobileRequest: " + CommonUtil.isMobileRequest(request));
        String url = "https://example.com/api/users?name=Ateng";
        System.out.println("isUrl: " + CommonUtil.isUrl(url));
        System.out.println("getUrlHost: " + CommonUtil.getUrlHost(url));
        System.out.println("getUrlPath: " + CommonUtil.getUrlPath(url));
        System.out.println("getUrlQuery: " + CommonUtil.getUrlQuery(url));
        System.out.println("toQueryString: " + CommonUtil.toQueryString(Map.of("name", "Ateng", "page", 1)));
        System.out.println("parseQueryString: " + CommonUtil.parseQueryString("name=Ateng&page=1"));
        System.out.println("appendQueryParam: " + CommonUtil.appendQueryParam(url, "page", 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        CommonUtil.addCookie(response, "sid", "123", 3600);
        CommonUtil.setJsonResponse(response);
        CommonUtil.writeJsonResponse(response, Map.of("success", true));
        System.out.println("responseContentType: " + response.getContentType());
        System.out.println("responseBody: " + response.getContentAsString(StandardCharsets.UTF_8));
        MockHttpServletResponse downloadResponse = new MockHttpServletResponse();
        CommonUtil.writeDownloadBytes(downloadResponse, "test.txt", "hello".getBytes(StandardCharsets.UTF_8));
        System.out.println("downloadHeader: " + downloadResponse.getHeader("Content-Disposition"));
        System.out.println("downloadBodyLength: " + downloadResponse.getContentAsByteArray().length);
    }

    @Test
    @DisplayName("Batch 10 - Parameter validation tools")
    void testBatch10ParameterValidationTools() {
        printTitle("Batch 10 - Parameter validation tools");
        System.out.println("isValidObject: " + CommonUtil.isValidObject("value"));
        System.out.println("isValidStr: " + CommonUtil.isValidStr("Ateng"));
        System.out.println("isValidCollection: " + CommonUtil.isValidCollection(List.of(1, 2)));
        System.out.println("isValidMap: " + CommonUtil.isValidMap(Map.of("a", 1)));
        System.out.println("isValidMobile: " + CommonUtil.isValidMobile("13800138000"));
        System.out.println("isValidEmail: " + CommonUtil.isValidEmail("ateng@example.com"));
        System.out.println("isValidIdCard: " + CommonUtil.isValidIdCard("110101199003078117"));
        System.out.println("isValidUrl: " + CommonUtil.isValidUrl("https://example.com"));
        System.out.println("isValidIp: " + CommonUtil.isValidIp("127.0.0.1"));
        System.out.println("isValidMac: " + CommonUtil.isValidMac("00:1A:2B:3C:4D:5E"));
        System.out.println("isValidDomain: " + CommonUtil.isValidDomain("example.com"));
        System.out.println("isValidPostalCode: " + CommonUtil.isValidPostalCode("410000"));
        System.out.println("isValidBankCard: " + CommonUtil.isValidBankCard("6222021001116245187"));
        System.out.println("isValidPlateNumber: " + CommonUtil.isValidPlateNumber("\u6e58A12345"));
        System.out.println("isValidNumber: " + CommonUtil.isValidNumber("123.45"));
        System.out.println("isValidInteger: " + CommonUtil.isValidInteger("123"));
        System.out.println("isValidAmount: " + CommonUtil.isValidAmount("123.45"));
        System.out.println("isValidPort: " + CommonUtil.isValidPort("8080"));
        System.out.println("isLengthBetween: " + CommonUtil.isLengthBetween("Ateng", 1, 10));
        System.out.println("isByteLengthBetween: " + CommonUtil.isByteLengthBetween("Ateng", 1, 10));
        System.out.println("isValidChinese: " + CommonUtil.isValidChinese("\u5f20\u4e09"));
        System.out.println("isValidLetters: " + CommonUtil.isValidLetters("abcXYZ"));
        System.out.println("isValidDigits: " + CommonUtil.isValidDigits("123456"));
        System.out.println("isValidUsername: " + CommonUtil.isValidUsername("ateng_001"));
        System.out.println("isValidPassword: " + CommonUtil.isValidPassword("abc123456"));
        System.out.println("isStrongPassword: " + CommonUtil.isStrongPassword("Aa123456!"));
        System.out.println("isMatch: " + CommonUtil.isMatch("abc123", "^[a-z]+\\d+$"));
        System.out.println("isValidRegex: " + CommonUtil.isValidRegex("^[a-z]+$"));
        System.out.println("isValidDate: " + CommonUtil.isValidDate("2026-04-29"));
        System.out.println("isValidDateTime: " + CommonUtil.isValidDateTime("2026-04-29 12:30:00"));
        System.out.println("isValidDateRange: " + CommonUtil.isValidDateRange(LocalDate.now(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        System.out.println("isFutureDate: " + CommonUtil.isFutureDate(LocalDate.now().plusDays(1)));
        System.out.println("isSizeBetween: " + CommonUtil.isSizeBetween(List.of(1, 2, 3), 1, 5));
        System.out.println("containsElement: " + CommonUtil.containsElement(List.of("a", "b"), "a"));
        System.out.println("isValidEnumName: " + CommonUtil.isValidEnumName(UserStatus.class, "ENABLE"));
        System.out.println("isValidFileName: " + CommonUtil.isValidFileName("test.txt"));
    }

    @Test
    @DisplayName("Batch 11 - Exception assertion tools")
    void testBatch11ExceptionAssertionTools() {
        printTitle("Batch 11 - Exception assertion tools");
        System.out.println("assertNotNull: " + CommonUtil.assertNotNull("value", "value required"));
        System.out.println("assertNotEmpty: " + CommonUtil.assertNotEmpty("value", "value required"));
        System.out.println("assertNotBlank: " + CommonUtil.assertNotBlank("Ateng", "name required"));
        System.out.println("assertLengthBetween: " + CommonUtil.assertLengthBetween("Ateng", 1, 10, "length invalid"));
        System.out.println("assertCollNotEmpty: " + CommonUtil.assertCollNotEmpty(List.of(1, 2), "list required"));
        System.out.println("assertMapNotEmpty: " + CommonUtil.assertMapNotEmpty(Map.of("a", 1), "map required"));
        System.out.println("assertGreaterThan: " + CommonUtil.assertGreaterThan(10, 1, "number invalid"));
        System.out.println("assertNumberBetween: " + CommonUtil.assertNumberBetween(5, 1, 10, "range invalid"));
        System.out.println("assertMobile: " + CommonUtil.assertMobile("13800138000", "mobile invalid"));
        System.out.println("assertEmail: " + CommonUtil.assertEmail("ateng@example.com", "email invalid"));
        System.out.println("assertUrl: " + CommonUtil.assertUrl("https://example.com", "url invalid"));
        System.out.println("assertDate: " + CommonUtil.assertDate("2026-04-29", "date invalid"));
        System.out.println("assertDateTime: " + CommonUtil.assertDateTime("2026-04-29 12:30:00", "datetime invalid"));
        System.out.println("assertEnumName: " + CommonUtil.assertEnumName(UserStatus.class, "ENABLE", "enum invalid"));
        CommonUtil.assertState(true, "state invalid");
        System.out.println("assertState: ok");
        System.out.println("throwIfNull: " + CommonUtil.throwIfNull("value", CommonUtil.illegalArgument("value required")));
        System.out.println("throwIfBlank: " + CommonUtil.throwIfBlank("value", CommonUtil.illegalArgument("value required")));
        System.out.println("callOrDefault success: " + CommonUtil.callOrDefault(() -> "ok", "default"));
        System.out.println("callOrDefault error: " + CommonUtil.callOrDefault(() -> { throw new IOException("io error"); }, "default"));
        System.out.println("runQuietly success: " + CommonUtil.runQuietly(() -> System.out.println("quiet run")));
        System.out.println("runQuietly error: " + CommonUtil.runQuietly(() -> { throw new IOException("quiet error"); }));
        try {
            CommonUtil.assertNotBlank(" ", "name required");
        } catch (Exception e) {
            System.out.println("getSimpleExceptionMessage: " + CommonUtil.getSimpleExceptionMessage(e));
            System.out.println("getRootCauseMessage: " + CommonUtil.getRootCauseMessage(e));
            System.out.println("containsException: " + CommonUtil.containsException(e, IllegalArgumentException.class));
            System.out.println("findException: " + CommonUtil.findException(e, IllegalArgumentException.class));
            System.out.println("getStackTrace limit: " + CommonUtil.getStackTrace(e, 120));
        }
    }

    @Test
    @DisplayName("Batch 12 - System environment tools")
    void testBatch12SystemEnvironmentTools() {
        printTitle("Batch 12 - System environment tools");
        CommonUtil.setSystemProperty("ateng.test.key", "123");
        System.out.println("getSystemProperty: " + CommonUtil.getSystemProperty("ateng.test.key"));
        System.out.println("hasSystemProperty: " + CommonUtil.hasSystemProperty("ateng.test.key"));
        System.out.println("getSystemPropertyInt: " + CommonUtil.getSystemPropertyInt("ateng.test.key"));
        System.out.println("getEnv JAVA_HOME: " + CommonUtil.getEnv("JAVA_HOME"));
        System.out.println("getEnvOrProperty: " + CommonUtil.getEnvOrProperty("ATENG_TEST_ENV", "ateng.test.key", "default"));
        System.out.println("getOsName: " + CommonUtil.getOsName());
        System.out.println("getOsVersion: " + CommonUtil.getOsVersion());
        System.out.println("getOsArch: " + CommonUtil.getOsArch());
        System.out.println("isWindows: " + CommonUtil.isWindows());
        System.out.println("isLinux: " + CommonUtil.isLinux());
        System.out.println("isMac: " + CommonUtil.isMac());
        System.out.println("isUnixLike: " + CommonUtil.isUnixLike());
        System.out.println("getFileSeparator: " + CommonUtil.getFileSeparator());
        System.out.println("getDefaultCharsetName: " + CommonUtil.getDefaultCharsetName());
        System.out.println("getSystemZoneId: " + CommonUtil.getSystemZoneId());
        System.out.println("getAvailableProcessors: " + CommonUtil.getAvailableProcessors());
        System.out.println("getUserName: " + CommonUtil.getUserName());
        System.out.println("getUserHome: " + CommonUtil.getUserHome());
        System.out.println("getUserDir: " + CommonUtil.getUserDir());
        System.out.println("getJavaTempDir: " + CommonUtil.getJavaTempDir());
        System.out.println("userHomePath: " + CommonUtil.userHomePath(".ateng"));
        System.out.println("userDirPath: " + CommonUtil.userDirPath("target"));
        System.out.println("tempDirPath: " + CommonUtil.tempDirPath("ateng"));
        System.out.println("getJavaVersion: " + CommonUtil.getJavaVersion());
        System.out.println("getJavaMajorVersion: " + CommonUtil.getJavaMajorVersion());
        System.out.println("isJavaVersionAtLeast 21: " + CommonUtil.isJavaVersionAtLeast(21));
        System.out.println("getJavaHome: " + CommonUtil.getJavaHome());
        System.out.println("getJvmName: " + CommonUtil.getJvmName());
        System.out.println("getJvmVersion: " + CommonUtil.getJvmVersion());
        System.out.println("getPid: " + CommonUtil.getPid());
        System.out.println("getProcessUser: " + CommonUtil.getProcessUser());
        System.out.println("getJvmUptime: " + CommonUtil.getJvmUptime());
        System.out.println("getJvmInputArguments: " + CommonUtil.getJvmInputArguments());
        System.out.println("formatMaxMemory: " + CommonUtil.formatMaxMemory());
        System.out.println("formatUsedMemory: " + CommonUtil.formatUsedMemory());
        System.out.println("getMemoryUsagePercent: " + CommonUtil.getMemoryUsagePercent());
        System.out.println("getMemoryInfoMap: " + CommonUtil.getMemoryInfoMap());
        System.out.println("getLocalHostName: " + CommonUtil.getLocalHostName());
        System.out.println("getLocalHostAddress: " + CommonUtil.getLocalHostAddress());
        System.out.println("getLocalIpList: " + CommonUtil.getLocalIpList());
        System.out.println("getFirstLocalIpv4: " + CommonUtil.getFirstLocalIpv4());
        System.out.println("getActiveProfile: " + CommonUtil.getActiveProfile());
        System.out.println("getActiveProfiles: " + CommonUtil.getActiveProfiles());
        System.out.println("isDevProfile: " + CommonUtil.isDevProfile());
        System.out.println("isProdProfile: " + CommonUtil.isProdProfile());
        System.out.println("getSystemInfoMap: " + CommonUtil.getSystemInfoMap());
        System.out.println("getRuntimeInfoMap: " + CommonUtil.getRuntimeInfoMap());
        System.out.println("getEnvironmentInfoJson: " + CommonUtil.getEnvironmentInfoJson());
        CommonUtil.clearSystemProperty("ateng.test.key");
    }

    private static void printTitle(String title) {
        System.out.println();
        System.out.println("==================== " + title + " ====================");
    }

    /**
     * Source bean for test.
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public static class UserSource {
        private Long id;
        private String name;
        private Integer age;
        private String email;

        public UserSource() {
        }

        public UserSource(Long id, String name, Integer age, String email) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "UserSource{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    /**
     * Target bean for test.
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public static class UserTarget {
        private Long id;
        private String name;
        private Integer age;
        private String email;

        public UserTarget() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "UserTarget{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    /**
     * User status enum for test.
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public enum UserStatus {
        ENABLE(1, "enable"),
        DISABLE(0, "disable");

        private final Integer code;
        private final String desc;

        UserStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
