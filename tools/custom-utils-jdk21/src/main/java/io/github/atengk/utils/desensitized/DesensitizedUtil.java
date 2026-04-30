package io.github.atengk.utils.desensitized;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 数据脱敏工具类，提供常见个人信息、联系方式、账号凭证、金融支付、网络设备、文本、日志、对象字段等脱敏能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class DesensitizedUtil {

    private static final char DEFAULT_MASK_CHAR = '*';
    private static final String DEFAULT_MASK_TEXT = "***";
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])|(?<!\\d)\\d{15}(?!\\d)");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$");
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("(\\\"([^\\\"]+)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");
    private static final Pattern JSON_NUMBER_FIELD_PATTERN = Pattern.compile("(\\\"([^\\\"]+)\\\"\\s*:\\s*)(-?\\d+(?:\\.\\d+)?)");
    private static final ConcurrentMap<String, Function<String, String>> CUSTOM_RULES = new ConcurrentHashMap<>();

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "password", "pwd", "pass", "token", "access_token", "refresh_token", "secret", "appsecret",
            "clientsecret", "apikey", "api_key", "appkey", "app_key", "authorization", "cookie", "session",
            "sessionid", "mobile", "phone", "email", "idcard", "id_card", "bankcard", "bank_card", "cardno"
    );

    private DesensitizedUtil() {
        throw new UnsupportedOperationException("DesensitizedUtil 是静态工具类，不允许实例化");
    }

    /**
     * 脱敏类型枚举，用于按类型统一分派脱敏规则。
     */
    public enum DesensitizedType {
        NAME,
        CHINESE_NAME,
        REAL_NAME,
        ID_CARD,
        PASSPORT,
        DRIVER_LICENSE,
        OFFICER_CARD,
        BIRTH_DATE,
        AGE,
        GENDER,
        MOBILE,
        PHONE,
        FIXED_PHONE,
        EMAIL,
        QQ,
        WECHAT,
        TELEGRAM,
        WHATSAPP,
        CONTACT,
        EMERGENCY_CONTACT,
        ADDRESS,
        DETAIL_ADDRESS,
        PROVINCE_CITY_ADDRESS,
        GEO_LOCATION,
        LONGITUDE,
        LATITUDE,
        POSTCODE,
        IP_LOCATION,
        USERNAME,
        ACCOUNT,
        LOGIN_NAME,
        PASSWORD,
        SALT,
        TOKEN,
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        SECRET_KEY,
        API_KEY,
        APP_KEY,
        APP_SECRET,
        CLIENT_SECRET,
        AUTHORIZATION,
        COOKIE,
        SESSION_ID,
        BANK_CARD,
        CREDIT_CARD,
        DEBIT_CARD,
        CVV,
        BANK_ACCOUNT,
        PAY_ACCOUNT,
        ALIPAY_ACCOUNT,
        WECHAT_PAY_ACCOUNT,
        TRANSACTION_NO,
        ORDER_NO,
        INVOICE_NO,
        TAX_NO,
        AMOUNT,
        BALANCE,
        BUSINESS_NO,
        TRADE_NO,
        SERIAL_NO,
        CONTRACT_NO,
        TICKET_NO,
        CASE_NO,
        CUSTOMER_NO,
        MEMBER_NO,
        EMPLOYEE_NO,
        DEVICE_NO,
        LICENSE_NO,
        IP,
        IPV4,
        IPV6,
        MAC,
        IMEI,
        IMSI,
        ANDROID_ID,
        IDFA,
        OAID,
        UUID,
        USER_AGENT,
        DOMAIN,
        URL,
        URI,
        QUERY_STRING,
        COMPANY_NAME,
        UNIFIED_SOCIAL_CREDIT_CODE,
        TAXPAYER_NO,
        BUSINESS_LICENSE_NO,
        ORGANIZATION_CODE,
        LEGAL_PERSON_NAME,
        COMPANY_PHONE,
        COMPANY_ADDRESS,
        BANK_NAME,
        PUBLIC_BANK_ACCOUNT,
        TEXT,
        CONTENT,
        REMARK,
        MESSAGE,
        LOG_MESSAGE,
        JSON,
        DISPLAY,
        EXPORT,
        PRINT,
        REPORT,
        LIST_VIEW,
        DETAIL_VIEW,
        PREVIEW
    }

    /**
     * 通用脱敏，保留左侧和右侧指定长度，中间使用默认掩码字符替换。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int keepLeft, int keepRight) {
        return mask(value, keepLeft, keepRight, DEFAULT_MASK_CHAR);
    }

    /**
     * 通用脱敏，保留左侧和右侧指定长度，中间使用指定掩码字符替换。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int keepLeft, int keepRight, char maskChar) {
        checkNonNegative(keepLeft, "keepLeft");
        checkNonNegative(keepRight, "keepRight");
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.length();
        if (keepLeft + keepRight >= length) {
            return value;
        }
        return value.substring(0, keepLeft) + repeat(maskChar, length - keepLeft - keepRight) + value.substring(length - keepRight);
    }

    /**
     * 从左侧开始脱敏指定长度。
     *
     * @param value 原始字符串
     * @param length 脱敏长度
     * @return 脱敏后的字符串
     */
    public static String maskLeft(String value, int length) {
        return maskLeft(value, length, DEFAULT_MASK_CHAR);
    }

    /**
     * 从左侧开始使用指定掩码字符脱敏指定长度。
     *
     * @param value 原始字符串
     * @param length 脱敏长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String maskLeft(String value, int length, char maskChar) {
        checkNonNegative(length, "length");
        return maskRange(value, 0, length, maskChar);
    }

    /**
     * 从右侧开始脱敏指定长度。
     *
     * @param value 原始字符串
     * @param length 脱敏长度
     * @return 脱敏后的字符串
     */
    public static String maskRight(String value, int length) {
        return maskRight(value, length, DEFAULT_MASK_CHAR);
    }

    /**
     * 从右侧开始使用指定掩码字符脱敏指定长度。
     *
     * @param value 原始字符串
     * @param length 脱敏长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String maskRight(String value, int length, char maskChar) {
        checkNonNegative(length, "length");
        if (value == null || value.isEmpty()) {
            return value;
        }
        int start = Math.max(0, value.length() - length);
        return maskRange(value, start, value.length(), maskChar);
    }

    /**
     * 脱敏字符串中间部分，默认保留首尾各 1 位。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String maskMiddle(String value) {
        return maskMiddle(value, DEFAULT_MASK_CHAR);
    }

    /**
     * 使用指定掩码字符脱敏字符串中间部分，默认保留首尾各 1 位。
     *
     * @param value 原始字符串
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String maskMiddle(String value, char maskChar) {
        return mask(value, 1, 1, maskChar);
    }

    /**
     * 按指定区间脱敏，起始下标包含，结束下标不包含。
     *
     * @param value 原始字符串
     * @param startInclusive 起始下标，包含
     * @param endExclusive 结束下标，不包含
     * @return 脱敏后的字符串
     */
    public static String maskRange(String value, int startInclusive, int endExclusive) {
        return maskRange(value, startInclusive, endExclusive, DEFAULT_MASK_CHAR);
    }

    /**
     * 按指定区间使用指定掩码字符脱敏，起始下标包含，结束下标不包含。
     *
     * @param value 原始字符串
     * @param startInclusive 起始下标，包含
     * @param endExclusive 结束下标，不包含
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String maskRange(String value, int startInclusive, int endExclusive, char maskChar) {
        checkRange(startInclusive, endExclusive);
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.length();
        if (startInclusive >= length || startInclusive == endExclusive) {
            return value;
        }
        int end = Math.min(endExclusive, length);
        return value.substring(0, startInclusive) + repeat(maskChar, end - startInclusive) + value.substring(end);
    }

    /**
     * 保留左侧指定长度，其余部分脱敏。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @return 脱敏后的字符串
     */
    public static String keepLeft(String value, int keepLeft) {
        return keepLeft(value, keepLeft, DEFAULT_MASK_CHAR);
    }

    /**
     * 保留左侧指定长度，其余部分使用指定掩码字符脱敏。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String keepLeft(String value, int keepLeft, char maskChar) {
        return mask(value, keepLeft, 0, maskChar);
    }

    /**
     * 保留右侧指定长度，其余部分脱敏。
     *
     * @param value 原始字符串
     * @param keepRight 右侧保留长度
     * @return 脱敏后的字符串
     */
    public static String keepRight(String value, int keepRight) {
        return keepRight(value, keepRight, DEFAULT_MASK_CHAR);
    }

    /**
     * 保留右侧指定长度，其余部分使用指定掩码字符脱敏。
     *
     * @param value 原始字符串
     * @param keepRight 右侧保留长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String keepRight(String value, int keepRight, char maskChar) {
        return mask(value, 0, keepRight, maskChar);
    }

    /**
     * 保留左侧和右侧指定长度，中间部分脱敏。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @return 脱敏后的字符串
     */
    public static String keepAround(String value, int keepLeft, int keepRight) {
        return mask(value, keepLeft, keepRight);
    }

    /**
     * 返回固定长度的默认掩码字符串。
     *
     * @param value 原始字符串
     * @param fixedLength 固定掩码长度
     * @return 固定长度脱敏结果
     */
    public static String fixedMask(String value, int fixedLength) {
        return fixedMask(value, fixedLength, DEFAULT_MASK_CHAR);
    }

    /**
     * 返回固定长度的指定掩码字符串。
     *
     * @param value 原始字符串
     * @param fixedLength 固定掩码长度
     * @param maskChar 掩码字符
     * @return 固定长度脱敏结果
     */
    public static String fixedMask(String value, int fixedLength, char maskChar) {
        checkNonNegative(fixedLength, "fixedLength");
        if (value == null) {
            return null;
        }
        return repeat(maskChar, fixedLength);
    }

    /**
     * 将字符串全部替换为默认掩码字符，长度保持不变。
     *
     * @param value 原始字符串
     * @return 全量脱敏后的字符串
     */
    public static String fullMask(String value) {
        return fullMask(value, DEFAULT_MASK_CHAR);
    }

    /**
     * 将字符串全部替换为指定掩码字符，长度保持不变。
     *
     * @param value 原始字符串
     * @param maskChar 掩码字符
     * @return 全量脱敏后的字符串
     */
    public static String fullMask(String value, char maskChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return repeat(maskChar, value.length());
    }

    /**
     * 当字符串为空或空白时返回默认脱敏值，否则返回原始字符串。
     *
     * @param value 原始字符串
     * @param defaultMask 默认脱敏值
     * @return 处理后的字符串
     */
    public static String emptyToDefaultMask(String value, String defaultMask) {
        return isBlank(value) ? defaultIfNull(defaultMask) : value;
    }

    /**
     * 使用自定义左右保留长度和掩码字符进行脱敏。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String customMask(String value, int keepLeft, int keepRight, char maskChar) {
        return mask(value, keepLeft, keepRight, maskChar);
    }

    /**
     * 中文姓名脱敏。
     *
     * @param value 中文姓名
     * @return 脱敏后的中文姓名
     */
    public static String chineseName(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() == 1) {
            return DEFAULT_MASK_TEXT;
        }
        if (text.length() == 2) {
            return text.charAt(0) + "*";
        }
        return text.charAt(0) + repeat(DEFAULT_MASK_CHAR, text.length() - 2) + text.substring(text.length() - 1);
    }

    /**
     * 通用姓名脱敏，兼容中文姓名和英文姓名。
     *
     * @param value 姓名
     * @return 脱敏后的姓名
     */
    public static String name(String value) {
        if (isBlank(value)) {
            return value;
        }
        if (containsChinese(value)) {
            return chineseName(value);
        }
        return mask(value.trim(), 1, 1);
    }

    /**
     * 实名信息脱敏。
     *
     * @param value 实名信息
     * @return 脱敏后的实名信息
     */
    public static String realName(String value) {
        return name(value);
    }

    /**
     * 身份证号脱敏。
     *
     * @param value 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() >= 15) {
            return mask(text, 3, 4);
        }
        return mask(text, 1, Math.min(1, text.length()));
    }

    /**
     * 护照号脱敏。
     *
     * @param value 护照号
     * @return 脱敏后的护照号
     */
    public static String passport(String value) {
        return certificate(value);
    }

    /**
     * 驾驶证号脱敏。
     *
     * @param value 驾驶证号
     * @return 脱敏后的驾驶证号
     */
    public static String driverLicense(String value) {
        return certificate(value);
    }

    /**
     * 军官证、警官证等证件号脱敏。
     *
     * @param value 证件号
     * @return 脱敏后的证件号
     */
    public static String officerCard(String value) {
        return certificate(value);
    }

    /**
     * 出生日期脱敏。
     *
     * @param value 出生日期
     * @return 脱敏后的出生日期
     */
    public static String birthDate(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
            char separator = text.contains("/") ? '/' : '-';
            return text.substring(0, 4) + separator + "**" + separator + "**";
        }
        if (text.matches("\\d{8}")) {
            return text.substring(0, 4) + "****";
        }
        return mask(text, 2, 0);
    }

    /**
     * 年龄脱敏。
     *
     * @param value 年龄
     * @return 脱敏后的年龄
     */
    public static String age(String value) {
        return isBlank(value) ? value : DEFAULT_MASK_TEXT;
    }

    /**
     * 性别脱敏。
     *
     * @param value 性别
     * @return 脱敏后的性别
     */
    public static String gender(String value) {
        return isBlank(value) ? value : "*";
    }

    /**
     * 手机号脱敏。
     *
     * @param value 手机号
     * @return 脱敏后的手机号
     */
    public static String mobile(String value) {
        if (isBlank(value)) {
            return value;
        }
        return mask(value.trim(), 3, 4);
    }

    /**
     * 通用电话号码脱敏。
     *
     * @param value 电话号码
     * @return 脱敏后的电话号码
     */
    public static String phone(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.matches("1[3-9]\\d{9}")) {
            return mobile(text);
        }
        return mask(text, Math.min(3, text.length()), Math.min(2, Math.max(0, text.length() - 3)));
    }

    /**
     * 固定电话脱敏。
     *
     * @param value 固定电话号码
     * @return 脱敏后的固定电话
     */
    public static String fixedPhone(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int index = text.indexOf('-');
        if (index > 0 && index < text.length() - 1) {
            return text.substring(0, index + 1) + mask(text.substring(index + 1), 0, 2);
        }
        return phone(text);
    }

    /**
     * 邮箱地址脱敏。
     *
     * @param value 邮箱地址
     * @return 脱敏后的邮箱地址
     */
    public static String email(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int atIndex = text.indexOf('@');
        if (atIndex <= 0 || atIndex == text.length() - 1) {
            return mask(text, 1, Math.min(1, text.length()));
        }
        String local = text.substring(0, atIndex);
        String domain = text.substring(atIndex);
        if (local.length() == 1) {
            return "*" + domain;
        }
        return mask(local, 1, 1) + domain;
    }

    /**
     * QQ 号脱敏。
     *
     * @param value QQ 号
     * @return 脱敏后的 QQ 号
     */
    public static String qq(String value) {
        return account(value);
    }

    /**
     * 微信号脱敏。
     *
     * @param value 微信号
     * @return 脱敏后的微信号
     */
    public static String wechat(String value) {
        return account(value);
    }

    /**
     * Telegram 账号脱敏。
     *
     * @param value Telegram 账号
     * @return 脱敏后的 Telegram 账号
     */
    public static String telegram(String value) {
        return account(value);
    }

    /**
     * WhatsApp 账号脱敏。
     *
     * @param value WhatsApp 账号
     * @return 脱敏后的 WhatsApp 账号
     */
    public static String whatsapp(String value) {
        return account(value);
    }

    /**
     * 通用联系方式脱敏，会优先识别手机号和邮箱。
     *
     * @param value 联系方式
     * @return 脱敏后的联系方式
     */
    public static String contact(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (MOBILE_PATTERN.matcher(text).matches()) {
            return mobile(text);
        }
        if (EMAIL_PATTERN.matcher(text).matches()) {
            return email(text);
        }
        return phone(text);
    }

    /**
     * 紧急联系人信息脱敏。
     *
     * @param value 紧急联系人信息
     * @return 脱敏后的紧急联系人信息
     */
    public static String emergencyContact(String value) {
        return contact(value);
    }

    /**
     * 通用地址脱敏。
     *
     * @param value 地址
     * @return 脱敏后的地址
     */
    public static String address(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() <= 6) {
            return maskMiddle(text);
        }
        return text.substring(0, 6) + DEFAULT_MASK_TEXT;
    }

    /**
     * 详细地址脱敏。
     *
     * @param value 详细地址
     * @return 脱敏后的详细地址
     */
    public static String detailAddress(String value) {
        return address(value);
    }

    /**
     * 保留省市区信息，隐藏详细地址。
     *
     * @param value 地址
     * @return 脱敏后的省市地址
     */
    public static String provinceCityAddress(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int index = maxIndexOf(text, '省', '市', '区', '县');
        if (index >= 0 && index < text.length() - 1) {
            return text.substring(0, index + 1) + DEFAULT_MASK_TEXT;
        }
        return address(text);
    }

    /**
     * 经纬度坐标脱敏。
     *
     * @param value 经纬度坐标
     * @return 脱敏后的经纬度坐标
     */
    public static String geoLocation(String value) {
        if (isBlank(value)) {
            return value;
        }
        String[] parts = value.split(",", -1);
        if (parts.length == 2) {
            return longitude(parts[0].trim()) + "," + latitude(parts[1].trim());
        }
        return mask(value, Math.min(3, value.length()), 0);
    }

    /**
     * 经度脱敏。
     *
     * @param value 经度
     * @return 脱敏后的经度
     */
    public static String longitude(String value) {
        return coordinate(value);
    }

    /**
     * 纬度脱敏。
     *
     * @param value 纬度
     * @return 脱敏后的纬度
     */
    public static String latitude(String value) {
        return coordinate(value);
    }

    /**
     * 邮政编码脱敏。
     *
     * @param value 邮政编码
     * @return 脱敏后的邮政编码
     */
    public static String postcode(String value) {
        if (isBlank(value)) {
            return value;
        }
        return mask(value.trim(), Math.min(2, value.trim().length()), 0);
    }

    /**
     * IP 归属地信息脱敏。
     *
     * @param value IP 归属地信息
     * @return 脱敏后的归属地信息
     */
    public static String ipLocation(String value) {
        return address(value);
    }

    /**
     * 用户名脱敏。
     *
     * @param value 用户名
     * @return 脱敏后的用户名
     */
    public static String username(String value) {
        return account(value);
    }

    /**
     * 通用账号脱敏。
     *
     * @param value 账号
     * @return 脱敏后的账号
     */
    public static String account(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() <= 2) {
            return fullMask(text);
        }
        return mask(text, 1, 1);
    }

    /**
     * 登录名脱敏。
     *
     * @param value 登录名
     * @return 脱敏后的登录名
     */
    public static String loginName(String value) {
        return account(value);
    }

    /**
     * 密码脱敏。
     *
     * @param value 密码
     * @return 脱敏后的密码
     */
    public static String password(String value) {
        return isBlank(value) ? value : DEFAULT_MASK_TEXT;
    }

    /**
     * 密码盐脱敏。
     *
     * @param value 密码盐
     * @return 脱敏后的密码盐
     */
    public static String salt(String value) {
        return password(value);
    }

    /**
     * Token 脱敏。
     *
     * @param value Token
     * @return 脱敏后的 Token
     */
    public static String token(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(4, text.length()), Math.min(4, Math.max(0, text.length() - 4)));
    }

    /**
     * Access Token 脱敏。
     *
     * @param value Access Token
     * @return 脱敏后的 Access Token
     */
    public static String accessToken(String value) {
        return token(value);
    }

    /**
     * Refresh Token 脱敏。
     *
     * @param value Refresh Token
     * @return 脱敏后的 Refresh Token
     */
    public static String refreshToken(String value) {
        return token(value);
    }

    /**
     * 密钥脱敏。
     *
     * @param value 密钥
     * @return 脱敏后的密钥
     */
    public static String secretKey(String value) {
        return token(value);
    }

    /**
     * API Key 脱敏。
     *
     * @param value API Key
     * @return 脱敏后的 API Key
     */
    public static String apiKey(String value) {
        return token(value);
    }

    /**
     * App Key 脱敏。
     *
     * @param value App Key
     * @return 脱敏后的 App Key
     */
    public static String appKey(String value) {
        return token(value);
    }

    /**
     * App Secret 脱敏。
     *
     * @param value App Secret
     * @return 脱敏后的 App Secret
     */
    public static String appSecret(String value) {
        return token(value);
    }

    /**
     * OAuth Client Secret 脱敏。
     *
     * @param value Client Secret
     * @return 脱敏后的 Client Secret
     */
    public static String clientSecret(String value) {
        return token(value);
    }

    /**
     * Authorization 请求头脱敏。
     *
     * @param value Authorization 请求头值
     * @return 脱敏后的 Authorization 请求头值
     */
    public static String authorization(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int index = text.indexOf(' ');
        if (index > 0 && index < text.length() - 1) {
            return text.substring(0, index + 1) + token(text.substring(index + 1));
        }
        return token(text);
    }

    /**
     * Cookie 字符串脱敏。
     *
     * @param value Cookie 字符串
     * @return 脱敏后的 Cookie 字符串
     */
    public static String cookie(String value) {
        if (isBlank(value)) {
            return value;
        }
        String[] items = value.split(";", -1);
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                builder.append(';');
            }
            String item = items[i];
            int index = item.indexOf('=');
            if (index < 0) {
                builder.append(item.trim());
            } else {
                builder.append(item, 0, index + 1).append(DEFAULT_MASK_TEXT);
            }
        }
        return builder.toString();
    }

    /**
     * Session ID 脱敏。
     *
     * @param value Session ID
     * @return 脱敏后的 Session ID
     */
    public static String sessionId(String value) {
        return token(value);
    }

    /**
     * 银行卡号脱敏。
     *
     * @param value 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String bankCard(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(4, text.length()), Math.min(4, Math.max(0, text.length() - 4)));
    }

    /**
     * 信用卡号脱敏。
     *
     * @param value 信用卡号
     * @return 脱敏后的信用卡号
     */
    public static String creditCard(String value) {
        return bankCard(value);
    }

    /**
     * 借记卡号脱敏。
     *
     * @param value 借记卡号
     * @return 脱敏后的借记卡号
     */
    public static String debitCard(String value) {
        return bankCard(value);
    }

    /**
     * CVV 脱敏。
     *
     * @param value CVV
     * @return 脱敏后的 CVV
     */
    public static String cvv(String value) {
        return isBlank(value) ? value : DEFAULT_MASK_TEXT;
    }

    /**
     * 银行账户脱敏。
     *
     * @param value 银行账户
     * @return 脱敏后的银行账户
     */
    public static String bankAccount(String value) {
        return bankCard(value);
    }

    /**
     * 支付账号脱敏。
     *
     * @param value 支付账号
     * @return 脱敏后的支付账号
     */
    public static String payAccount(String value) {
        return contact(value);
    }

    /**
     * 支付宝账号脱敏。
     *
     * @param value 支付宝账号
     * @return 脱敏后的支付宝账号
     */
    public static String alipayAccount(String value) {
        return payAccount(value);
    }

    /**
     * 微信支付账号脱敏。
     *
     * @param value 微信支付账号
     * @return 脱敏后的微信支付账号
     */
    public static String wechatPayAccount(String value) {
        return payAccount(value);
    }

    /**
     * 交易流水号脱敏。
     *
     * @param value 交易流水号
     * @return 脱敏后的交易流水号
     */
    public static String transactionNo(String value) {
        return businessNo(value);
    }

    /**
     * 订单号脱敏。
     *
     * @param value 订单号
     * @return 脱敏后的订单号
     */
    public static String orderNo(String value) {
        return businessNo(value);
    }

    /**
     * 发票号码脱敏。
     *
     * @param value 发票号码
     * @return 脱敏后的发票号码
     */
    public static String invoiceNo(String value) {
        return businessNo(value);
    }

    /**
     * 税号脱敏。
     *
     * @param value 税号
     * @return 脱敏后的税号
     */
    public static String taxNo(String value) {
        return businessNo(value);
    }

    /**
     * 金额脱敏。
     *
     * @param value 金额
     * @return 脱敏后的金额
     */
    public static String amount(String value) {
        return isBlank(value) ? value : DEFAULT_MASK_TEXT;
    }

    /**
     * 余额脱敏。
     *
     * @param value 余额
     * @return 脱敏后的余额
     */
    public static String balance(String value) {
        return amount(value);
    }

    /**
     * 通用业务编号脱敏。
     *
     * @param value 业务编号
     * @return 脱敏后的业务编号
     */
    public static String businessNo(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(4, text.length()), Math.min(4, Math.max(0, text.length() - 4)));
    }

    /**
     * 交易号脱敏。
     *
     * @param value 交易号
     * @return 脱敏后的交易号
     */
    public static String tradeNo(String value) {
        return businessNo(value);
    }

    /**
     * 序列号脱敏。
     *
     * @param value 序列号
     * @return 脱敏后的序列号
     */
    public static String serialNo(String value) {
        return businessNo(value);
    }

    /**
     * 合同编号脱敏。
     *
     * @param value 合同编号
     * @return 脱敏后的合同编号
     */
    public static String contractNo(String value) {
        return businessNo(value);
    }

    /**
     * 工单编号脱敏。
     *
     * @param value 工单编号
     * @return 脱敏后的工单编号
     */
    public static String ticketNo(String value) {
        return businessNo(value);
    }

    /**
     * 案件编号脱敏。
     *
     * @param value 案件编号
     * @return 脱敏后的案件编号
     */
    public static String caseNo(String value) {
        return businessNo(value);
    }

    /**
     * 客户编号脱敏。
     *
     * @param value 客户编号
     * @return 脱敏后的客户编号
     */
    public static String customerNo(String value) {
        return businessNo(value);
    }

    /**
     * 会员编号脱敏。
     *
     * @param value 会员编号
     * @return 脱敏后的会员编号
     */
    public static String memberNo(String value) {
        return businessNo(value);
    }

    /**
     * 员工编号脱敏。
     *
     * @param value 员工编号
     * @return 脱敏后的员工编号
     */
    public static String employeeNo(String value) {
        return businessNo(value);
    }

    /**
     * 设备编号脱敏。
     *
     * @param value 设备编号
     * @return 脱敏后的设备编号
     */
    public static String deviceNo(String value) {
        return businessNo(value);
    }

    /**
     * 许可证编号脱敏。
     *
     * @param value 许可证编号
     * @return 脱敏后的许可证编号
     */
    public static String licenseNo(String value) {
        return businessNo(value);
    }

    /**
     * 通用 IP 地址脱敏。
     *
     * @param value IP 地址
     * @return 脱敏后的 IP 地址
     */
    public static String ip(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return text.contains(":") ? ipv6(text) : ipv4(text);
    }

    /**
     * IPv4 地址脱敏。
     *
     * @param value IPv4 地址
     * @return 脱敏后的 IPv4 地址
     */
    public static String ipv4(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (!IPV4_PATTERN.matcher(text).matches()) {
            return mask(text, Math.min(3, text.length()), 0);
        }
        String[] parts = text.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
    }

    /**
     * IPv6 地址脱敏。
     *
     * @param value IPv6 地址
     * @return 脱敏后的 IPv6 地址
     */
    public static String ipv6(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int first = text.indexOf(':');
        int last = text.lastIndexOf(':');
        if (first < 0 || first == last) {
            return mask(text, Math.min(4, text.length()), 0);
        }
        return text.substring(0, first) + ":****:" + text.substring(last + 1);
    }

    /**
     * MAC 地址脱敏。
     *
     * @param value MAC 地址
     * @return 脱敏后的 MAC 地址
     */
    public static String mac(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        String separator = text.contains("-") ? "-" : ":";
        String[] parts = text.split(Pattern.quote(separator));
        if (parts.length != 6) {
            return mask(text, Math.min(4, text.length()), 0);
        }
        return parts[0] + separator + parts[1] + separator + parts[2] + separator + "**" + separator + "**" + separator + "**";
    }

    /**
     * IMEI 脱敏。
     *
     * @param value IMEI
     * @return 脱敏后的 IMEI
     */
    public static String imei(String value) {
        return businessNo(value);
    }

    /**
     * IMSI 脱敏。
     *
     * @param value IMSI
     * @return 脱敏后的 IMSI
     */
    public static String imsi(String value) {
        return businessNo(value);
    }

    /**
     * 设备 ID 脱敏。
     *
     * @param value 设备 ID
     * @return 脱敏后的设备 ID
     */
    public static String deviceId(String value) {
        return businessNo(value);
    }

    /**
     * Android ID 脱敏。
     *
     * @param value Android ID
     * @return 脱敏后的 Android ID
     */
    public static String androidId(String value) {
        return businessNo(value);
    }

    /**
     * iOS IDFA 脱敏。
     *
     * @param value IDFA
     * @return 脱敏后的 IDFA
     */
    public static String idfa(String value) {
        return uuid(value);
    }

    /**
     * OAID 脱敏。
     *
     * @param value OAID
     * @return 脱敏后的 OAID
     */
    public static String oaid(String value) {
        return uuid(value);
    }

    /**
     * UUID 脱敏。
     *
     * @param value UUID
     * @return 脱敏后的 UUID
     */
    public static String uuid(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(8, text.length()), Math.min(4, Math.max(0, text.length() - 8)));
    }

    /**
     * User-Agent 脱敏。
     *
     * @param value User-Agent
     * @return 脱敏后的 User-Agent
     */
    public static String userAgent(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(10, text.length()), Math.min(5, Math.max(0, text.length() - 10)));
    }

    /**
     * 域名脱敏。
     *
     * @param value 域名
     * @return 脱敏后的域名
     */
    public static String domain(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int index = text.indexOf('.');
        if (index > 0) {
            return mask(text.substring(0, index), 1, 1) + text.substring(index);
        }
        return maskMiddle(text);
    }

    /**
     * URL 脱敏，默认处理查询参数中的敏感字段。
     *
     * @param value URL
     * @return 脱敏后的 URL
     */
    public static String url(String value) {
        if (isBlank(value)) {
            return value;
        }
        int queryStart = value.indexOf('?');
        if (queryStart < 0) {
            return value;
        }
        String prefix = value.substring(0, queryStart + 1);
        String queryAndFragment = value.substring(queryStart + 1);
        int fragmentStart = queryAndFragment.indexOf('#');
        if (fragmentStart >= 0) {
            return prefix + queryString(queryAndFragment.substring(0, fragmentStart)) + queryAndFragment.substring(fragmentStart);
        }
        return prefix + queryString(queryAndFragment);
    }

    /**
     * URI 脱敏，默认处理查询参数中的敏感字段。
     *
     * @param value URI
     * @return 脱敏后的 URI
     */
    public static String uri(String value) {
        return url(value);
    }

    /**
     * URL 查询参数脱敏。
     *
     * @param value 查询参数字符串
     * @return 脱敏后的查询参数字符串
     */
    public static String queryString(String value) {
        if (isBlank(value)) {
            return value;
        }
        String[] parts = value.split("&", -1);
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('&');
            }
            String part = parts[i];
            int index = part.indexOf('=');
            if (index < 0) {
                builder.append(part);
                continue;
            }
            String key = part.substring(0, index);
            String paramValue = part.substring(index + 1);
            builder.append(key).append('=');
            if (isSensitiveKey(key)) {
                builder.append(DEFAULT_MASK_TEXT);
            } else {
                builder.append(paramValue);
            }
        }
        return builder.toString();
    }

    /**
     * 企业名称脱敏。
     *
     * @param value 企业名称
     * @return 脱敏后的企业名称
     */
    public static String companyName(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() == 1) {
            return DEFAULT_MASK_TEXT;
        }
        if (text.length() == 2) {
            return text.charAt(0) + "*";
        }
        if (text.length() <= 4) {
            return mask(text, 1, 1);
        }
        return mask(text, 2, 2);
    }

    /**
     * 统一社会信用代码脱敏。
     *
     * @param value 统一社会信用代码
     * @return 脱敏后的统一社会信用代码
     */
    public static String unifiedSocialCreditCode(String value) {
        return businessNo(value);
    }

    /**
     * 纳税人识别号脱敏。
     *
     * @param value 纳税人识别号
     * @return 脱敏后的纳税人识别号
     */
    public static String taxpayerNo(String value) {
        return businessNo(value);
    }

    /**
     * 营业执照编号脱敏。
     *
     * @param value 营业执照编号
     * @return 脱敏后的营业执照编号
     */
    public static String businessLicenseNo(String value) {
        return businessNo(value);
    }

    /**
     * 组织机构代码脱敏。
     *
     * @param value 组织机构代码
     * @return 脱敏后的组织机构代码
     */
    public static String organizationCode(String value) {
        return businessNo(value);
    }

    /**
     * 法人姓名脱敏。
     *
     * @param value 法人姓名
     * @return 脱敏后的法人姓名
     */
    public static String legalPersonName(String value) {
        return name(value);
    }

    /**
     * 企业联系电话脱敏。
     *
     * @param value 企业联系电话
     * @return 脱敏后的企业联系电话
     */
    public static String companyPhone(String value) {
        return fixedPhone(value);
    }

    /**
     * 企业地址脱敏。
     *
     * @param value 企业地址
     * @return 脱敏后的企业地址
     */
    public static String companyAddress(String value) {
        return address(value);
    }

    /**
     * 开户行信息脱敏。
     *
     * @param value 开户行信息
     * @return 脱敏后的开户行信息
     */
    public static String bankName(String value) {
        return isBlank(value) ? value : mask(value.trim(), 2, 2);
    }

    /**
     * 对公银行账号脱敏。
     *
     * @param value 对公银行账号
     * @return 脱敏后的对公银行账号
     */
    public static String publicBankAccount(String value) {
        return bankAccount(value);
    }

    /**
     * 通用文本脱敏，会识别并脱敏文本中的手机号、邮箱、身份证号和银行卡号。
     *
     * @param value 文本内容
     * @return 脱敏后的文本内容
     */
    public static String text(String value) {
        return autoText(value);
    }

    /**
     * 内容脱敏。
     *
     * @param value 内容
     * @return 脱敏后的内容
     */
    public static String content(String value) {
        return text(value);
    }

    /**
     * 备注脱敏。
     *
     * @param value 备注
     * @return 脱敏后的备注
     */
    public static String remark(String value) {
        return text(value);
    }

    /**
     * 消息内容脱敏。
     *
     * @param value 消息内容
     * @return 脱敏后的消息内容
     */
    public static String message(String value) {
        return text(value);
    }

    /**
     * 日志内容脱敏。
     *
     * @param value 日志内容
     * @return 脱敏后的日志内容
     */
    public static String logMessage(String value) {
        return text(value);
    }

    /**
     * 使用敏感词集合替换文本内容。
     *
     * @param value 文本内容
     * @param sensitiveWords 敏感词集合
     * @return 替换后的文本内容
     */
    public static String replaceSensitiveWords(String value, Collection<String> sensitiveWords) {
        return replaceByKeywords(value, sensitiveWords);
    }

    /**
     * 按正则表达式替换匹配内容。
     *
     * @param value 文本内容
     * @param regex 正则表达式
     * @param replacement 替换内容
     * @return 替换后的文本内容
     * @throws PatternSyntaxException 正则表达式非法时抛出
     */
    public static String replaceByRegex(String value, String regex, String replacement) {
        if (value == null || isBlank(regex)) {
            return value;
        }
        return Pattern.compile(regex).matcher(value).replaceAll(defaultIfNull(replacement));
    }

    /**
     * 按关键词集合替换文本内容。
     *
     * @param value 文本内容
     * @param keywords 关键词集合
     * @return 替换后的文本内容
     */
    public static String replaceByKeywords(String value, Collection<String> keywords) {
        if (value == null || keywords == null || keywords.isEmpty()) {
            return value;
        }
        String result = value;
        for (String keyword : keywords) {
            if (!isBlank(keyword)) {
                result = result.replace(keyword, DEFAULT_MASK_TEXT);
            }
        }
        return result;
    }

    /**
     * 对正则匹配到的内容按指定保留规则脱敏。
     *
     * @param value 文本内容
     * @param regex 正则表达式
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @return 脱敏后的文本内容
     */
    public static String maskMatched(String value, String regex, int keepLeft, int keepRight) {
        checkNonNegative(keepLeft, "keepLeft");
        checkNonNegative(keepRight, "keepRight");
        if (value == null || isBlank(regex)) {
            return value;
        }
        Matcher matcher = Pattern.compile(regex).matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(mask(matcher.group(), keepLeft, keepRight)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * 脱敏文本中的手机号。
     *
     * @param value 文本内容
     * @return 脱敏后的文本内容
     */
    public static String maskMobileInText(String value) {
        return maskMatchedByPattern(value, MOBILE_PATTERN, DesensitizedUtil::mobile);
    }

    /**
     * 脱敏文本中的邮箱地址。
     *
     * @param value 文本内容
     * @return 脱敏后的文本内容
     */
    public static String maskEmailInText(String value) {
        return maskMatchedByPattern(value, EMAIL_PATTERN, DesensitizedUtil::email);
    }

    /**
     * 脱敏文本中的身份证号。
     *
     * @param value 文本内容
     * @return 脱敏后的文本内容
     */
    public static String maskIdCardInText(String value) {
        return maskMatchedByPattern(value, ID_CARD_PATTERN, DesensitizedUtil::idCard);
    }

    /**
     * 脱敏文本中的银行卡号。
     *
     * @param value 文本内容
     * @return 脱敏后的文本内容
     */
    public static String maskBankCardInText(String value) {
        return maskMatchedByPattern(value, BANK_CARD_PATTERN, DesensitizedUtil::bankCard);
    }

    /**
     * 对 Map 中的字段进行脱敏，优先使用指定规则，未指定时按字段名自动推断。
     *
     * @param source 原始 Map
     * @param rules 字段规则映射
     * @return 脱敏后的新 Map
     */
    public static Map<String, Object> map(Map<String, ?> source, Map<String, DesensitizedType> rules) {
        if (source == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            DesensitizedType type = rules == null ? null : rules.get(key);
            result.put(key, type == null ? field(key, value) : byType(toString(value), type));
        }
        return result;
    }

    /**
     * 批量脱敏 Map 集合。
     *
     * @param sources 原始 Map 集合
     * @param rules 字段规则映射
     * @return 脱敏后的 Map 集合
     */
    public static List<Map<String, Object>> maps(Collection<? extends Map<String, ?>> sources, Map<String, DesensitizedType> rules) {
        if (sources == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(sources.size());
        for (Map<String, ?> source : sources) {
            result.add(map(source, rules));
        }
        return result;
    }

    /**
     * 对 Java Bean 中的字符串字段进行脱敏，优先使用指定规则，未指定时按字段名自动推断。
     *
     * @param bean 原始 Bean
     * @param rules 字段规则映射
     * @param <T> Bean 类型
     * @return 脱敏后的原 Bean 实例
     */
    public static <T> T bean(T bean, Map<String, DesensitizedType> rules) {
        if (bean == null) {
            return null;
        }
        for (Field field : allFields(bean.getClass())) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.getType() != String.class) {
                continue;
            }
            try {
                if (!field.trySetAccessible()) {
                    continue;
                }
                Object raw = field.get(bean);
                DesensitizedType type = rules == null ? null : rules.get(field.getName());
                String masked = type == null ? field(field.getName(), raw) : byType(toString(raw), type);
                field.set(bean, masked);
            } catch (IllegalAccessException ignored) {
                // 无法访问的字段跳过，避免工具方法影响主流程。
            }
        }
        return bean;
    }

    /**
     * 批量脱敏 Java Bean 集合。
     *
     * @param beans 原始 Bean 集合
     * @param rules 字段规则映射
     * @param <T> Bean 类型
     * @return 脱敏后的 Bean 集合
     */
    public static <T> List<T> beans(Collection<T> beans, Map<String, DesensitizedType> rules) {
        if (beans == null) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(beans.size());
        for (T item : beans) {
            result.add(bean(item, rules));
        }
        return result;
    }

    /**
     * 根据字段名和值自动脱敏单个字段。
     *
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @return 脱敏后的字段值
     */
    public static String field(String fieldName, Object fieldValue) {
        return byFieldName(toString(fieldValue), fieldName);
    }

    /**
     * 根据字段名自动脱敏多个字段。
     *
     * @param source 原始字段 Map
     * @return 脱敏后的新 Map
     */
    public static Map<String, Object> fields(Map<String, ?> source) {
        return map(source, null);
    }

    /**
     * JSON 字符串脱敏，会按字段名自动推断脱敏规则。
     *
     * @param value JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String json(String value) {
        return json(value, null);
    }

    /**
     * JSON 字符串脱敏，优先使用指定字段规则，未指定时按字段名自动推断。
     *
     * @param value JSON 字符串
     * @param rules 字段规则映射
     * @return 脱敏后的 JSON 字符串
     */
    public static String json(String value, Map<String, DesensitizedType> rules) {
        if (value == null) {
            return null;
        }
        String result = replaceJsonStringFields(value, rules);
        return replaceJsonNumberFields(result, rules);
    }

    /**
     * JSON 对象字符串脱敏。
     *
     * @param value JSON 对象字符串
     * @return 脱敏后的 JSON 对象字符串
     */
    public static String jsonObject(String value) {
        return json(value);
    }

    /**
     * JSON 对象字符串脱敏，优先使用指定字段规则。
     *
     * @param value JSON 对象字符串
     * @param rules 字段规则映射
     * @return 脱敏后的 JSON 对象字符串
     */
    public static String jsonObject(String value, Map<String, DesensitizedType> rules) {
        return json(value, rules);
    }

    /**
     * JSON 数组字符串脱敏。
     *
     * @param value JSON 数组字符串
     * @return 脱敏后的 JSON 数组字符串
     */
    public static String jsonArray(String value) {
        return json(value);
    }

    /**
     * JSON 数组字符串脱敏，优先使用指定字段规则。
     *
     * @param value JSON 数组字符串
     * @param rules 字段规则映射
     * @return 脱敏后的 JSON 数组字符串
     */
    public static String jsonArray(String value, Map<String, DesensitizedType> rules) {
        return json(value, rules);
    }

    /**
     * 自动识别单个值并脱敏，优先识别手机号、邮箱、身份证号、银行卡号、IP 和 URL。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String auto(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (MOBILE_PATTERN.matcher(text).matches()) {
            return mobile(text);
        }
        if (EMAIL_PATTERN.matcher(text).matches()) {
            return email(text);
        }
        if (ID_CARD_PATTERN.matcher(text).matches()) {
            return idCard(text);
        }
        if (BANK_CARD_PATTERN.matcher(text).matches()) {
            return bankCard(text);
        }
        if (text.contains("?") && text.contains("=")) {
            return url(text);
        }
        if (IPV4_PATTERN.matcher(text).matches() || looksLikeIpv6(text)) {
            return ip(text);
        }
        return text;
    }

    /**
     * 自动识别文本中的敏感信息并脱敏。
     *
     * @param value 原始文本
     * @return 脱敏后的文本
     */
    public static String autoText(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        result = maskEmailInText(result);
        result = maskMobileInText(result);
        result = maskIdCardInText(result);
        result = maskBankCardInText(result);
        return result;
    }

    /**
     * 根据脱敏类型进行脱敏。
     *
     * @param value 原始字符串
     * @param type 脱敏类型
     * @return 脱敏后的字符串
     */
    public static String byType(String value, DesensitizedType type) {
        Objects.requireNonNull(type, "type 不能为空");
        return switch (type) {
            case NAME -> name(value);
            case CHINESE_NAME -> chineseName(value);
            case REAL_NAME -> realName(value);
            case ID_CARD -> idCard(value);
            case PASSPORT -> passport(value);
            case DRIVER_LICENSE -> driverLicense(value);
            case OFFICER_CARD -> officerCard(value);
            case BIRTH_DATE -> birthDate(value);
            case AGE -> age(value);
            case GENDER -> gender(value);
            case MOBILE -> mobile(value);
            case PHONE -> phone(value);
            case FIXED_PHONE -> fixedPhone(value);
            case EMAIL -> email(value);
            case QQ -> qq(value);
            case WECHAT -> wechat(value);
            case TELEGRAM -> telegram(value);
            case WHATSAPP -> whatsapp(value);
            case CONTACT -> contact(value);
            case EMERGENCY_CONTACT -> emergencyContact(value);
            case ADDRESS -> address(value);
            case DETAIL_ADDRESS -> detailAddress(value);
            case PROVINCE_CITY_ADDRESS -> provinceCityAddress(value);
            case GEO_LOCATION -> geoLocation(value);
            case LONGITUDE -> longitude(value);
            case LATITUDE -> latitude(value);
            case POSTCODE -> postcode(value);
            case IP_LOCATION -> ipLocation(value);
            case USERNAME -> username(value);
            case ACCOUNT -> account(value);
            case LOGIN_NAME -> loginName(value);
            case PASSWORD -> password(value);
            case SALT -> salt(value);
            case TOKEN -> token(value);
            case ACCESS_TOKEN -> accessToken(value);
            case REFRESH_TOKEN -> refreshToken(value);
            case SECRET_KEY -> secretKey(value);
            case API_KEY -> apiKey(value);
            case APP_KEY -> appKey(value);
            case APP_SECRET -> appSecret(value);
            case CLIENT_SECRET -> clientSecret(value);
            case AUTHORIZATION -> authorization(value);
            case COOKIE -> cookie(value);
            case SESSION_ID -> sessionId(value);
            case BANK_CARD -> bankCard(value);
            case CREDIT_CARD -> creditCard(value);
            case DEBIT_CARD -> debitCard(value);
            case CVV -> cvv(value);
            case BANK_ACCOUNT -> bankAccount(value);
            case PAY_ACCOUNT -> payAccount(value);
            case ALIPAY_ACCOUNT -> alipayAccount(value);
            case WECHAT_PAY_ACCOUNT -> wechatPayAccount(value);
            case TRANSACTION_NO -> transactionNo(value);
            case ORDER_NO -> orderNo(value);
            case INVOICE_NO -> invoiceNo(value);
            case TAX_NO -> taxNo(value);
            case AMOUNT -> amount(value);
            case BALANCE -> balance(value);
            case BUSINESS_NO -> businessNo(value);
            case TRADE_NO -> tradeNo(value);
            case SERIAL_NO -> serialNo(value);
            case CONTRACT_NO -> contractNo(value);
            case TICKET_NO -> ticketNo(value);
            case CASE_NO -> caseNo(value);
            case CUSTOMER_NO -> customerNo(value);
            case MEMBER_NO -> memberNo(value);
            case EMPLOYEE_NO -> employeeNo(value);
            case DEVICE_NO -> deviceNo(value);
            case LICENSE_NO -> licenseNo(value);
            case IP -> ip(value);
            case IPV4 -> ipv4(value);
            case IPV6 -> ipv6(value);
            case MAC -> mac(value);
            case IMEI -> imei(value);
            case IMSI -> imsi(value);
            case ANDROID_ID -> androidId(value);
            case IDFA -> idfa(value);
            case OAID -> oaid(value);
            case UUID -> uuid(value);
            case USER_AGENT -> userAgent(value);
            case DOMAIN -> domain(value);
            case URL -> url(value);
            case URI -> uri(value);
            case QUERY_STRING -> queryString(value);
            case COMPANY_NAME -> companyName(value);
            case UNIFIED_SOCIAL_CREDIT_CODE -> unifiedSocialCreditCode(value);
            case TAXPAYER_NO -> taxpayerNo(value);
            case BUSINESS_LICENSE_NO -> businessLicenseNo(value);
            case ORGANIZATION_CODE -> organizationCode(value);
            case LEGAL_PERSON_NAME -> legalPersonName(value);
            case COMPANY_PHONE -> companyPhone(value);
            case COMPANY_ADDRESS -> companyAddress(value);
            case BANK_NAME -> bankName(value);
            case PUBLIC_BANK_ACCOUNT -> publicBankAccount(value);
            case TEXT -> text(value);
            case CONTENT -> content(value);
            case REMARK -> remark(value);
            case MESSAGE -> message(value);
            case LOG_MESSAGE -> logMessage(value);
            case JSON -> json(value);
            case DISPLAY -> display(value);
            case EXPORT -> export(value);
            case PRINT -> print(value);
            case REPORT -> report(value);
            case LIST_VIEW -> listView(value);
            case DETAIL_VIEW -> detailView(value);
            case PREVIEW -> preview(value);
        };
    }

    /**
     * 根据字段名自动选择脱敏规则。
     *
     * @param value 字段值
     * @param fieldName 字段名
     * @return 脱敏后的字段值
     */
    public static String byFieldName(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (isBlank(fieldName)) {
            return auto(value);
        }
        String normalized = normalizeFieldName(fieldName);
        if (containsAny(normalized, "mobile", "cellphone", "phonenumber", "telephone", "tel")) {
            return mobile(value);
        }
        if (containsAny(normalized, "email", "mail")) {
            return email(value);
        }
        if (containsAny(normalized, "idcard", "identitycard", "certno")) {
            return idCard(value);
        }
        if (containsAny(normalized, "realname", "username", "nickname", "name")) {
            return name(value);
        }
        if (containsAny(normalized, "password", "passwd", "pwd")) {
            return password(value);
        }
        if (containsAny(normalized, "authorization")) {
            return authorization(value);
        }
        if (containsAny(normalized, "accesstoken")) {
            return accessToken(value);
        }
        if (containsAny(normalized, "refreshtoken")) {
            return refreshToken(value);
        }
        if (containsAny(normalized, "token", "sessionid")) {
            return token(value);
        }
        if (containsAny(normalized, "secret", "apikey", "appkey")) {
            return secretKey(value);
        }
        if (containsAny(normalized, "bankcard", "cardno", "bankaccount", "accountno")) {
            return bankCard(value);
        }
        if (containsAny(normalized, "address", "addr")) {
            return address(value);
        }
        if (containsAny(normalized, "ipv4", "ipv6", "ipaddress", "clientip", "remoteip")) {
            return ip(value);
        }
        if (containsAny(normalized, "mac")) {
            return mac(value);
        }
        if (containsAny(normalized, "url", "uri")) {
            return url(value);
        }
        if (containsAny(normalized, "amount", "balance")) {
            return amount(value);
        }
        if (containsAny(normalized, "orderno", "tradeno", "serialno", "businessno")) {
            return businessNo(value);
        }
        return auto(value);
    }

    /**
     * 根据左右保留规则进行脱敏。
     *
     * @param value 原始字符串
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @param maskChar 掩码字符
     * @return 脱敏后的字符串
     */
    public static String byRule(String value, int keepLeft, int keepRight, char maskChar) {
        return mask(value, keepLeft, keepRight, maskChar);
    }

    /**
     * 根据已注册的规则名称进行脱敏。
     *
     * @param value 原始字符串
     * @param ruleName 规则名称
     * @return 脱敏后的字符串
     */
    public static String byRule(String value, String ruleName) {
        if (isBlank(ruleName)) {
            throw new IllegalArgumentException("ruleName 不能为空");
        }
        Function<String, String> handler = CUSTOM_RULES.get(ruleName);
        if (handler == null) {
            throw new IllegalArgumentException("未找到脱敏规则：" + ruleName);
        }
        return handler.apply(value);
    }

    /**
     * 根据正则表达式进行脱敏。
     *
     * @param value 原始字符串
     * @param regex 正则表达式
     * @param keepLeft 左侧保留长度
     * @param keepRight 右侧保留长度
     * @return 脱敏后的字符串
     */
    public static String byRegex(String value, String regex, int keepLeft, int keepRight) {
        return maskMatched(value, regex, keepLeft, keepRight);
    }

    /**
     * 根据自定义策略函数进行脱敏。
     *
     * @param value 原始字符串
     * @param strategy 脱敏策略函数
     * @return 脱敏后的字符串
     */
    public static String byStrategy(String value, Function<String, String> strategy) {
        Objects.requireNonNull(strategy, "strategy 不能为空");
        return strategy.apply(value);
    }

    /**
     * 注册自定义脱敏规则。
     *
     * @param ruleName 规则名称
     * @param handler 脱敏处理函数
     */
    public static void registerRule(String ruleName, Function<String, String> handler) {
        if (isBlank(ruleName)) {
            throw new IllegalArgumentException("ruleName 不能为空");
        }
        Objects.requireNonNull(handler, "handler 不能为空");
        CUSTOM_RULES.put(ruleName, handler);
    }

    /**
     * 移除自定义脱敏规则。
     *
     * @param ruleName 规则名称
     * @return 是否移除成功
     */
    public static boolean removeRule(String ruleName) {
        if (isBlank(ruleName)) {
            return false;
        }
        return CUSTOM_RULES.remove(ruleName) != null;
    }

    /**
     * 获取自定义脱敏规则。
     *
     * @param ruleName 规则名称
     * @return 脱敏处理函数，未找到时返回 null
     */
    public static Function<String, String> getRule(String ruleName) {
        if (isBlank(ruleName)) {
            return null;
        }
        return CUSTOM_RULES.get(ruleName);
    }

    /**
     * 日志内容脱敏。
     *
     * @param value 日志内容
     * @return 脱敏后的日志内容
     */
    public static String log(String value) {
        return autoText(value);
    }

    /**
     * 请求日志脱敏。
     *
     * @param value 请求日志
     * @return 脱敏后的请求日志
     */
    public static String requestLog(String value) {
        return log(value);
    }

    /**
     * 响应日志脱敏。
     *
     * @param value 响应日志
     * @return 脱敏后的响应日志
     */
    public static String responseLog(String value) {
        return log(value);
    }

    /**
     * 异常信息脱敏。
     *
     * @param value 异常信息
     * @return 脱敏后的异常信息
     */
    public static String exceptionMessage(String value) {
        return log(value);
    }

    /**
     * 异常栈信息脱敏。
     *
     * @param value 异常栈信息
     * @return 脱敏后的异常栈信息
     */
    public static String stackTrace(String value) {
        return log(value);
    }

    /**
     * 请求头字段脱敏。
     *
     * @param headerName 请求头名称
     * @param headerValue 请求头值
     * @return 脱敏后的请求头值
     */
    public static String header(String headerName, String headerValue) {
        return byFieldName(headerValue, headerName);
    }

    /**
     * 请求头集合脱敏。
     *
     * @param headers 请求头集合
     * @return 脱敏后的请求头集合
     */
    public static Map<String, Object> headers(Map<String, ?> headers) {
        return fields(headers);
    }

    /**
     * 请求参数字段脱敏。
     *
     * @param paramName 参数名
     * @param paramValue 参数值
     * @return 脱敏后的参数值
     */
    public static String param(String paramName, Object paramValue) {
        return field(paramName, paramValue);
    }

    /**
     * 请求参数集合脱敏。
     *
     * @param params 请求参数集合
     * @return 脱敏后的请求参数集合
     */
    public static Map<String, Object> params(Map<String, ?> params) {
        return fields(params);
    }

    /**
     * 请求体或响应体脱敏。
     *
     * @param value 请求体或响应体
     * @return 脱敏后的内容
     */
    public static String body(String value) {
        return autoText(value);
    }

    /**
     * 页面展示场景脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String display(String value) {
        return auto(value);
    }

    /**
     * 导出场景脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String export(String value) {
        return auto(value);
    }

    /**
     * 打印场景脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String print(String value) {
        return auto(value);
    }

    /**
     * 报表场景脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String report(String value) {
        return auto(value);
    }

    /**
     * 列表页展示脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String listView(String value) {
        return auto(value);
    }

    /**
     * 详情页展示脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String detailView(String value) {
        return auto(value);
    }

    /**
     * 预览场景脱敏。
     *
     * @param value 原始字符串
     * @return 脱敏后的字符串
     */
    public static String preview(String value) {
        return auto(value);
    }

    private static boolean looksLikeIpv6(String value) {
        return value != null && value.indexOf(':') > 0 && value.indexOf('/') < 0 && value.chars().filter(ch -> ch == ':').count() >= 2;
    }

    private static String certificate(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        return mask(text, Math.min(2, text.length()), Math.min(2, Math.max(0, text.length() - 2)));
    }

    private static String coordinate(String value) {
        if (isBlank(value)) {
            return value;
        }
        String text = value.trim();
        int dotIndex = text.indexOf('.');
        if (dotIndex > 0 && text.length() > dotIndex + 3) {
            return text.substring(0, dotIndex + 3) + DEFAULT_MASK_TEXT;
        }
        return mask(text, Math.min(2, text.length()), 0);
    }

    private static String maskMatchedByPattern(String value, Pattern pattern, Function<String, String> masker) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        StringBuilder builder = new StringBuilder(value.length());
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(masker.apply(matcher.group())));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String replaceJsonStringFields(String value, Map<String, DesensitizedType> rules) {
        Matcher matcher = JSON_STRING_FIELD_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder(value.length());
        while (matcher.find()) {
            String fieldName = matcher.group(2);
            String fieldValue = matcher.group(3);
            String masked = maskByRuleOrField(fieldName, fieldValue, rules);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(1) + masked + matcher.group(4)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String replaceJsonNumberFields(String value, Map<String, DesensitizedType> rules) {
        Matcher matcher = JSON_NUMBER_FIELD_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder(value.length());
        while (matcher.find()) {
            String fieldName = matcher.group(2);
            String fieldValue = matcher.group(3);
            if (shouldMaskField(fieldName, rules)) {
                String masked = maskByRuleOrField(fieldName, fieldValue, rules);
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(1) + '"' + masked + '"'));
            }
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static boolean shouldMaskField(String fieldName, Map<String, DesensitizedType> rules) {
        return rules != null && rules.containsKey(fieldName) || isAutoSensitiveField(fieldName);
    }

    private static boolean isAutoSensitiveField(String fieldName) {
        String normalized = normalizeFieldName(fieldName);
        return containsAny(normalized, "mobile", "phone", "email", "idcard", "password", "token", "secret", "bankcard", "cardno", "address", "amount", "balance");
    }

    private static String maskByRuleOrField(String fieldName, String value, Map<String, DesensitizedType> rules) {
        DesensitizedType type = rules == null ? null : rules.get(fieldName);
        return type == null ? byFieldName(value, fieldName) : byType(value, type);
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("-", "_");
        if (SENSITIVE_QUERY_KEYS.contains(normalized)) {
            return true;
        }
        String compact = normalized.replace("_", "");
        return SENSITIVE_QUERY_KEYS.contains(compact)
                || compact.contains("token")
                || compact.contains("password")
                || compact.contains("secret")
                || compact.contains("mobile")
                || compact.contains("phone")
                || compact.contains("email")
                || compact.contains("idcard")
                || compact.contains("bankcard");
    }

    private static int maxIndexOf(String text, char... chars) {
        int max = -1;
        for (char ch : chars) {
            max = Math.max(max, text.indexOf(ch));
        }
        return max;
    }

    private static void checkNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能小于 0");
        }
    }

    private static void checkRange(int startInclusive, int endExclusive) {
        if (startInclusive < 0) {
            throw new IllegalArgumentException("startInclusive 不能小于 0");
        }
        if (endExclusive < 0) {
            throw new IllegalArgumentException("endExclusive 不能小于 0");
        }
        if (startInclusive > endExclusive) {
            throw new IllegalArgumentException("startInclusive 不能大于 endExclusive");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String defaultIfNull(String value) {
        return value == null ? DEFAULT_MASK_TEXT : value;
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(ch).repeat(count);
    }
}
