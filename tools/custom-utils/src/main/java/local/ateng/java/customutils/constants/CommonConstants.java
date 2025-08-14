package local.ateng.java.customutils.constants;

/**
 * CommonConstants
 * <p>项目常用的全局静态常量类</p>
 *
 * @author 孔余
 * @since 2025-08-09
 */
public final class CommonConstants {

    /**
     * 私有构造，防止实例化
     */
    private CommonConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /* ==============================
       常用的编码
       ============================== */

    /**
     * UTF-8 编码
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * UTF-16 编码
     */
    public static final String UTF_16 = "UTF-16";

    /**
     * UTF-32 编码
     */
    public static final String UTF_32 = "UTF-32";

    /**
     * GBK 编码（中文 Windows 常用）
     */
    public static final String GBK = "GBK";

    /**
     * GB2312 编码（简体中文早期标准）
     */
    public static final String GB2312 = "GB2312";

    /**
     * ISO-8859-1 编码（Latin-1，西欧字符集）
     */
    public static final String ISO_8859_1 = "ISO-8859-1";

    /**
     * US-ASCII 编码（基本拉丁字符）
     */
    public static final String US_ASCII = "US-ASCII";

    /**
     * Big5 编码（繁体中文常用，台湾/香港）
     */
    public static final String BIG5 = "Big5";

    /**
     * Windows-1252 编码（西欧 Windows 默认）
     */
    public static final String WINDOWS_1252 = "windows-1252";

    /**
     * Shift_JIS 编码（日本常用）
     */
    public static final String SHIFT_JIS = "Shift_JIS";

    /**
     * EUC-KR 编码（韩文常用）
     */
    public static final String EUC_KR = "EUC-KR";

        /* ==============================
       时间格式化常量
       ============================== */

    /**
     * 默认日期格式（年月日）
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 默认日期时间格式（年月日 时分秒）
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间戳格式（年月日 时分秒 毫秒）
     */
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 年月格式（yyyy-MM）
     */
    public static final String YEAR_MONTH_FORMAT = "yyyy-MM";

    /**
     * 年月日格式（yyyyMMdd）
     */
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";

    /**
     * 年月日时分秒格式（yyyyMMddHHmmss）
     */
    public static final String DATETIME_FORMAT_COMPACT = "yyyyMMddHHmmss";

    /**
     * 年月日时分秒毫秒格式（yyyyMMddHHmmssSSS）
     */
    public static final String TIMESTAMP_FORMAT_COMPACT = "yyyyMMddHHmmssSSS";

    /**
     * 仅时间格式（HH:mm:ss）
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 仅时间格式（HHmmss）
     */
    public static final String TIME_FORMAT_COMPACT = "HHmmss";

    /**
     * 日期时间格式（yyyy/MM/dd HH:mm:ss）
     */
    public static final String DATETIME_FORMAT_SLASH = "yyyy/MM/dd HH:mm:ss";

    /**
     * 日期格式（yyyy/MM/dd）
     */
    public static final String DATE_FORMAT_SLASH = "yyyy/MM/dd";

    /**
     * 美式日期格式（MM-dd-yyyy）
     */
    public static final String DATE_FORMAT_US = "MM-dd-yyyy";

    /**
     * 美式日期时间格式（MM-dd-yyyy HH:mm:ss）
     */
    public static final String DATETIME_FORMAT_US = "MM-dd-yyyy HH:mm:ss";

    /**
     * 英式日期格式（dd-MM-yyyy）
     */
    public static final String DATE_FORMAT_UK = "dd-MM-yyyy";

    /**
     * 英式日期时间格式（dd-MM-yyyy HH:mm:ss）
     */
    public static final String DATETIME_FORMAT_UK = "dd-MM-yyyy HH:mm:ss";

    /**
     * ISO 日期格式（yyyy-MM-dd'T'HH:mm:ss'Z'）
     */
    public static final String DATETIME_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * ISO 日期时间格式（yyyy-MM-dd'T'HH:mm:ss.SSS'Z'）
     */
    public static final String TIMESTAMP_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * 中国标准时间格式（EEE MMM dd HH:mm:ss zzz yyyy，Locale.CHINA）
     */
    public static final String CST_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    /**
     * 中文日期格式（yyyy年MM月dd日）
     */
    public static final String DATE_FORMAT_CN = "yyyy年MM月dd日";

    /**
     * 中文日期时间格式（yyyy年MM月dd日 HH时mm分ss秒）
     */
    public static final String DATETIME_FORMAT_CN = "yyyy年MM月dd日 HH时mm分ss秒";

    /**
     * 中文日期时间格式（yyyy年MM月dd日 HH:mm:ss）
     */
    public static final String DATETIME_FORMAT_CN_NORMAL = "yyyy年MM月dd日 HH:mm:ss";

    /**
     * 中文年月格式（yyyy年MM月）
     */
    public static final String YEAR_MONTH_FORMAT_CN = "yyyy年MM月";

    /**
     * 中文月日格式（MM月dd日）
     */
    public static final String MONTH_DAY_FORMAT_CN = "MM月dd日";

    /**
     * 中文时间格式（HH时mm分ss秒）
     */
    public static final String TIME_FORMAT_CN = "HH时mm分ss秒";

    /**
     * 中文时间格式（HH时mm分）
     */
    public static final String TIME_FORMAT_CN_HM = "HH时mm分";

    /**
     * 中文星期格式（EEEE）
     */
    public static final String WEEK_FORMAT_CN = "EEEE";

    /**
     * 中文日期+星期格式（yyyy年MM月dd日 EEEE）
     */
    public static final String DATE_WEEK_FORMAT_CN = "yyyy年MM月dd日 EEEE";

    /**
     * 日志文件时间格式（yyyyMMdd_HHmmss）
     */
    public static final String LOG_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";

    /**
     * 文件导出常用时间格式（yyyy-MM-dd_HH-mm-ss）
     */
    public static final String FILE_EXPORT_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    /**
     * 短日期时间格式（MM-dd HH:mm）
     */
    public static final String SHORT_DATETIME_FORMAT = "MM-dd HH:mm";

    /**
     * 短日期时间格式（MM月dd日 HH:mm）
     */
    public static final String SHORT_DATETIME_FORMAT_CN = "MM月dd日 HH:mm";

        /* ==============================
       常用字符与符号（包含英文、中文及常见转义符）
       ============================== */

    /**
     * 空格（space）
     */
    public static final String SPACE = " ";

    /**
     * 空字符串
     */
    public static final String EMPTY = "";

    /**
     * 英文逗号（,）
     */
    public static final String COMMA = ",";

    /**
     * 英文句点（.）
     */
    public static final String DOT = ".";

    /**
     * 英文冒号（:）
     */
    public static final String COLON = ":";

    /**
     * 英文分号（;）
     */
    public static final String SEMICOLON = ";";

    /**
     * 正斜杠（/）
     */
    public static final String SLASH = "/";

    /**
     * 反斜杠（\）
     */
    public static final String BACKSLASH = "\\";

    /**
     * 下划线（_）
     */
    public static final String UNDERLINE = "_";

    /**
     * 连字符（-）
     */
    public static final String DASH = "-";

    /**
     * 竖线（|）
     */
    public static final String PIPE = "|";

    /**
     * 艾特符号（@）
     */
    public static final String AT = "@";

    /**
     * 井号（#）
     */
    public static final String HASH = "#";

    /**
     * 美元符号（$）
     */
    public static final String DOLLAR = "$";

    /**
     * 问号（?）
     */
    public static final String QUESTION_MARK = "?";

    /**
     * 等号（=）
     */
    public static final String EQUAL_SIGN = "=";

    /**
     * 与号（&）
     */
    public static final String AND = "&";

    /**
     * 百分号（%）
     */
    public static final String PERCENT = "%";

    /**
     * 星号（*）
     */
    public static final String ASTERISK = "*";

    /**
     * 加号（+）
     */
    public static final String PLUS = "+";

    /**
     * 波浪号（~）
     */
    public static final String TILDE = "~";

    /**
     * 脱字符（^）
     */
    public static final String CARET = "^";

    /**
     * 左圆括号（(）
     */
    public static final String LEFT_PAREN = "(";

    /**
     * 右圆括号（)）
     */
    public static final String RIGHT_PAREN = ")";

    /**
     * 左中括号（[）
     */
    public static final String LEFT_BRACKET = "[";

    /**
     * 右中括号（]）
     */
    public static final String RIGHT_BRACKET = "]";

    /**
     * 左花括号（{）
     */
    public static final String LEFT_BRACE = "{";

    /**
     * 右花括号（}）
     */
    public static final String RIGHT_BRACE = "}";

    /**
     * 小于号（<）
     */
    public static final String LESS_THAN = "<";

    /**
     * 大于号（>）
     */
    public static final String GREATER_THAN = ">";

    /**
     * 中文逗号（，）
     */
    public static final String COMMA_CN = "，";

    /**
     * 中文句号（。）
     */
    public static final String DOT_CN = "。";

    /**
     * 中文冒号（：）
     */
    public static final String COLON_CN = "：";

    /**
     * 中文分号（；）
     */
    public static final String SEMICOLON_CN = "；";

    /**
     * 中文问号（？）
     */
    public static final String QUESTION_MARK_CN = "？";

    /**
     * 中文感叹号（！）
     */
    public static final String EXCLAMATION_MARK_CN = "！";

    /**
     * 中文引号（“）
     */
    public static final String QUOTE_LEFT_CN = "“";

    /**
     * 中文引号（”）
     */
    public static final String QUOTE_RIGHT_CN = "”";

    /**
     * 中文单引号（‘）
     */
    public static final String SINGLE_QUOTE_LEFT_CN = "‘";

    /**
     * 中文单引号（’）
     */
    public static final String SINGLE_QUOTE_RIGHT_CN = "’";

    /**
     * 英文双引号（"）
     */
    public static final String DOUBLE_QUOTE = "\"";

    /**
     * 英文单引号（'）
     */
    public static final String SINGLE_QUOTE = "'";

    /**
     * 换行符（\n）
     */
    public static final String NEWLINE = "\n";

    /**
     * 回车符（\r）
     */
    public static final String CARRIAGE_RETURN = "\r";

    /**
     * 制表符（\t）
     */
    public static final String TAB = "\t";

    /* ==============================
       空值与占位常量
       ============================== */

    /**
     * 空字符串
     */
    public static final String EMPTY_STRING = "";

    /**
     * 换行符（Unix/Linux）
     */
    public static final String LF = "\n";

    /**
     * 回车符（Windows）
     */
    public static final String CRLF = "\r\n";

    /**
     * 字符串 "null"（注意与 null 引用区分）
     */
    public static final String NULL_STRING = "null";

    /**
     * 空 JSON 对象字符串
     */
    public static final String EMPTY_JSON_OBJECT = "{}";

    /**
     * 空 JSON 数组字符串
     */
    public static final String EMPTY_JSON_ARRAY = "[]";

    /**
     * 空 Map 描述字符串
     */
    public static final String EMPTY_MAP_STRING = "{}";

    /**
     * 占位符：连字符 "-"
     */
    public static final String PLACEHOLDER_DASH = "-";

    /**
     * 占位符：下划线 "_"
     */
    public static final String PLACEHOLDER_UNDERSCORE = "_";

    /**
     * 占位符：默认未知值 "UNKNOWN"
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * 占位符：默认未设置值 "UNSET"
     */
    public static final String UNSET = "UNSET";

        /* ==============================
       常用正则表达式（包含常见输入校验规则）
       ============================== */

    /**
     * 邮箱格式校验（支持字母、数字、下划线、加号、点号等）
     */
    public static final String REGEX_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    /**
     * 手机号（中国大陆，支持 13-19 开头）
     */
    public static final String REGEX_MOBILE = "^1[3-9]\\d{9}$";

    /**
     * 固定电话（支持区号 3-4 位，可带或不带 - 符号）
     */
    public static final String REGEX_PHONE = "^(\\d{3,4}-)?\\d{7,8}$";

    /**
     * 身份证（15位或18位，最后一位可为数字或 X/x）
     */
    public static final String REGEX_ID_CARD = "(^\\d{15}$)|(^\\d{17}([0-9Xx])$)";

    /**
     * IP 地址（IPv4）
     */
    public static final String REGEX_IPV4 =
            "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\."
                    + "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\."
                    + "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\."
                    + "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";

    /**
     * URL 校验（支持 http/https，端口可选，路径可选）
     */
    public static final String REGEX_URL =
            "^(http|https)://[\\w.-]+(:\\d+)?(/.*)?$";

    /**
     * 用户名（4-16位字母、数字、下划线）
     */
    public static final String REGEX_USERNAME = "^[a-zA-Z0-9_]{4,16}$";

    /**
     * 中国车牌号（新能源 + 普通车牌）
     */
    public static final String REGEX_CAR_PLATE =
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]" +
                    "([A-HJ-NP-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳]|[A-HJ-NP-Z][A-HJ-NP-Z0-9]{5}[DF])$";

    /**
     * 中国邮政编码（6位数字）
     */
    public static final String REGEX_POSTCODE = "^[1-9]\\d{5}$";

    /**
     * 金额（支持两位小数，可为正负数）
     */
    public static final String REGEX_MONEY = "^-?\\d+(\\.\\d{1,2})?$";

    /**
     * 中文字符（一个或多个）
     */
    public static final String REGEX_CHINESE = "^[\\u4e00-\\u9fa5]+$";

    /**
     * 英文字符（一个或多个）
     */
    public static final String REGEX_ENGLISH = "^[A-Za-z]+$";

    /**
     * 英文 + 数字（一个或多个）
     */
    public static final String REGEX_ALPHANUMERIC = "^[A-Za-z0-9]+$";

    /**
     * 强密码（8-20 位，必须包含字母、数字、特殊符号）
     */
    public static final String REGEX_STRONG_PASSWORD =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()_+`\\-={}\\[\\]:\";'<>?,./]).{8,20}$";

    /**
     * 日期（yyyy-MM-dd，考虑闰年）
     */
    public static final String REGEX_DATE =
            "^(?:(?:19|20)\\d{2})-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|"
                    + "(?:0[13-9]|1[0-2])-(?:29|30)|"
                    + "(?:0[13578]|1[02])-31|02-29(?:(?:19|20)(?:0[48]|[2468][048]|[13579][26])"
                    + "|2000))$";

    /**
     * 时间（HH:mm:ss）
     */
    public static final String REGEX_TIME = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";

    /**
     * 日期时间（yyyy-MM-dd HH:mm:ss）
     */
    public static final String REGEX_DATETIME =
            "^(?:(?:19|20)\\d{2})-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|"
                    + "(?:0[13-9]|1[0-2])-(?:29|30)|"
                    + "(?:0[13578]|1[02])-31|02-29(?:(?:19|20)(?:0[48]|[2468][048]|[13579][26])"
                    + "|2000)) ([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";

    /**
     * Base64 编码（标准 + URL 安全）
     */
    public static final String REGEX_BASE64 =
            "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";

    /**
     * MAC 地址（支持大小写字母）
     */
    public static final String REGEX_MAC =
            "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    /**
     * 正则表达式：匹配字符串开头的一个或多个斜杠（/ 或 \）
     */
    public static final String REGEX_START_SLASHES = "^[\\\\/]+";

    /* ==============================
       业务通用状态码（可结合枚举使用）
       ============================== */

    /**
     * 通用：成功
     */
    public static final int SUCCESS = 200;

    /**
     * 通用：失败
     */
    public static final int FAIL = 500;

    /**
     * 参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 权限不足
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器内部错误
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * 请求超时
     */
    public static final int REQUEST_TIMEOUT = 408;

    /**
     * 业务自定义状态码：数据已存在
     */
    public static final int DATA_EXISTS = 1001;

    /**
     * 业务自定义状态码：数据不存在
     */
    public static final int DATA_NOT_FOUND = 1002;

    /**
     * 业务自定义状态码：操作过于频繁
     */
    public static final int TOO_MANY_REQUESTS = 1003;

     /* ==============================
       数字常量（系统化生成）
       ============================== */

    /**
     * 1 到 99
     */
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int SIX = 6;
    public static final int SEVEN = 7;
    public static final int EIGHT = 8;
    public static final int NINE = 9;
    public static final int TEN = 10;
    public static final int ELEVEN = 11;
    public static final int TWELVE = 12;
    public static final int THIRTEEN = 13;
    public static final int FOURTEEN = 14;
    public static final int FIFTEEN = 15;
    public static final int SIXTEEN = 16;
    public static final int SEVENTEEN = 17;
    public static final int EIGHTEEN = 18;
    public static final int NINETEEN = 19;
    public static final int TWENTY = 20;
    public static final int TWENTY_ONE = 21;
    public static final int TWENTY_TWO = 22;
    public static final int TWENTY_THREE = 23;
    public static final int TWENTY_FOUR = 24;
    public static final int TWENTY_FIVE = 25;
    public static final int TWENTY_SIX = 26;
    public static final int TWENTY_SEVEN = 27;
    public static final int TWENTY_EIGHT = 28;
    public static final int TWENTY_NINE = 29;
    public static final int THIRTY = 30;
    public static final int THIRTY_ONE = 31;
    public static final int THIRTY_TWO = 32;
    public static final int THIRTY_THREE = 33;
    public static final int THIRTY_FOUR = 34;
    public static final int THIRTY_FIVE = 35;
    public static final int THIRTY_SIX = 36;
    public static final int THIRTY_SEVEN = 37;
    public static final int THIRTY_EIGHT = 38;
    public static final int THIRTY_NINE = 39;
    public static final int FORTY = 40;
    public static final int FORTY_ONE = 41;
    public static final int FORTY_TWO = 42;
    public static final int FORTY_THREE = 43;
    public static final int FORTY_FOUR = 44;
    public static final int FORTY_FIVE = 45;
    public static final int FORTY_SIX = 46;
    public static final int FORTY_SEVEN = 47;
    public static final int FORTY_EIGHT = 48;
    public static final int FORTY_NINE = 49;
    public static final int FIFTY = 50;
    public static final int FIFTY_ONE = 51;
    public static final int FIFTY_TWO = 52;
    public static final int FIFTY_THREE = 53;
    public static final int FIFTY_FOUR = 54;
    public static final int FIFTY_FIVE = 55;
    public static final int FIFTY_SIX = 56;
    public static final int FIFTY_SEVEN = 57;
    public static final int FIFTY_EIGHT = 58;
    public static final int FIFTY_NINE = 59;
    public static final int SIXTY = 60;
    public static final int SIXTY_ONE = 61;
    public static final int SIXTY_TWO = 62;
    public static final int SIXTY_THREE = 63;
    public static final int SIXTY_FOUR = 64;
    public static final int SIXTY_FIVE = 65;
    public static final int SIXTY_SIX = 66;
    public static final int SIXTY_SEVEN = 67;
    public static final int SIXTY_EIGHT = 68;
    public static final int SIXTY_NINE = 69;
    public static final int SEVENTY = 70;
    public static final int SEVENTY_ONE = 71;
    public static final int SEVENTY_TWO = 72;
    public static final int SEVENTY_THREE = 73;
    public static final int SEVENTY_FOUR = 74;
    public static final int SEVENTY_FIVE = 75;
    public static final int SEVENTY_SIX = 76;
    public static final int SEVENTY_SEVEN = 77;
    public static final int SEVENTY_EIGHT = 78;
    public static final int SEVENTY_NINE = 79;
    public static final int EIGHTY = 80;
    public static final int EIGHTY_ONE = 81;
    public static final int EIGHTY_TWO = 82;
    public static final int EIGHTY_THREE = 83;
    public static final int EIGHTY_FOUR = 84;
    public static final int EIGHTY_FIVE = 85;
    public static final int EIGHTY_SIX = 86;
    public static final int EIGHTY_SEVEN = 87;
    public static final int EIGHTY_EIGHT = 88;
    public static final int EIGHTY_NINE = 89;
    public static final int NINETY = 90;
    public static final int NINETY_ONE = 91;
    public static final int NINETY_TWO = 92;
    public static final int NINETY_THREE = 93;
    public static final int NINETY_FOUR = 94;
    public static final int NINETY_FIVE = 95;
    public static final int NINETY_SIX = 96;
    public static final int NINETY_SEVEN = 97;
    public static final int NINETY_EIGHT = 98;
    public static final int NINETY_NINE = 99;

    /**
     * 100 到 1000
     */
    public static final int ONE_HUNDRED = 100;
    public static final int TWO_HUNDRED = 200;
    public static final int THREE_HUNDRED = 300;
    public static final int FOUR_HUNDRED = 400;
    public static final int FIVE_HUNDRED = 500;
    public static final int SIX_HUNDRED = 600;
    public static final int SEVEN_HUNDRED = 700;
    public static final int EIGHT_HUNDRED = 800;
    public static final int NINE_HUNDRED = 900;

    /**
     * 1000 到 10000
     */
    public static final int ONE_THOUSAND = 1000;
    public static final int TWO_THOUSAND = 2000;
    public static final int THREE_THOUSAND = 3000;
    public static final int FOUR_THOUSAND = 4000;
    public static final int FIVE_THOUSAND = 5000;
    public static final int SIX_THOUSAND = 6000;
    public static final int SEVEN_THOUSAND = 7000;
    public static final int EIGHT_THOUSAND = 8000;
    public static final int NINE_THOUSAND = 9000;
    public static final int TEN_THOUSAND = 10000;

    /**
     * 整数常量：负一
     */
    public static final int NEGATIVE_ONE = -1;

    /**
     * 整数常量：负二
     */
    public static final int NEGATIVE_TWO = -2;

    /**
     * 整数常量：负三
     */
    public static final int NEGATIVE_THREE = -3;

    /**
     * 整数常量：负四
     */
    public static final int NEGATIVE_FOUR = -4;

    /**
     * 整数常量：负五
     */
    public static final int NEGATIVE_FIVE = -5;

    /**
     * 整数常量：负六
     */
    public static final int NEGATIVE_SIX = -6;

    /**
     * 整数常量：负七
     */
    public static final int NEGATIVE_SEVEN = -7;

    /**
     * 整数常量：负八
     */
    public static final int NEGATIVE_EIGHT = -8;

    /**
     * 整数常量：负九
     */
    public static final int NEGATIVE_NINE = -9;

    /**
     * 整数常量：负十
     */
    public static final int NEGATIVE_TEN = -10;

    /* ==============================
       数字字符串常量（系统化生成）
       ============================== */

    /**
     * 数字 0（字符串）
     */
    public static final String STR_ZERO = "0";

    /**
     * 数字 1（字符串）
     */
    public static final String STR_ONE = "1";

    /**
     * 数字 2（字符串）
     */
    public static final String STR_TWO = "2";

    /**
     * 数字 3（字符串）
     */
    public static final String STR_THREE = "3";

    /**
     * 数字 4（字符串）
     */
    public static final String STR_FOUR = "4";

    /**
     * 数字 5（字符串）
     */
    public static final String STR_FIVE = "5";

    /**
     * 数字 6（字符串）
     */
    public static final String STR_SIX = "6";

    /**
     * 数字 7（字符串）
     */
    public static final String STR_SEVEN = "7";

    /**
     * 数字 8（字符串）
     */
    public static final String STR_EIGHT = "8";

    /**
     * 数字 9（字符串）
     */
    public static final String STR_NINE = "9";

    /**
     * 数字 10（字符串）
     */
    public static final String STR_TEN = "10";

    /**
     * 数字 11（字符串）
     */
    public static final String STR_ELEVEN = "11";

    /**
     * 数字 12（字符串）
     */
    public static final String STR_TWELVE = "12";

    /**
     * 数字 13（字符串）
     */
    public static final String STR_THIRTEEN = "13";

    /**
     * 数字 14（字符串）
     */
    public static final String STR_FOURTEEN = "14";

    /**
     * 数字 15（字符串）
     */
    public static final String STR_FIFTEEN = "15";

    /**
     * 数字 16（字符串）
     */
    public static final String STR_SIXTEEN = "16";

    /**
     * 数字 17（字符串）
     */
    public static final String STR_SEVENTEEN = "17";

    /**
     * 数字 18（字符串）
     */
    public static final String STR_EIGHTEEN = "18";

    /**
     * 数字 19（字符串）
     */
    public static final String STR_NINETEEN = "19";

    /**
     * 数字 20（字符串）
     */
    public static final String STR_TWENTY = "20";

    /**
     * 数字 -1（字符串）
     */
    public static final String STR_NEGATIVE_ONE = "-1";

    /**
     * 数字 -2（字符串）
     */
    public static final String STR_NEGATIVE_TWO = "-2";

    /**
     * 数字 -3（字符串）
     */
    public static final String STR_NEGATIVE_THREE = "-3";

    /**
     * 数字 -4（字符串）
     */
    public static final String STR_NEGATIVE_FOUR = "-4";

    /**
     * 数字 -5（字符串）
     */
    public static final String STR_NEGATIVE_FIVE = "-5";

    /**
     * 数字 -6（字符串）
     */
    public static final String STR_NEGATIVE_SIX = "-6";

    /**
     * 数字 -7（字符串）
     */
    public static final String STR_NEGATIVE_SEVEN = "-7";

    /**
     * 数字 -8（字符串）
     */
    public static final String STR_NEGATIVE_EIGHT = "-8";

    /**
     * 数字 -9（字符串）
     */
    public static final String STR_NEGATIVE_NINE = "-9";

    /**
     * 数字 -10（字符串）
     */
    public static final String STR_NEGATIVE_TEN = "-10";

    /* ==============================
       布尔常量
       ============================== */

    public static final boolean TRUE = true;
    public static final boolean FALSE = false;

    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";

    /* ==============================
       缓存与会话相关常量
       ============================== */

    /**
     * Redis Key 分隔符（建议统一使用冒号）
     */
    public static final String REDIS_KEY_SEPARATOR = ":";

    /**
     * Redis Key 前缀：用户信息
     */
    public static final String REDIS_KEY_USER = "user";

    /**
     * Redis Key 前缀：验证码
     */
    public static final String REDIS_KEY_CAPTCHA = "captcha";

    /**
     * Redis Key 前缀：会话（Session）
     */
    public static final String REDIS_KEY_SESSION = "session";

    /**
     * Redis Key 前缀：登录令牌（Token）
     */
    public static final String REDIS_KEY_TOKEN = "token";

    /**
     * 默认缓存过期时间（秒） - 10分钟
     */
    public static final long CACHE_EXPIRE_10MIN = 10 * 60;

    /**
     * 默认缓存过期时间（秒） - 30分钟
     */
    public static final long CACHE_EXPIRE_30MIN = 30 * 60;

    /**
     * 默认缓存过期时间（秒） - 1小时
     */
    public static final long CACHE_EXPIRE_1H = 60 * 60;

    /**
     * 默认缓存过期时间（秒） - 2小时
     */
    public static final long CACHE_EXPIRE_2H = 2 * 60 * 60;

    /**
     * 默认缓存过期时间（秒） - 1天
     */
    public static final long CACHE_EXPIRE_1D = 24 * 60 * 60;

    /**
     * Session Key：当前用户
     */
    public static final String SESSION_USER = "SESSION_USER";

    /**
     * Session Key：权限信息
     */
    public static final String SESSION_PERMISSIONS = "SESSION_PERMISSIONS";

    /**
     * Session Key：验证码
     */
    public static final String SESSION_CAPTCHA = "SESSION_CAPTCHA";

    /* ==============================
       文件相关常量
       ============================== */

    /**
     * 文件路径分隔符（系统相关）
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * 用户目录路径
     */
    public static final String USER_HOME = System.getProperty("user.home");

    /**
     * 临时文件目录
     */
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    /* ---------- 文件扩展名 ---------- */

    /**
     * 文件扩展名：文本
     */
    public static final String FILE_EXT_TXT = ".txt";

    /**
     * 文件扩展名：CSV（逗号分隔值）
     */
    public static final String FILE_EXT_CSV = ".csv";

    /**
     * 文件扩展名：Excel 2003
     */
    public static final String FILE_EXT_XLS = ".xls";

    /**
     * 文件扩展名：Excel 2007+
     */
    public static final String FILE_EXT_XLSX = ".xlsx";

    /**
     * 文件扩展名：Word 2003
     */
    public static final String FILE_EXT_DOC = ".doc";

    /**
     * 文件扩展名：Word 2007+
     */
    public static final String FILE_EXT_DOCX = ".docx";

    /**
     * 文件扩展名：PDF
     */
    public static final String FILE_EXT_PDF = ".pdf";

    /**
     * 文件扩展名：JSON
     */
    public static final String FILE_EXT_JSON = ".json";

    /**
     * 文件扩展名：XML
     */
    public static final String FILE_EXT_XML = ".xml";

    /**
     * 文件扩展名：ZIP
     */
    public static final String FILE_EXT_ZIP = ".zip";

    /**
     * 文件扩展名：RAR
     */
    public static final String FILE_EXT_RAR = ".rar";

    /**
     * 文件扩展名：7Z
     */
    public static final String FILE_EXT_7Z = ".7z";

    /**
     * 文件扩展名：JPG
     */
    public static final String FILE_EXT_JPG = ".jpg";

    /**
     * 文件扩展名：JPEG
     */
    public static final String FILE_EXT_JPEG = ".jpeg";

    /**
     * 文件扩展名：PNG
     */
    public static final String FILE_EXT_PNG = ".png";

    /**
     * 文件扩展名：GIF
     */
    public static final String FILE_EXT_GIF = ".gif";

    /**
     * 文件扩展名：BMP
     */
    public static final String FILE_EXT_BMP = ".bmp";

    /**
     * 文件扩展名：SVG
     */
    public static final String FILE_EXT_SVG = ".svg";

    /**
     * 文件扩展名：MP3
     */
    public static final String FILE_EXT_MP3 = ".mp3";

    /**
     * 文件扩展名：WAV
     */
    public static final String FILE_EXT_WAV = ".wav";

    /**
     * 文件扩展名：MP4
     */
    public static final String FILE_EXT_MP4 = ".mp4";

    /**
     * 文件扩展名：AVI
     */
    public static final String FILE_EXT_AVI = ".avi";

    /**
     * 文件扩展名：MOV
     */
    public static final String FILE_EXT_MOV = ".mov";

    /**
     * 文件扩展名：Markdown
     */
    public static final String FILE_EXT_MD = ".md";

    /**
     * 文件扩展名：HTML
     */
    public static final String FILE_EXT_HTML = ".html";

    /**
     * 文件扩展名：HTM
     */
    public static final String FILE_EXT_HTM = ".htm";

    /* ---------- MIME 类型 ---------- */

    /**
     * MIME 类型：文本（UTF-8 编码）
     */
    public static final String MIME_TYPE_TEXT = "text/plain;charset=UTF-8";

    /**
     * MIME 类型：CSV
     */
    public static final String MIME_TYPE_CSV = "text/csv;charset=UTF-8";

    /**
     * MIME 类型：JSON
     */
    public static final String MIME_TYPE_JSON = "application/json";

    /**
     * MIME 类型：XML
     */
    public static final String MIME_TYPE_XML = "application/xml";

    /**
     * MIME 类型：Excel
     */
    public static final String MIME_TYPE_EXCEL = "application/vnd.ms-excel";

    /**
     * MIME 类型：Excel（XLSX）
     */
    public static final String MIME_TYPE_EXCEL_X = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * MIME 类型：Word
     */
    public static final String MIME_TYPE_WORD = "application/msword";

    /**
     * MIME 类型：Word（DOCX）
     */
    public static final String MIME_TYPE_WORD_X = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * MIME 类型：PDF
     */
    public static final String MIME_TYPE_PDF = "application/pdf";

    /**
     * MIME 类型：ZIP
     */
    public static final String MIME_TYPE_ZIP = "application/zip";

    /**
     * MIME 类型：RAR
     */
    public static final String MIME_TYPE_RAR = "application/x-rar-compressed";

    /**
     * MIME 类型：7Z
     */
    public static final String MIME_TYPE_7Z = "application/x-7z-compressed";

    /**
     * MIME 类型：JPG
     */
    public static final String MIME_TYPE_JPG = "image/jpeg";

    /**
     * MIME 类型：PNG
     */
    public static final String MIME_TYPE_PNG = "image/png";

    /**
     * MIME 类型：GIF
     */
    public static final String MIME_TYPE_GIF = "image/gif";

    /**
     * MIME 类型：BMP
     */
    public static final String MIME_TYPE_BMP = "image/bmp";

    /**
     * MIME 类型：SVG
     */
    public static final String MIME_TYPE_SVG = "image/svg+xml";

    /**
     * MIME 类型：MP3
     */
    public static final String MIME_TYPE_MP3 = "audio/mpeg";

    /**
     * MIME 类型：WAV
     */
    public static final String MIME_TYPE_WAV = "audio/wav";

    /**
     * MIME 类型：MP4
     */
    public static final String MIME_TYPE_MP4 = "video/mp4";

    /**
     * MIME 类型：AVI
     */
    public static final String MIME_TYPE_AVI = "video/x-msvideo";

    /**
     * MIME 类型：MOV
     */
    public static final String MIME_TYPE_MOV = "video/quicktime";

    /**
     * MIME 类型：HTML
     */
    public static final String MIME_TYPE_HTML = "text/html;charset=UTF-8";

    /* ==============================
       HTTP 相关常量
       ============================== */

    /**
     * HTTP Header - Content-Type
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * HTTP Header - Authorization
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * HTTP Header - User-Agent
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * HTTP Header - Accept
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * HTTP Header - Accept-Language
     */
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * HTTP Header - Accept-Encoding（客户端可接收的压缩编码类型）
     */
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * HTTP Header - Cache-Control（缓存控制）
     */
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /**
     * HTTP Header - Pragma（HTTP/1.0 缓存控制）
     */
    public static final String HEADER_PRAGMA = "Pragma";

    /**
     * HTTP Header - Expires（响应过期时间）
     */
    public static final String HEADER_EXPIRES = "Expires";

    /**
     * HTTP Header - Content-Length（响应内容长度）
     */
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * HTTP Header - Content-Encoding（内容压缩编码类型）
     */
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    /**
     * HTTP Header - Connection（连接方式：keep-alive/close）
     */
    public static final String HEADER_CONNECTION = "Connection";

    /**
     * HTTP Header - Host（请求的服务器主机名和端口号）
     */
    public static final String HEADER_HOST = "Host";

    /**
     * HTTP Header - Referer（来源页面）
     */
    public static final String HEADER_REFERER = "Referer";

    /**
     * HTTP Header - Origin（跨域请求来源）
     */
    public static final String HEADER_ORIGIN = "Origin";

    /**
     * HTTP Header - Access-Control-Allow-Origin（跨域允许的来源）
     */
    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * HTTP Header - Access-Control-Allow-Methods（跨域允许的方法）
     */
    public static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * HTTP Header - Access-Control-Allow-Headers（跨域允许的请求头）
     */
    public static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * HTTP Content-Type - JSON
     */
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    /**
     * HTTP Content-Type - Form
     */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded;charset=UTF-8";

    /**
     * HTTP Content-Type - Multipart Form
     */
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data;charset=UTF-8";

    /**
     * HTTP Content-Type - XML
     */
    public static final String CONTENT_TYPE_XML = "application/xml;charset=UTF-8";

    /**
     * HTTP Content-Type - HTML
     */
    public static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";

    /**
     * HTTP Content-Type - Plain Text
     */
    public static final String CONTENT_TYPE_TEXT = "text/plain;charset=UTF-8";

    /**
     * HTTP Content-Type - JavaScript
     */
    public static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript;charset=UTF-8";

    /**
     * HTTP Content-Type - CSS
     */
    public static final String CONTENT_TYPE_CSS = "text/css;charset=UTF-8";

    /**
     * HTTP Method - GET
     */
    public static final String HTTP_GET = "GET";

    /**
     * HTTP Method - POST
     */
    public static final String HTTP_POST = "POST";

    /**
     * HTTP Method - PUT
     */
    public static final String HTTP_PUT = "PUT";

    /**
     * HTTP Method - DELETE
     */
    public static final String HTTP_DELETE = "DELETE";

    /**
     * HTTP Method - PATCH
     */
    public static final String HTTP_PATCH = "PATCH";

    /**
     * HTTP Method - HEAD
     */
    public static final String HTTP_HEAD = "HEAD";

    /**
     * HTTP Method - OPTIONS
     */
    public static final String HTTP_OPTIONS = "OPTIONS";

    /* ==============================
       通用业务枚举值常量
       ============================== */

    /**
     * 性别：未知
     */
    public static final int GENDER_UNKNOWN = 0;

    /**
     * 性别：男
     */
    public static final int GENDER_MALE = 1;

    /**
     * 性别：女
     */
    public static final int GENDER_FEMALE = 2;

    /**
     * 启用状态：禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 启用状态：启用
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 删除状态：未删除
     */
    public static final int DELETE_NO = 0;

    /**
     * 删除状态：已删除
     */
    public static final int DELETE_YES = 1;

    /* ==============================
       数据库相关常量
       ============================== */

    /**
     * 数据库字段：主键
     */
    public static final String DB_ID = "id";

    /**
     * 数据库字段：创建时间
     */
    public static final String DB_CREATE_TIME = "create_time";

    /**
     * 数据库字段：更新时间
     */
    public static final String DB_UPDATE_TIME = "update_time";

    /**
     * 数据库字段：删除标记
     */
    public static final String DB_DELETE_FLAG = "delete_flag";

    /**
     * 数据库通用排序字段：升序
     */
    public static final String DB_ORDER_ASC = "ASC";

    /**
     * 数据库通用排序字段：降序
     */
    public static final String DB_ORDER_DESC = "DESC";

    /**
     * MyBatis 分页参数：offset
     */
    public static final String DB_OFFSET = "offset";

    /**
     * MyBatis 分页参数：limit
     */
    public static final String DB_LIMIT = "limit";

    /* ==============================
       安全相关常量
       ============================== */

    /**
     * AES 加密算法
     */
    public static final String ALGORITHM_AES = "AES";

    /**
     * RSA 加密算法
     */
    public static final String ALGORITHM_RSA = "RSA";

    /**
     * SHA-256 摘要算法
     */
    public static final String ALGORITHM_SHA256 = "SHA-256";

    /**
     * MD5 摘要算法
     */
    public static final String ALGORITHM_MD5 = "MD5";

    /**
     * 盐值长度
     */
    public static final int SALT_LENGTH = 16;

    /**
     * 默认加密编码
     */
    public static final String ENCRYPTION_CHARSET = "UTF-8";

    /**
     * JWT Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT 默认过期时间（秒） - 2小时
     */
    public static final long JWT_EXPIRE_2H = 2 * 60 * 60;

    /* ==============================
       时间相关常量
       ============================== */

    /**
     * 毫秒 - 一秒
     */
    public static final long MILLIS_SECOND = 1000L;

    /**
     * 毫秒 - 一分钟
     */
    public static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 毫秒 - 一小时
     */
    public static final long MILLIS_HOUR = 60 * MILLIS_MINUTE;

    /**
     * 毫秒 - 一天
     */
    public static final long MILLIS_DAY = 24 * MILLIS_HOUR;

    /**
     * 毫秒 - 一周
     */
    public static final long MILLIS_WEEK = 7 * MILLIS_DAY;

    /**
     * 秒 - 一分钟
     */
    public static final long SECONDS_MINUTE = 60L;

    /**
     * 秒 - 一小时
     */
    public static final long SECONDS_HOUR = 60 * SECONDS_MINUTE;

    /**
     * 秒 - 一天
     */
    public static final long SECONDS_DAY = 24 * SECONDS_HOUR;

    /**
     * 秒 - 一周
     */
    public static final long SECONDS_WEEK = 7 * SECONDS_DAY;

    /**
     * 分钟 - 一小时
     */
    public static final long MINUTES_HOUR = 60L;

    /**
     * 分钟 - 一天
     */
    public static final long MINUTES_DAY = 24 * MINUTES_HOUR;

    /**
     * 凌晨 00:00
     */
    public static final String TIME_00_00 = "00:00";

    /**
     * 凌晨 01:00
     */
    public static final String TIME_01_00 = "01:00";

    /**
     * 凌晨 02:00
     */
    public static final String TIME_02_00 = "02:00";

    /**
     * 凌晨 03:00
     */
    public static final String TIME_03_00 = "03:00";

    /**
     * 凌晨 04:00
     */
    public static final String TIME_04_00 = "04:00";

    /**
     * 清晨 05:00
     */
    public static final String TIME_05_00 = "05:00";

    /**
     * 早上 06:00
     */
    public static final String TIME_06_00 = "06:00";

    /**
     * 早上 07:00
     */
    public static final String TIME_07_00 = "07:00";

    /**
     * 上午 08:00（常用于上班打卡）
     */
    public static final String TIME_08_00 = "08:00";

    /**
     * 上午 09:00（常用会议时间）
     */
    public static final String TIME_09_00 = "09:00";

    /**
     * 上午 10:00
     */
    public static final String TIME_10_00 = "10:00";

    /**
     * 上午 11:00
     */
    public static final String TIME_11_00 = "11:00";

    /**
     * 中午 12:00（午休开始）
     */
    public static final String TIME_12_00 = "12:00";

    /**
     * 下午 13:00（午休结束/下午工作开始）
     */
    public static final String TIME_13_00 = "13:00";

    /**
     * 下午 14:00
     */
    public static final String TIME_14_00 = "14:00";

    /**
     * 下午 15:00（常用会议/茶歇时间）
     */
    public static final String TIME_15_00 = "15:00";

    /**
     * 下午 16:00
     */
    public static final String TIME_16_00 = "16:00";

    /**
     * 下午 17:00
     */
    public static final String TIME_17_00 = "17:00";

    /**
     * 下午 18:00（下班高峰）
     */
    public static final String TIME_18_00 = "18:00";

    /**
     * 晚上 19:00
     */
    public static final String TIME_19_00 = "19:00";

    /**
     * 晚上 20:00
     */
    public static final String TIME_20_00 = "20:00";

    /**
     * 晚上 21:00
     */
    public static final String TIME_21_00 = "21:00";

    /**
     * 晚上 22:00
     */
    public static final String TIME_22_00 = "22:00";

    /**
     * 晚上 23:00
     */
    public static final String TIME_23_00 = "23:00";

    /**
     * 晚上 23:59（一天结束）
     */
    public static final String TIME_23_59 = "23:59";

    /**
     * 晚上 23:59.999999（精确）
     */
    public static final String TIME_23_59_999999 = "23:59.999999";

    /* ==============================
       第三方服务相关常量
       ============================== */

    /**
     * 第三方服务 - 支付宝 API 网关
     */
    public static final String ALIPAY_GATEWAY_URL = "https://openapi.alipay.com/gateway.do";

    /**
     * 第三方服务 - 微信支付统一下单 API
     */
    public static final String WECHAT_PAY_UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    /**
     * 第三方服务 - 微信支付订单查询 API
     */
    public static final String WECHAT_PAY_ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/pay/orderquery";

    /**
     * 第三方服务 - 阿里云短信 API
     */
    public static final String ALIYUN_SMS_API_URL = "https://dysmsapi.aliyuncs.com/";

    /**
     * 第三方服务 - 七牛云上传地址
     */
    public static final String QINIU_UPLOAD_URL = "https://upload.qiniup.com/";

    /**
     * 第三方服务 - HTTP Header 授权字段
     */
    public static final String HEADER_API_KEY = "X-API-KEY";

    /**
     * 第三方服务 - HTTP Header 签名字段
     */
    public static final String HEADER_SIGNATURE = "X-SIGNATURE";

    /* ==============================
       消息队列相关常量
       ============================== */

    /**
     * MQ Topic：订单相关
     */
    public static final String MQ_TOPIC_ORDER = "topic_order";

    /**
     * MQ Topic：支付相关
     */
    public static final String MQ_TOPIC_PAYMENT = "topic_payment";

    /**
     * MQ Topic：通知相关
     */
    public static final String MQ_TOPIC_NOTIFICATION = "topic_notification";

    /**
     * MQ Queue：延迟队列 - 订单超时关闭
     */
    public static final String MQ_QUEUE_ORDER_TIMEOUT = "queue_order_timeout";

    /**
     * MQ Queue：延迟队列 - 支付结果确认
     */
    public static final String MQ_QUEUE_PAYMENT_CONFIRM = "queue_payment_confirm";

    /**
     * MQ Exchange：订单交换机
     */
    public static final String MQ_EXCHANGE_ORDER = "exchange_order";

    /**
     * MQ Exchange：支付交换机
     */
    public static final String MQ_EXCHANGE_PAYMENT = "exchange_payment";

    /* ==============================
       微服务相关常量
       ============================== */

    /**
     * 微服务名：用户服务
     */
    public static final String SERVICE_USER = "service-user";

    /**
     * 微服务名：订单服务
     */
    public static final String SERVICE_ORDER = "service-order";

    /**
     * 微服务名：支付服务
     */
    public static final String SERVICE_PAYMENT = "service-payment";

    /**
     * 微服务名：库存服务
     */
    public static final String SERVICE_INVENTORY = "service-inventory";

    /**
     * 网关统一前缀
     */
    public static final String GATEWAY_PREFIX = "/api";

    /**
     * 网关路由：用户模块
     */
    public static final String GATEWAY_USER = GATEWAY_PREFIX + "/user";

    /**
     * 网关路由：订单模块
     */
    public static final String GATEWAY_ORDER = GATEWAY_PREFIX + "/order";

    /**
     * 网关路由：支付模块
     */
    public static final String GATEWAY_PAYMENT = GATEWAY_PREFIX + "/payment";

    /* ==============================
       常用异常与校验提示信息
       ============================== */

    /**
     * 通用错误提示
     */
    public static final String ERROR_COMMON = "系统错误，请稍后重试";

    /**
     * 参数校验失败
     */
    public static final String ERROR_INVALID_PARAM = "请求参数不合法";

    /**
     * 空指针异常提示
     */
    public static final String ERROR_NULL_POINTER = "空指针异常";

    /**
     * 数据未找到
     */
    public static final String ERROR_NOT_FOUND = "未找到相关数据";

    /**
     * 权限不足
     */
    public static final String ERROR_FORBIDDEN = "权限不足，无法访问";

    /**
     * 用户未登录
     */
    public static final String ERROR_UNAUTHORIZED = "用户未登录或登录已过期";

    /**
     * 数据库操作异常
     */
    public static final String ERROR_DATABASE = "数据库操作异常";

    /**
     * 网络异常
     */
    public static final String ERROR_NETWORK = "网络异常，请检查网络连接";

    /**
     * 文件上传失败
     */
    public static final String ERROR_UPLOAD_FAIL = "文件上传失败";

    /**
     * 文件格式不支持
     */
    public static final String ERROR_FILE_TYPE_UNSUPPORTED = "不支持的文件格式";

    /**
     * 文件大小超限
     */
    public static final String ERROR_FILE_SIZE_EXCEEDED = "文件大小超过限制";

    /**
     * 业务处理异常
     */
    public static final String ERROR_BUSINESS = "业务处理异常";

    /**
     * 请求超时
     */
    public static final String ERROR_TIMEOUT = "请求超时，请重试";

    /**
     * 系统维护中
     */
    public static final String ERROR_MAINTENANCE = "系统维护中，请稍后访问";

    /**
     * 参数不能为空
     */
    public static final String VALIDATE_NOT_NULL = "不能为空";

    /**
     * 参数不能为空，字段示例
     */
    public static final String VALIDATE_NOT_NULL_FIELD = "%s不能为空";

    /**
     * 参数格式错误，字段示例
     */
    public static final String VALIDATE_FORMAT_ERROR_FIELD = "%s格式不正确";

    /**
     * 参数长度超限，字段示例
     */
    public static final String VALIDATE_LENGTH_EXCEEDED_FIELD = "%s长度超出限制";

    /**
     * 数值范围错误
     */
    public static final String VALIDATE_RANGE_ERROR_FIELD = "%s数值超出范围";

    /**
     * 数组不能为空
     */
    public static final String VALIDATE_ARRAY_NOT_EMPTY = "数组不能为空";

    /**
     * 列表不能为空
     */
    public static final String VALIDATE_LIST_NOT_EMPTY = "列表不能为空";

    /**
     * 日期格式错误
     */
    public static final String VALIDATE_DATE_FORMAT_ERROR = "日期格式错误";

    /**
     * 邮箱格式错误
     */
    public static final String VALIDATE_EMAIL_FORMAT_ERROR = "邮箱格式不正确";

    /**
     * 手机号格式错误
     */
    public static final String VALIDATE_MOBILE_FORMAT_ERROR = "手机号格式不正确";

    /**
     * 身份证号格式错误
     */
    public static final String VALIDATE_ID_CARD_FORMAT_ERROR = "身份证号格式不正确";

    /**
     * 密码格式不符合要求
     */
    public static final String VALIDATE_PASSWORD_FORMAT_ERROR = "密码格式不符合要求";

    /**
     * 验证码错误
     */
    public static final String VALIDATE_CAPTCHA_ERROR = "验证码错误";

    /**
     * 重复数据
     */
    public static final String ERROR_DUPLICATE_DATA = "数据已存在，请勿重复提交";

    /**
     * 操作失败
     */
    public static final String ERROR_OPERATION_FAILED = "操作失败，请重试";

    /**
     * 操作成功
     */
    public static final String SUCCESS_OPERATION = "操作成功";

    /* ==============================
       数据库操作相关常量
       ============================== */

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小（防止一次性查询过多数据）
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 默认分页页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 排序：升序
     */
    public static final String ORDER_ASC = "ASC";

    /**
     * 排序：降序
     */
    public static final String ORDER_DESC = "DESC";

    /**
     * 排序字段分隔符（多字段排序用逗号分隔）
     */
    public static final String ORDER_BY_SEPARATOR = ",";

    /**
     * 数据库通用字段：主键
     */
    public static final String FIELD_ID = "id";

    /**
     * 数据库通用字段：创建时间
     */
    public static final String FIELD_CREATE_TIME = "create_time";

    /**
     * 数据库通用字段：更新时间
     */
    public static final String FIELD_UPDATE_TIME = "update_time";

    /**
     * 数据库通用字段：创建人
     */
    public static final String FIELD_CREATE_BY = "create_by";

    /**
     * 数据库通用字段：更新人
     */
    public static final String FIELD_UPDATE_BY = "update_by";

    /**
     * 数据库通用字段：逻辑删除标志
     */
    public static final String FIELD_DELETED = "deleted";

    /**
     * 逻辑删除：未删除
     */
    public static final Integer LOGIC_NOT_DELETED = 0;

    /**
     * 逻辑删除：已删除
     */
    public static final Integer LOGIC_DELETED = 1;

    /**
     * SQL 通配符：模糊查询（任意字符）
     */
    public static final String SQL_LIKE = "%";

    /**
     * SQL 模糊查询：左匹配
     */
    public static final String SQL_LIKE_LEFT = "%s%";

    /**
     * SQL 模糊查询：右匹配
     */
    public static final String SQL_LIKE_RIGHT = "%s%";

    /**
     * SQL 条件关键字：AND
     */
    public static final String SQL_AND = "AND";

    /**
     * SQL 条件关键字：OR
     */
    public static final String SQL_OR = "OR";

    /**
     * SQL 关键字：NULL
     */
    public static final String SQL_NULL = "NULL";

    /**
     * SQL 关键字：NOT NULL
     */
    public static final String SQL_NOT_NULL = "NOT NULL";

    /**
     * 默认 MyBatis-Plus 主键 ID 生成策略（雪花算法）
     */
    public static final String DEFAULT_ID_GENERATOR = "assign_id";

    /**
     * 默认批量插入大小（防止一次性插入过多导致内存溢出）
     */
    public static final int DEFAULT_BATCH_SIZE = 500;

    /**
     * 分页插件参数：当前页
     */
    public static final String PAGE_PARAM_CURRENT = "current";

    /**
     * 分页插件参数：每页大小
     */
    public static final String PAGE_PARAM_SIZE = "size";

    /**
     * 分页插件参数：排序字段
     */
    public static final String PAGE_PARAM_ORDER_BY = "orderBy";

    /**
     * 分页插件参数：排序规则
     */
    public static final String PAGE_PARAM_ORDER_RULE = "orderRule";

    /**
     * SQL 关键字：LIMIT
     */
    public static final String SQL_LIMIT = "LIMIT";

    /**
     * SQL 关键字：LIMIT 1（用于仅查询一条记录）
     */
    public static final String SQL_LIMIT_1 = "LIMIT 1";

    /**
     * SQL 关键字：LIMIT 2
     */
    public static final String SQL_LIMIT_2 = "LIMIT 2";

    /**
     * SQL 关键字：LIMIT 3
     */
    public static final String SQL_LIMIT_3 = "LIMIT 3";

    /**
     * SQL 关键字：LIMIT 4
     */
    public static final String SQL_LIMIT_4 = "LIMIT 4";

    /**
     * SQL 关键字：LIMIT 5
     */
    public static final String SQL_LIMIT_5 = "LIMIT 5";

    /**
     * SQL 关键字：LIMIT 6
     */
    public static final String SQL_LIMIT_6 = "LIMIT 6";

    /**
     * SQL 关键字：LIMIT 7
     */
    public static final String SQL_LIMIT_7 = "LIMIT 7";

    /**
     * SQL 关键字：LIMIT 8
     */
    public static final String SQL_LIMIT_8 = "LIMIT 8";

    /**
     * SQL 关键字：LIMIT 9
     */
    public static final String SQL_LIMIT_9 = "LIMIT 9";

    /**
     * SQL 关键字：LIMIT 10（常用于限制查询返回的结果数）
     */
    public static final String SQL_LIMIT_10 = "LIMIT 10";

    /**
     * SQL 关键字：LIMIT 20
     */
    public static final String SQL_LIMIT_20 = "LIMIT 20";

    /**
     * SQL 关键字：LIMIT 50
     */
    public static final String SQL_LIMIT_50 = "LIMIT 50";

    /**
     * SQL 关键字：LIMIT 100
     */
    public static final String SQL_LIMIT_100 = "LIMIT 100";

    /**
     * SQL 关键字：LIMIT 200
     */
    public static final String SQL_LIMIT_200 = "LIMIT 200";

    /**
     * SQL 关键字：LIMIT 500
     */
    public static final String SQL_LIMIT_500 = "LIMIT 500";

    /**
     * SQL 关键字：LIMIT 1000
     */
    public static final String SQL_LIMIT_1000 = "LIMIT 1000";

    /**
     * SQL 分页占位符（LIMIT n）
     */
    public static final String SQL_LIMIT_N = "LIMIT %d";

    /**
     * SQL 关键字：OFFSET
     */
    public static final String SQL_OFFSET = "OFFSET";

    /**
     * SQL 分页占位符（MySQL 用法：LIMIT ?, ?）
     */
    public static final String SQL_LIMIT_PLACEHOLDER = "LIMIT %d, %d";

    /**
     * SQL 分页占位符（PostgreSQL 用法：LIMIT ? OFFSET ?）
     */
    public static final String SQL_LIMIT_OFFSET_PLACEHOLDER = "LIMIT %d OFFSET %d";

    /**
     * 默认 LIMIT 条数（防止无分页条件全表扫描）
     */
    public static final int DEFAULT_LIMIT = 100;

    /**
     * 无限制分页（需谨慎）
     */
    public static final int LIMIT_NO_RESTRICTION = -1;


}

