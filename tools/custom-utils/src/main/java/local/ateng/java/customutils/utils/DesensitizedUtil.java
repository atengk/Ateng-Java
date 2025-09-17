package local.ateng.java.customutils.utils;

/**
 * 数据脱敏工具类
 * <p>
 * 提供常见敏感信息的脱敏处理方法，例如姓名、身份证号、手机号等。
 * 在日志打印、接口返回时可使用本工具类进行数据保护，防止敏感信息泄露。
 * </p>
 *
 * @author 孔余
 * @since 2025-09-17
 */
public final class DesensitizedUtil {

    /**
     * 禁止实例化工具类
     */
    private DesensitizedUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    private static final String ASTERISK = "*";

    private static final String PASSWORD_MASK = "******";

    private static final String MOBILE_MASK = "****";

    private static final String ID_CARD_MASK = "********";

    private static final String BANK_CARD_MASK = "********";

    private static final String ADDRESS_MASK = "***";

    private static final String EMAIL_MASK = "***";

    /**
     * 用户ID脱敏
     * <p>
     * 保留前两位和后两位，中间固定替换为四个星号
     * </p>
     *
     * @param userId 用户ID
     * @return 脱敏后的用户ID
     */
    public static String desensitizeUserId(String userId) {
        if (ObjectUtil.isEmpty(userId) || userId.length() < 4) {
            return userId;
        }
        return userId.substring(0, 2) + "****" + userId.substring(userId.length() - 2);
    }

    /**
     * 中文姓名脱敏
     * <p>
     * 保留第一个汉字，其他统一替换为两个星号
     * </p>
     *
     * @param fullName 姓名
     * @return 脱敏后的姓名
     */
    public static String desensitizeChineseName(String fullName) {
        if (ObjectUtil.isEmpty(fullName)) {
            return "";
        }
        return fullName.charAt(0) + "**";
    }

    /**
     * 身份证号脱敏
     * <p>
     * 保留前三位和后四位，中间统一替换为八个星号
     * </p>
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String desensitizeIdCard(String idCard) {
        if (ObjectUtil.isEmpty(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + ID_CARD_MASK + idCard.substring(idCard.length() - 4);
    }

    /**
     * 座机号脱敏
     * <p>
     * 保留前四位和后两位，中间统一替换为四个星号
     * </p>
     *
     * @param phone 座机号
     * @return 脱敏后的座机号
     */
    public static String desensitizeFixedPhone(String phone) {
        if (ObjectUtil.isEmpty(phone) || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * 手机号脱敏
     * <p>
     * 保留前三位和后四位，中间统一替换为四个星号
     * </p>
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizeMobile(String mobile) {
        if (ObjectUtil.isEmpty(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + MOBILE_MASK + mobile.substring(mobile.length() - 4);
    }

    /**
     * 地址脱敏
     * <p>
     * 保留前六位字符，后续统一替换为三个星号
     * </p>
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String desensitizeAddress(String address) {
        if (ObjectUtil.isEmpty(address) || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + ADDRESS_MASK;
    }

    /**
     * 邮箱脱敏
     * <p>
     * 保留首尾字符，中间统一替换为三个星号，域名部分完整保留
     * </p>
     *
     * @param email 邮箱地址
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (ObjectUtil.isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String prefix = parts[0];
        String domain = parts[1];
        if (prefix.length() <= 2) {
            return prefix.charAt(0) + EMAIL_MASK + "@" + domain;
        }
        return prefix.charAt(0) + EMAIL_MASK + prefix.charAt(prefix.length() - 1) + "@" + domain;
    }

    /**
     * 密码脱敏
     * <p>
     * 不保留任何明文，统一返回固定长度******
     * </p>
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String desensitizePassword(String password) {
        if (ObjectUtil.isEmpty(password)) {
            return "";
        }
        return PASSWORD_MASK;
    }

    /**
     * 车牌号脱敏
     * <p>
     * 普通车牌（7位）：保留前两位和后1位，中间统一替换为四个星号
     * 新能源车牌（8位）：保留前两位和后2位，中间统一替换为四个星号
     * </p>
     *
     * @param carNumber 车牌号
     * @return 脱敏后的车牌号
     */
    public static String desensitizeCarNumber(String carNumber) {
        if (ObjectUtil.isEmpty(carNumber)) {
            return "";
        }
        int length = carNumber.length();
        if (length == 7) {
            return carNumber.substring(0, 2) + "****" + carNumber.substring(6);
        } else if (length == 8) {
            return carNumber.substring(0, 2) + "****" + carNumber.substring(6);
        }
        return carNumber;
    }

    /**
     * 银行卡号脱敏
     * <p>
     * 保留前六位和后四位，中间统一替换为八个星号
     * </p>
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String desensitizeBankCard(String bankCard) {
        if (ObjectUtil.isEmpty(bankCard) || bankCard.length() < 10) {
            return bankCard;
        }
        return bankCard.substring(0, 6) + BANK_CARD_MASK + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 出生日期脱敏
     * <p>
     * 脱敏规则：保留年份，月份与日期统一替换为 **-**
     * 例如：1990-05-20 → 1990-**-**
     * </p>
     *
     * @param birthDate 出生日期字符串
     * @return 脱敏后的出生日期
     */
    public static String desensitizeBirthDate(String birthDate) {
        if (ObjectUtil.isEmpty(birthDate)) {
            return "";
        }
        if (birthDate.length() >= 4) {
            return birthDate.substring(0, 4) + "-**-**";
        }
        return "****-**-**";
    }

    /**
     * 年龄脱敏
     * <p>
     * 脱敏规则：将精确年龄替换为区间
     * 例如：32 → 30-35
     * </p>
     *
     * @param age 年龄
     * @return 脱敏后的年龄字符串
     */
    public static String desensitizeAge(Integer age) {
        if (age == null || age < 0) {
            return "";
        }
        int lower = (age / 5) * 5;
        int upper = lower + 5;
        return lower + "-" + upper;
    }

    /**
     * 护照号码脱敏
     * <p>
     * 脱敏规则：保留首字母和最后一位，其余替换为*******
     * 例如：E12345678 → E*******8
     * </p>
     *
     * @param passport 护照号码
     * @return 脱敏后的护照号码
     */
    public static String desensitizePassport(String passport) {
        if (ObjectUtil.isEmpty(passport) || passport.length() < 2) {
            return passport;
        }
        return passport.substring(0, 1) + "*******" + passport.substring(passport.length() - 1);
    }

    /**
     * 通行证号码脱敏（港澳/台湾通行证）
     * <p>
     * 脱敏规则：保留前 2 位和最后 2 位，中间固定替换为 ****
     * 例如：C12345678 → C1****78
     * </p>
     *
     * @param pass 通行证号码
     * @return 脱敏后的通行证号码
     */
    public static String desensitizePass(String pass) {
        if (ObjectUtil.isEmpty(pass) || pass.length() < 4) {
            return pass;
        }
        return pass.substring(0, 2) + "****" + pass.substring(pass.length() - 2);
    }

    /**
     * 统一社会信用代码脱敏
     * <p>
     * 脱敏规则：保留前 6 位和后 3 位，中间统一替换为 *********
     * 例如：91350211M000100Y43 → 913502*********Y43
     * </p>
     *
     * @param creditCode 统一社会信用代码
     * @return 脱敏后的统一社会信用代码
     */
    public static String desensitizeCreditCode(String creditCode) {
        if (ObjectUtil.isEmpty(creditCode) || creditCode.length() < 9) {
            return creditCode;
        }
        return creditCode.substring(0, 6) + "*********" + creditCode.substring(creditCode.length() - 3);
    }

    /**
     * 税号脱敏
     * <p>
     * 脱敏规则：保留前 6 位和后 3 位，中间统一替换为 *********
     * 例如：110101201707010016 → 110101*********016
     * </p>
     *
     * @param taxId 税号或纳税人识别号
     * @return 脱敏后的税号
     */
    public static String desensitizeTaxId(String taxId) {
        if (ObjectUtil.isEmpty(taxId) || taxId.length() < 9) {
            return taxId;
        }
        return taxId.substring(0, 6) + "*********" + taxId.substring(taxId.length() - 3);
    }

    /**
     * 对公银行账户脱敏
     * <p>
     * 脱敏规则：保留前 6 位和后 4 位，中间统一替换为 ********
     * 例如：6227001234567890123 → 622700********90123
     * </p>
     *
     * @param account 对公银行账户
     * @return 脱敏后的对公银行账户
     */
    public static String desensitizePublicBankAccount(String account) {
        if (ObjectUtil.isEmpty(account) || account.length() < 10) {
            return account;
        }
        return account.substring(0, 6) + "********" + account.substring(account.length() - 4);
    }

    /**
     * 工号或学号脱敏
     * <p>
     * 脱敏规则：保留前 2 位和后 2 位，中间统一替换为 ****
     * 例如：2023001234 → 20****34
     * </p>
     *
     * @param code 工号或学号
     * @return 脱敏后的工号或学号
     */
    public static String desensitizeWorkOrStudentCode(String code) {
        if (ObjectUtil.isEmpty(code) || code.length() < 4) {
            return code;
        }
        return code.substring(0, 2) + "****" + code.substring(code.length() - 2);
    }

    /**
     * 社保号脱敏
     * <p>
     * 脱敏规则：保留前 4 位和后 4 位，中间统一替换为 ********
     * 例如：1234567890123456 → 1234********3456
     * </p>
     *
     * @param socialSecurityNumber 社保号
     * @return 脱敏后的社保号
     */
    public static String desensitizeSocialSecurityNumber(String socialSecurityNumber) {
        if (ObjectUtil.isEmpty(socialSecurityNumber) || socialSecurityNumber.length() < 8) {
            return socialSecurityNumber;
        }
        return socialSecurityNumber.substring(0, 4) + "********" + socialSecurityNumber.substring(socialSecurityNumber.length() - 4);
    }

    /**
     * 医保号脱敏
     * <p>
     * 脱敏规则：保留前 4 位和后 4 位，中间统一替换为 ********
     * 例如：9876543210987654 → 9876********7654
     * </p>
     *
     * @param medicalInsuranceNumber 医保号
     * @return 脱敏后的医保号
     */
    public static String desensitizeMedicalInsuranceNumber(String medicalInsuranceNumber) {
        if (ObjectUtil.isEmpty(medicalInsuranceNumber) || medicalInsuranceNumber.length() < 8) {
            return medicalInsuranceNumber;
        }
        return medicalInsuranceNumber.substring(0, 4) + "********" + medicalInsuranceNumber.substring(medicalInsuranceNumber.length() - 4);
    }

    /**
     * IPv4 地址脱敏
     * <p>
     * 脱敏规则：保留前两段，后两段统一替换为 *.*
     * 例如：192.168.1.100 → 192.168.*.*
     * </p>
     *
     * @param ipv4 IPv4 地址
     * @return 脱敏后的 IPv4 地址
     */
    public static String desensitizeIPv4(String ipv4) {
        if (ObjectUtil.isEmpty(ipv4) || !ipv4.contains(".")) {
            return ipv4;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return ipv4;
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }

    /**
     * IPv6 地址脱敏
     * <p>
     * 脱敏规则：保留前 4 段，后续统一替换为 :****:****
     * 例如：2001:0db8:85a3:0000:0000:8a2e:0370:7334 → 2001:0db8:85a3:0000:****:****
     * </p>
     *
     * @param ipv6 IPv6 地址
     * @return 脱敏后的 IPv6 地址
     */
    public static String desensitizeIPv6(String ipv6) {
        if (ObjectUtil.isEmpty(ipv6) || !ipv6.contains(":")) {
            return ipv6;
        }
        String[] parts = ipv6.split(":");
        if (parts.length < 4) {
            return ipv6;
        }
        return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":****:****";
    }

    /**
     * MAC 地址脱敏
     * <p>
     * 脱敏规则：保留前 3 段，后 3 段统一替换为 ****
     * 例如：00-16-3E-7E-4A-1A → 00-16-3E-****
     * </p>
     *
     * @param mac MAC 地址
     * @return 脱敏后的 MAC 地址
     */
    public static String desensitizeMac(String mac) {
        if (ObjectUtil.isEmpty(mac) || !mac.contains("-")) {
            return mac;
        }
        String[] parts = mac.split("-");
        if (parts.length < 3) {
            return mac;
        }
        return parts[0] + "-" + parts[1] + "-" + parts[2] + "-****";
    }

    /**
     * 地理坐标脱敏
     * <p>
     * 脱敏规则：经纬度各保留前 5 位，其余统一替换为 ****
     * 例如：29.56301,106.55156 → 29.56****,106.55****
     * </p>
     *
     * @param coordinate 经纬度字符串
     * @return 脱敏后的地理坐标
     */
    public static String desensitizeCoordinate(String coordinate) {
        if (ObjectUtil.isEmpty(coordinate) || !coordinate.contains(",")) {
            return coordinate;
        }
        String[] parts = coordinate.split(",");
        if (parts.length != 2) {
            return coordinate;
        }
        String lat = parts[0];
        String lon = parts[1];
        String latMasked = lat.length() > 5 ? lat.substring(0, 5) + "****" : lat + "****";
        String lonMasked = lon.length() > 5 ? lon.substring(0, 5) + "****" : lon + "****";
        return latMasked + "," + lonMasked;
    }

    /**
     * 支付宝账号脱敏
     * <p>
     * 脱敏规则：若为手机号/邮箱形式，按手机号或邮箱规则脱敏
     * 例如：13812345678 → 138****5678
     * 例如：zhangsan@example.com → z***n@example.com
     * </p>
     *
     * @param account 支付宝账号
     * @return 脱敏后的支付宝账号
     */
    public static String desensitizeAlipay(String account) {
        if (ObjectUtil.isEmpty(account)) {
            return "";
        }
        if (account.contains("@")) {
            return desensitizeEmail(account);
        }
        if (account.matches("^\\d{7,}$")) {
            return desensitizeMobile(account);
        }
        return "****";
    }

    /**
     * 微信号脱敏
     * <p>
     * 脱敏规则：保留前三位和后两位，中间统一替换为 ****
     * 例如：wxid123456 → wxi****56
     * </p>
     *
     * @param wechat 微信号
     * @return 脱敏后的微信号
     */
    public static String desensitizeWechat(String wechat) {
        if (ObjectUtil.isEmpty(wechat) || wechat.length() < 5) {
            return wechat;
        }
        return wechat.substring(0, 3) + "****" + wechat.substring(wechat.length() - 2);
    }

    /**
     * 人脸或指纹数据脱敏
     * <p>
     * 脱敏规则：统一替换为 [SENSITIVE]，不保留任何明文
     * </p>
     *
     * @param biometricData 人脸或指纹特征字符串
     * @return 脱敏后的数据
     */
    public static String desensitizeBiometric(String biometricData) {
        if (ObjectUtil.isEmpty(biometricData)) {
            return "";
        }
        return "[SENSITIVE]";
    }

    /**
     * 合同编号脱敏
     * <p>
     * 脱敏规则：保留前 3 位和后 3 位，中间统一替换为 ****
     * 例如：HT202309001 → HT2****001
     * </p>
     *
     * @param contractNo 合同编号
     * @return 脱敏后的合同编号
     */
    public static String desensitizeContractNo(String contractNo) {
        if (ObjectUtil.isEmpty(contractNo) || contractNo.length() < 6) {
            return contractNo;
        }
        return contractNo.substring(0, 3) + "****" + contractNo.substring(contractNo.length() - 3);
    }

    /**
     * 信用卡 CVV2 脱敏
     * <p>
     * 脱敏规则：统一替换为 ***
     * </p>
     *
     * @param cvv2 信用卡 CVV2
     * @return 脱敏后的 CVV2
     */
    public static String desensitizeCreditCardCvv2(String cvv2) {
        if (ObjectUtil.isEmpty(cvv2)) {
            return "";
        }
        return "***";
    }

    /**
     * 信用卡有效期脱敏
     * <p>
     * 脱敏规则：统一替换为 **\/**，不保留任何明文
     * 例如：12/25 → **\/**
     * </p>
     *
     * @param expiryDate 信用卡有效期
     * @return 脱敏后的有效期
     */
    public static String desensitizeCreditCardExpiry(String expiryDate) {
        if (ObjectUtil.isEmpty(expiryDate)) {
            return "";
        }
        return "**/**";
    }

    /**
     * 通用脱敏方法
     * <p>
     * 支持自定义前缀保留长度、后缀保留长度、掩码字符和掩码长度
     * 如果 maskLength 为 0，则自动计算中间掩码长度覆盖原始值
     * </p>
     *
     * @param origin     原始字符串
     * @param prefixLen  前缀保留长度
     * @param suffixLen  后缀保留长度
     * @param maskChar   脱敏字符
     * @param maskLength 中间脱敏长度，如果为0则自动计算
     * @return 脱敏后的字符串
     */
    public static String desensitize(String origin, int prefixLen, int suffixLen, char maskChar, int maskLength) {
        if (ObjectUtil.isEmpty(origin)) {
            return "";
        }
        int originLength = origin.length();

        // 前缀 + 后缀大于原始长度时直接返回原始值
        if (prefixLen + suffixLen >= originLength) {
            return origin;
        }

        int middleLength = maskLength > 0 ? maskLength : originLength - prefixLen - suffixLen;

        StringBuilder sb = new StringBuilder();
        sb.append(origin, 0, prefixLen);
        for (int i = 0; i < middleLength; i++) {
            sb.append(maskChar);
        }
        sb.append(origin.substring(originLength - suffixLen));

        return sb.toString();
    }

}
