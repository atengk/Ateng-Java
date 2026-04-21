//package local.ateng.java.customutils.utils;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Base64;
//
///**
// * 安全工具类（还需要处理问题）
// *
// * @author Ateng
// * @since 2026-04-21
// */
//public final class SecureUtil {
//
//    private static final String DEFAULT_CHARSET = "UTF-8";
//
//    /**
//     * 常用 AES 模式
//     */
//    public static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";
//    public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
//    public static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
//
//    /**
//     * 常用 DES 模式
//     */
//    public static final String DES_ECB_PKCS5 = "DES/ECB/PKCS5Padding";
//    public static final String DES_CBC_PKCS5 = "DES/CBC/PKCS5Padding";
//
//    /**
//     * 常用 3DES 模式
//     */
//    public static final String DESede_ECB_PKCS5 = "DESede/ECB/PKCS5Padding";
//    public static final String DESede_CBC_PKCS5 = "DESede/CBC/PKCS5Padding";
//
//    /**
//     * SM4（依赖 BC）
//     */
//    public static final String SM4_ECB_PKCS5 = "SM4/ECB/PKCS5Padding";
//    public static final String SM4_CBC_PKCS5 = "SM4/CBC/PKCS5Padding";
//
//    /**
//     * 禁止实例化工具类
//     */
//    private SecureUtil() {
//        throw new UnsupportedOperationException("工具类不可实例化");
//    }
//
//    /**
//     * 字符串加密（返回Base64）
//     */
//    public static String encrypt(String data, String algorithm, String key) {
//        return encrypt(data, algorithm, key, null);
//    }
//
//    /**
//     * 字符串加密（支持IV）
//     */
//    public static String encrypt(String data, String algorithm, String key, byte[] iv) {
//        try {
//            byte[] bytes = data.getBytes(DEFAULT_CHARSET);
//            byte[] encrypted = encrypt(bytes, algorithm, key.getBytes(DEFAULT_CHARSET), iv);
//            return Base64.getEncoder().encodeToString(encrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("字符串加密失败", e);
//        }
//    }
//
//    /**
//     * 字符串解密（Base64输入）
//     */
//    public static String decrypt(String data, String algorithm, String key) {
//        return decrypt(data, algorithm, key, null);
//    }
//
//    /**
//     * 字符串解密（支持IV）
//     */
//    public static String decrypt(String data, String algorithm, String key, byte[] iv) {
//        try {
//            byte[] decoded = Base64.getDecoder().decode(data);
//            byte[] decrypted = decrypt(decoded, algorithm, key.getBytes(DEFAULT_CHARSET), iv);
//            return new String(decrypted, DEFAULT_CHARSET);
//        } catch (Exception e) {
//            throw new RuntimeException("字符串解密失败", e);
//        }
//    }
//
//    /**
//     * 字节数组加密
//     */
//    public static byte[] encrypt(byte[] data, String algorithm, byte[] key, byte[] iv) {
//        try {
//            Cipher cipher = Cipher.getInstance(algorithm);
//            Key secretKey = new SecretKeySpec(key, getAlgorithmName(algorithm));
//
//            if (iv != null) {
//                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
//            } else {
//                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
//            }
//
//            return cipher.doFinal(data);
//        } catch (Exception e) {
//            throw new RuntimeException("字节数组加密失败", e);
//        }
//    }
//
//    /**
//     * 字节数组解密
//     */
//    public static byte[] decrypt(byte[] data, String algorithm, byte[] key, byte[] iv) {
//        try {
//            Cipher cipher = Cipher.getInstance(algorithm);
//            Key secretKey = new SecretKeySpec(key, getAlgorithmName(algorithm));
//
//            if (iv != null) {
//                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
//            } else {
//                cipher.init(Cipher.DECRYPT_MODE, secretKey);
//            }
//
//            return cipher.doFinal(data);
//        } catch (Exception e) {
//            throw new RuntimeException("字节数组解密失败", e);
//        }
//    }
//
//    /**
//     * 文件加密
//     */
//    public static void encryptFile(File source, File target, String algorithm, byte[] key, byte[] iv) {
//        processFile(source, target, algorithm, key, iv, Cipher.ENCRYPT_MODE);
//    }
//
//    /**
//     * 文件解密
//     */
//    public static void decryptFile(File source, File target, String algorithm, byte[] key, byte[] iv) {
//        processFile(source, target, algorithm, key, iv, Cipher.DECRYPT_MODE);
//    }
//
//    /**
//     * 文件处理核心逻辑
//     */
//    private static void processFile(File source, File target, String algorithm, byte[] key, byte[] iv, int mode) {
//        try (InputStream is = new FileInputStream(source);
//             OutputStream os = new FileOutputStream(target)) {
//
//            Cipher cipher = Cipher.getInstance(algorithm);
//            Key secretKey = new SecretKeySpec(key, getAlgorithmName(algorithm));
//
//            if (iv != null) {
//                cipher.init(mode, secretKey, new IvParameterSpec(iv));
//            } else {
//                cipher.init(mode, secretKey);
//            }
//
//            byte[] buffer = new byte[4096];
//            int len;
//            while ((len = is.read(buffer)) != -1) {
//                byte[] processed = cipher.update(buffer, 0, len);
//                if (processed != null) {
//                    os.write(processed);
//                }
//            }
//
//            byte[] finalBytes = cipher.doFinal();
//            if (finalBytes != null) {
//                os.write(finalBytes);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("文件加解密失败", e);
//        }
//    }
//
//    /**
//     * Base64编码
//     */
//    public static String base64Encode(byte[] data) {
//        return Base64.getEncoder().encodeToString(data);
//    }
//
//    /**
//     * Base64解码
//     */
//    public static byte[] base64Decode(String data) {
//        return Base64.getDecoder().decode(data);
//    }
//
//    /**
//     * 提取算法名称（如 AES/CBC/PKCS5Padding -> AES）
//     */
//    private static String getAlgorithmName(String algorithm) {
//        if (algorithm == null) {
//            return null;
//        }
//        int index = algorithm.indexOf('/');
//        return index > 0 ? algorithm.substring(0, index) : algorithm;
//    }
//
//    /**
//     * AES 加密（字符串 -> Base64）
//     */
//    public static String aesEncrypt(String data, String key, String transformation, byte[] iv) {
//        return encrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * AES 解密（Base64 -> 字符串）
//     */
//    public static String aesDecrypt(String data, String key, String transformation, byte[] iv) {
//        return decrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * DES 加密
//     */
//    public static String desEncrypt(String data, String key, String transformation, byte[] iv) {
//        return encrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * DES 解密
//     */
//    public static String desDecrypt(String data, String key, String transformation, byte[] iv) {
//        return decrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * 3DES 加密（DESede）
//     */
//    public static String tripleDesEncrypt(String data, String key, String transformation, byte[] iv) {
//        return encrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * 3DES 解密（DESede）
//     */
//    public static String tripleDesDecrypt(String data, String key, String transformation, byte[] iv) {
//        return decrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * SM4 加密（需要 BouncyCastle Provider）
//     */
//    public static String sm4Encrypt(String data, String key, String transformation, byte[] iv) {
//        return encrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * SM4 解密（需要 BouncyCastle Provider）
//     */
//    public static String sm4Decrypt(String data, String key, String transformation, byte[] iv) {
//        return decrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * 通用对称加密（字节数组）
//     */
//    public static byte[] symmetricEncrypt(byte[] data, byte[] key, String transformation, byte[] iv) {
//        return encrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * 通用对称解密（字节数组）
//     */
//    public static byte[] symmetricDecrypt(byte[] data, byte[] key, String transformation, byte[] iv) {
//        return decrypt(data, transformation, key, iv);
//    }
//
//    /**
//     * 根据密码生成对称密钥（适用于 AES/DES/3DES）
//     *
//     * @param password  密码
//     * @param algorithm 算法（AES / DES / DESede）
//     * @param keySize   密钥长度（AES:128/192/256，DES:56，DESede:168）
//     */
//    public static byte[] generateKey(String password, String algorithm, int keySize) {
//        try {
//            javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(algorithm);
//            java.security.SecureRandom secureRandom = java.security.SecureRandom.getInstance("SHA1PRNG");
//            secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8));
//            keyGenerator.init(keySize, secureRandom);
//            return keyGenerator.generateKey().getEncoded();
//        } catch (Exception e) {
//            throw new RuntimeException("生成密钥失败", e);
//        }
//    }
//
//    /**
//     * 生成随机密钥
//     */
//    public static byte[] generateRandomKey(String algorithm, int keySize) {
//        try {
//            javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(algorithm);
//            keyGenerator.init(keySize);
//            return keyGenerator.generateKey().getEncoded();
//        } catch (Exception e) {
//            throw new RuntimeException("生成随机密钥失败", e);
//        }
//    }
//
//    /**
//     * 生成随机 IV
//     */
//    public static byte[] generateIv(int length) {
//        byte[] iv = new byte[length];
//        new java.security.SecureRandom().nextBytes(iv);
//        return iv;
//    }
//
//    /**
//     * 生成 RSA 密钥对
//     */
//    public static java.security.KeyPair generateRsaKeyPair(int keySize) {
//        try {
//            java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
//            keyPairGenerator.initialize(keySize);
//            return keyPairGenerator.generateKeyPair();
//        } catch (Exception e) {
//            throw new RuntimeException("生成RSA密钥对失败", e);
//        }
//    }
//
//    /**
//     * 公钥转 Base64（X.509）
//     */
//    public static String getPublicKey(java.security.PublicKey publicKey) {
//        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
//    }
//
//    /**
//     * 私钥转 Base64（PKCS#8）
//     */
//    public static String getPrivateKey(java.security.PrivateKey privateKey) {
//        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
//    }
//
//    /**
//     * Base64 转公钥（X.509）
//     */
//    public static java.security.PublicKey loadPublicKey(String base64Key) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(base64Key);
//            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(bytes);
//            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
//            return keyFactory.generatePublic(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载公钥失败", e);
//        }
//    }
//
//    /**
//     * Base64 转私钥（PKCS#8）
//     */
//    public static java.security.PrivateKey loadPrivateKey(String base64Key) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(base64Key);
//            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(bytes);
//            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
//            return keyFactory.generatePrivate(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载私钥失败", e);
//        }
//    }
//
//    /**
//     * PKCS#1 私钥转 PKCS#8
//     */
//    public static java.security.PrivateKey loadPkcs1PrivateKey(String base64Key) {
//        try {
//            byte[] pkcs1Bytes = Base64.getDecoder().decode(base64Key);
//
//            // 构造 PKCS#8 结构
//            byte[] pkcs8Bytes = wrapPkcs1ToPkcs8(pkcs1Bytes);
//
//            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(pkcs8Bytes);
//            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
//            return keyFactory.generatePrivate(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载PKCS#1私钥失败", e);
//        }
//    }
//
//    /**
//     * PKCS#1 -> PKCS#8 封装
//     */
//    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
//        try {
//            final byte[] header = new byte[]{
//                    0x30, (byte) 0x82, 0x00, 0x00,
//                    0x02, 0x01, 0x00,
//                    0x30, 0x0d,
//                    0x06, 0x09,
//                    0x2a, (byte) 0x86, 0x48, (byte) 0x86,
//                    (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
//                    0x05, 0x00,
//                    0x04, (byte) 0x82, 0x00, 0x00
//            };
//
//            int totalLength = pkcs1.length + 22;
//            header[2] = (byte) ((totalLength >> 8) & 0xff);
//            header[3] = (byte) (totalLength & 0xff);
//
//            header[header.length - 2] = (byte) ((pkcs1.length >> 8) & 0xff);
//            header[header.length - 1] = (byte) (pkcs1.length & 0xff);
//
//            byte[] result = new byte[header.length + pkcs1.length];
//            System.arraycopy(header, 0, result, 0, header.length);
//            System.arraycopy(pkcs1, 0, result, header.length, pkcs1.length);
//
//            return result;
//        } catch (Exception e) {
//            throw new RuntimeException("PKCS1转PKCS8失败", e);
//        }
//    }
//
//    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
//
//    /**
//     * 公钥加密（分段）
//     */
//    public static String rsaEncryptByPublicKey(String data, java.security.PublicKey publicKey) {
//        try {
//            byte[] result = rsaSplitCodec(data.getBytes(StandardCharsets.UTF_8), publicKey, Cipher.ENCRYPT_MODE);
//            return Base64.getEncoder().encodeToString(result);
//        } catch (Exception e) {
//            throw new RuntimeException("RSA公钥加密失败", e);
//        }
//    }
//
//    /**
//     * 私钥解密（分段）
//     */
//    public static String rsaDecryptByPrivateKey(String data, java.security.PrivateKey privateKey) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(data);
//            byte[] result = rsaSplitCodec(bytes, privateKey, Cipher.DECRYPT_MODE);
//            return new String(result, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new RuntimeException("RSA私钥解密失败", e);
//        }
//    }
//
//    /**
//     * 私钥加密（分段）
//     */
//    public static String rsaEncryptByPrivateKey(String data, java.security.PrivateKey privateKey) {
//        try {
//            byte[] result = rsaSplitCodec(data.getBytes(StandardCharsets.UTF_8), privateKey, Cipher.ENCRYPT_MODE);
//            return Base64.getEncoder().encodeToString(result);
//        } catch (Exception e) {
//            throw new RuntimeException("RSA私钥加密失败", e);
//        }
//    }
//
//    /**
//     * 公钥解密（分段）
//     */
//    public static String rsaDecryptByPublicKey(String data, java.security.PublicKey publicKey) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(data);
//            byte[] result = rsaSplitCodec(bytes, publicKey, Cipher.DECRYPT_MODE);
//            return new String(result, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new RuntimeException("RSA公钥解密失败", e);
//        }
//    }
//
//    /**
//     * RSA 分段处理
//     */
//    private static byte[] rsaSplitCodec(byte[] data, java.security.Key key, int mode) {
//        try {
//            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(RSA_TRANSFORMATION);
//            cipher.init(mode, key);
//
//            int keySize = ((java.security.interfaces.RSAKey) key).getModulus().bitLength();
//            int maxBlock;
//
//            if (mode == Cipher.ENCRYPT_MODE) {
//                maxBlock = keySize / 8 - 11;
//            } else {
//                maxBlock = keySize / 8;
//            }
//
//            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
//            int offset = 0;
//
//            while (data.length > offset) {
//                int len = Math.min(maxBlock, data.length - offset);
//                byte[] block = cipher.doFinal(data, offset, len);
//                out.write(block);
//                offset += len;
//            }
//
//            return out.toByteArray();
//        } catch (Exception e) {
//            throw new RuntimeException("RSA分段处理失败", e);
//        }
//    }
//
//    /**
//     * 初始化 BouncyCastle Provider（只需执行一次）
//     */
//    public static void initBouncyCastle() {
//        if (java.security.Security.getProvider("BC") == null) {
//            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//        }
//    }
//
//    /**
//     * 生成 SM2 密钥对
//     */
//    public static java.security.KeyPair generateSm2KeyPair() {
//        try {
//            initBouncyCastle();
//            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("EC", "BC");
//            org.bouncycastle.jce.spec.ECParameterSpec ecSpec =
//                    org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("sm2p256v1");
//            generator.initialize(ecSpec, new java.security.SecureRandom());
//            return generator.generateKeyPair();
//        } catch (Exception e) {
//            throw new RuntimeException("生成SM2密钥对失败", e);
//        }
//    }
//
//    /**
//     * SM2 公钥转 Base64
//     */
//    public static String sm2PublicKeyToBase64(java.security.PublicKey publicKey) {
//        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
//    }
//
//    /**
//     * SM2 私钥转 Base64
//     */
//    public static String sm2PrivateKeyToBase64(java.security.PrivateKey privateKey) {
//        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
//    }
//
//    /**
//     * Base64 转 SM2 公钥
//     */
//    public static java.security.PublicKey loadSm2PublicKey(String base64) {
//        try {
//            initBouncyCastle();
//            byte[] bytes = Base64.getDecoder().decode(base64);
//            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(bytes);
//            java.security.KeyFactory factory = java.security.KeyFactory.getInstance("EC", "BC");
//            return factory.generatePublic(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载SM2公钥失败", e);
//        }
//    }
//
//    /**
//     * Base64 转 SM2 私钥
//     */
//    public static java.security.PrivateKey loadSm2PrivateKey(String base64) {
//        try {
//            initBouncyCastle();
//            byte[] bytes = Base64.getDecoder().decode(base64);
//            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(bytes);
//            java.security.KeyFactory factory = java.security.KeyFactory.getInstance("EC", "BC");
//            return factory.generatePrivate(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载SM2私钥失败", e);
//        }
//    }
//
//    /**
//     * SM2 公钥加密（返回 Base64）
//     */
//    public static String sm2Encrypt(String data, java.security.PublicKey publicKey) {
//        try {
//            initBouncyCastle();
//            org.bouncycastle.crypto.engines.SM2Engine engine =
//                    new org.bouncycastle.crypto.engines.SM2Engine();
//
//            org.bouncycastle.crypto.params.ECPublicKeyParameters pubKey =
//                    org.bouncycastle.crypto.util.PublicKeyFactory.createKey(publicKey.getEncoded());
//
//            engine.init(true, new org.bouncycastle.crypto.params.ParametersWithRandom(pubKey, new java.security.SecureRandom()));
//
//            byte[] result = engine.processBlock(data.getBytes(StandardCharsets.UTF_8), 0, data.length());
//            return Base64.getEncoder().encodeToString(result);
//        } catch (Exception e) {
//            throw new RuntimeException("SM2加密失败", e);
//        }
//    }
//
//    /**
//     * SM2 私钥解密
//     */
//    public static String sm2Decrypt(String data, java.security.PrivateKey privateKey) {
//        try {
//            initBouncyCastle();
//            byte[] encrypted = Base64.getDecoder().decode(data);
//
//            org.bouncycastle.crypto.engines.SM2Engine engine =
//                    new org.bouncycastle.crypto.engines.SM2Engine();
//
//            org.bouncycastle.crypto.params.ECPrivateKeyParameters priKey =
//                    org.bouncycastle.crypto.util.PrivateKeyFactory.createKey(privateKey.getEncoded());
//
//            engine.init(false, priKey);
//
//            byte[] result = engine.processBlock(encrypted, 0, encrypted.length);
//            return new String(result, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new RuntimeException("SM2解密失败", e);
//        }
//    }
//
//    /**
//     * SM2 签名
//     */
//    public static String sm2Sign(String data, java.security.PrivateKey privateKey) {
//        try {
//            initBouncyCastle();
//            java.security.Signature signature = java.security.Signature.getInstance("SM3withSM2", "BC");
//            signature.initSign(privateKey);
//            signature.update(data.getBytes(StandardCharsets.UTF_8));
//            return Base64.getEncoder().encodeToString(signature.sign());
//        } catch (Exception e) {
//            throw new RuntimeException("SM2签名失败", e);
//        }
//    }
//
//    /**
//     * SM2 验证
//     */
//    public static boolean sm2Verify(String data, String sign, java.security.PublicKey publicKey) {
//        try {
//            initBouncyCastle();
//            java.security.Signature signature = java.security.Signature.getInstance("SM3withSM2", "BC");
//            signature.initVerify(publicKey);
//            signature.update(data.getBytes(StandardCharsets.UTF_8));
//            return signature.verify(Base64.getDecoder().decode(sign));
//        } catch (Exception e) {
//            throw new RuntimeException("SM2验签失败", e);
//        }
//    }
//
//    /**
//     * SM3 摘要（字符串 -> hex）
//     */
//    public static String sm3(String data) {
//        try {
//            initBouncyCastle();
//            org.bouncycastle.crypto.digests.SM3Digest digest = new org.bouncycastle.crypto.digests.SM3Digest();
//            byte[] input = data.getBytes(StandardCharsets.UTF_8);
//            digest.update(input, 0, input.length);
//
//            byte[] result = new byte[digest.getDigestSize()];
//            digest.doFinal(result, 0);
//
//            return bytesToHex(result);
//        } catch (Exception e) {
//            throw new RuntimeException("SM3摘要失败", e);
//        }
//    }
//
//    /**
//     * SM4 加密（CBC/PKCS5）
//     */
//    public static String sm4Encrypt(String data, byte[] key, byte[] iv) {
//        try {
//            initBouncyCastle();
//            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC");
//            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, "SM4");
//            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
//
//            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec);
//            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
//
//            return Base64.getEncoder().encodeToString(encrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("SM4加密失败", e);
//        }
//    }
//
//    /**
//     * SM4 解密
//     */
//    public static String sm4Decrypt(String data, byte[] key, byte[] iv) {
//        try {
//            initBouncyCastle();
//            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC");
//            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, "SM4");
//            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
//
//            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec);
//            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
//
//            return new String(decrypted, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new RuntimeException("SM4解密失败", e);
//        }
//    }
//
//    /**
//     * byte[] -> Hex
//     */
//    public static String bytesToHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder(bytes.length * 2);
//        for (byte b : bytes) {
//            String hex = Integer.toHexString(0xff & b);
//            if (hex.length() == 1) {
//                sb.append('0');
//            }
//            sb.append(hex);
//        }
//        return sb.toString();
//    }
//
//    /**
//     * Hex -> byte[]
//     */
//    public static byte[] hexToBytes(String hex) {
//        int len = hex.length();
//        byte[] result = new byte[len / 2];
//        for (int i = 0; i < len; i += 2) {
//            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
//        }
//        return result;
//    }
//
//    /**
//     * 通用摘要（byte[]）
//     */
//    public static byte[] digest(byte[] data, String algorithm) {
//        try {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
//            return md.digest(data);
//        } catch (Exception e) {
//            throw new RuntimeException("摘要计算失败", e);
//        }
//    }
//
//    /**
//     * 通用摘要（字符串 -> hex）
//     */
//    public static String digest(String data, String algorithm) {
//        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8), algorithm));
//    }
//
//    /**
//     * 带盐摘要（字符串）
//     */
//    public static String digest(String data, String salt, String algorithm) {
//        byte[] input = (data + salt).getBytes(StandardCharsets.UTF_8);
//        return bytesToHex(digest(input, algorithm));
//    }
//
//    /**
//     * 文件摘要（hex）
//     */
//    public static String digest(java.io.File file, String algorithm) {
//        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
//            byte[] buffer = new byte[4096];
//            int len;
//            while ((len = is.read(buffer)) != -1) {
//                md.update(buffer, 0, len);
//            }
//            return bytesToHex(md.digest());
//        } catch (Exception e) {
//            throw new RuntimeException("文件摘要失败", e);
//        }
//    }
//
//    public static String md5(String data) {
//        return digest(data, "MD5");
//    }
//
//    public static String sha1(String data) {
//        return digest(data, "SHA-1");
//    }
//
//    public static String sha224(String data) {
//        return digest(data, "SHA-224");
//    }
//
//    public static String sha256(String data) {
//        return digest(data, "SHA-256");
//    }
//
//    public static String sha384(String data) {
//        return digest(data, "SHA-384");
//    }
//
//    public static String sha512(String data) {
//        return digest(data, "SHA-512");
//    }
//
//    public static byte[] md5(byte[] data) {
//        return digest(data, "MD5");
//    }
//
//    public static byte[] sha1(byte[] data) {
//        return digest(data, "SHA-1");
//    }
//
//    public static byte[] sha224(byte[] data) {
//        return digest(data, "SHA-224");
//    }
//
//    public static byte[] sha256(byte[] data) {
//        return digest(data, "SHA-256");
//    }
//
//    public static byte[] sha384(byte[] data) {
//        return digest(data, "SHA-384");
//    }
//
//    public static byte[] sha512(byte[] data) {
//        return digest(data, "SHA-512");
//    }
//
//    public static String md5(java.io.File file) {
//        return digest(file, "MD5");
//    }
//
//    public static String sha1(java.io.File file) {
//        return digest(file, "SHA-1");
//    }
//
//    public static String sha224(java.io.File file) {
//        return digest(file, "SHA-224");
//    }
//
//    public static String sha256(java.io.File file) {
//        return digest(file, "SHA-256");
//    }
//
//    public static String sha384(java.io.File file) {
//        return digest(file, "SHA-384");
//    }
//
//    public static String sha512(java.io.File file) {
//        return digest(file, "SHA-512");
//    }
//
//    public static String md5(String data, String salt) {
//        return digest(data, salt, "MD5");
//    }
//
//    public static String sha256(String data, String salt) {
//        return digest(data, salt, "SHA-256");
//    }
//
//    public static String sha512(String data, String salt) {
//        return digest(data, salt, "SHA-512");
//    }
//
//    public static String sm3Hex(String data) {
//        return sm3(data);
//    }
//
//    /**
//     * 通用 Hmac 计算（byte[]）
//     */
//    public static byte[] hmac(byte[] data, byte[] key, String algorithm) {
//        try {
//            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
//            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, algorithm);
//            mac.init(keySpec);
//            return mac.doFinal(data);
//        } catch (Exception e) {
//            throw new RuntimeException("Hmac计算失败", e);
//        }
//    }
//
//    /**
//     * 通用 Hmac（字符串 -> hex）
//     */
//    public static String hmac(String data, String key, String algorithm) {
//        byte[] result = hmac(
//                data.getBytes(StandardCharsets.UTF_8),
//                key.getBytes(StandardCharsets.UTF_8),
//                algorithm
//        );
//        return bytesToHex(result);
//    }
//
//    /**
//     * 文件 Hmac（hex）
//     */
//    public static String hmac(java.io.File file, byte[] key, String algorithm) {
//        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
//            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
//            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, algorithm);
//            mac.init(keySpec);
//
//            byte[] buffer = new byte[4096];
//            int len;
//            while ((len = is.read(buffer)) != -1) {
//                mac.update(buffer, 0, len);
//            }
//
//            return bytesToHex(mac.doFinal());
//        } catch (Exception e) {
//            throw new RuntimeException("文件Hmac计算失败", e);
//        }
//    }
//
//    public static String hmacMd5(String data, String key) {
//        return hmac(data, key, "HmacMD5");
//    }
//
//    public static String hmacSha1(String data, String key) {
//        return hmac(data, key, "HmacSHA1");
//    }
//
//    public static String hmacSha224(String data, String key) {
//        return hmac(data, key, "HmacSHA224");
//    }
//
//    public static String hmacSha256(String data, String key) {
//        return hmac(data, key, "HmacSHA256");
//    }
//
//    public static String hmacSha384(String data, String key) {
//        return hmac(data, key, "HmacSHA384");
//    }
//
//    public static String hmacSha512(String data, String key) {
//        return hmac(data, key, "HmacSHA512");
//    }
//
//    /**
//     * MAC 校验（常量时间比较，防时序攻击）
//     */
//    public static boolean verifyMac(String data, String key, String algorithm, String expectedHex) {
//        String actual = hmac(data, key, algorithm);
//        return constantTimeEquals(actual, expectedHex);
//    }
//
//    /**
//     * 常量时间比较
//     */
//    private static boolean constantTimeEquals(String a, String b) {
//        if (a == null || b == null || a.length() != b.length()) {
//            return false;
//        }
//        int result = 0;
//        for (int i = 0; i < a.length(); i++) {
//            result |= a.charAt(i) ^ b.charAt(i);
//        }
//        return result == 0;
//    }
//
//    /**
//     * MAC 校验（常量时间比较，防时序攻击）
//     */
//    public static boolean verifyMac(String data, String key, String algorithm, String expectedHex) {
//        String actual = hmac(data, key, algorithm);
//        return constantTimeEquals(actual, expectedHex);
//    }
//
//    /**
//     * 常量时间比较
//     */
//    private static boolean constantTimeEquals(String a, String b) {
//        if (a == null || b == null || a.length() != b.length()) {
//            return false;
//        }
//        int result = 0;
//        for (int i = 0; i < a.length(); i++) {
//            result |= a.charAt(i) ^ b.charAt(i);
//        }
//        return result == 0;
//    }
//
//    public static byte[] hmacSha256(byte[] data, byte[] key) {
//        return hmac(data, key, "HmacSHA256");
//    }
//
//    /**
//     * 通用签名（byte[] -> byte[]）
//     */
//    public static byte[] sign(byte[] data, java.security.PrivateKey privateKey, String algorithm) {
//        try {
//            java.security.Signature signature = java.security.Signature.getInstance(algorithm);
//            signature.initSign(privateKey);
//            signature.update(data);
//            return signature.sign();
//        } catch (Exception e) {
//            throw new RuntimeException("签名失败", e);
//        }
//    }
//
//    /**
//     * 通用验签
//     */
//    public static boolean verify(byte[] data, byte[] sign, java.security.PublicKey publicKey, String algorithm) {
//        try {
//            java.security.Signature signature = java.security.Signature.getInstance(algorithm);
//            signature.initVerify(publicKey);
//            signature.update(data);
//            return signature.verify(sign);
//        } catch (Exception e) {
//            throw new RuntimeException("验签失败", e);
//        }
//    }
//
//    /**
//     * 字符串签名（Base64）
//     */
//    public static String sign(String data, java.security.PrivateKey privateKey, String algorithm) {
//        byte[] result = sign(data.getBytes(StandardCharsets.UTF_8), privateKey, algorithm);
//        return Base64.getEncoder().encodeToString(result);
//    }
//
//    /**
//     * 字符串验签
//     */
//    public static boolean verify(String data, String sign, java.security.PublicKey publicKey, String algorithm) {
//        byte[] signBytes = Base64.getDecoder().decode(sign);
//        return verify(data.getBytes(StandardCharsets.UTF_8), signBytes, publicKey, algorithm);
//    }
//
//    public static final String SHA1_WITH_RSA = "SHA1withRSA";
//    public static final String SHA256_WITH_RSA = "SHA256withRSA";
//    public static final String SHA384_WITH_RSA = "SHA384withRSA";
//    public static final String SHA512_WITH_RSA = "SHA512withRSA";
//
//    public static String rsaSign(String data, java.security.PrivateKey privateKey, String algorithm) {
//        return sign(data, privateKey, algorithm);
//    }
//
//    public static boolean rsaVerify(String data, String sign, java.security.PublicKey publicKey, String algorithm) {
//        return verify(data, sign, publicKey, algorithm);
//    }
//
//    public static final String SHA1_WITH_DSA = "SHA1withDSA";
//    public static final String SHA256_WITH_DSA = "SHA256withDSA";
//
//    public static String dsaSign(String data, java.security.PrivateKey privateKey, String algorithm) {
//        return sign(data, privateKey, algorithm);
//    }
//
//    public static boolean dsaVerify(String data, String sign, java.security.PublicKey publicKey, String algorithm) {
//        return verify(data, sign, publicKey, algorithm);
//    }
//
//    public static final String SM3_WITH_SM2 = "SM3withSM2";
//
//    /**
//     * SM2 签名（字符串）
//     */
//    public static String sm2SignStd(String data, java.security.PrivateKey privateKey) {
//        try {
//            initBouncyCastle();
//            java.security.Signature signature = java.security.Signature.getInstance(SM3_WITH_SM2, "BC");
//            signature.initSign(privateKey);
//            signature.update(data.getBytes(StandardCharsets.UTF_8));
//            return Base64.getEncoder().encodeToString(signature.sign());
//        } catch (Exception e) {
//            throw new RuntimeException("SM2签名失败", e);
//        }
//    }
//
//    /**
//     * SM2 验签
//     */
//    public static boolean sm2VerifyStd(String data, String sign, java.security.PublicKey publicKey) {
//        try {
//            initBouncyCastle();
//            java.security.Signature signature = java.security.Signature.getInstance(SM3_WITH_SM2, "BC");
//            signature.initVerify(publicKey);
//            signature.update(data.getBytes(StandardCharsets.UTF_8));
//            return signature.verify(Base64.getDecoder().decode(sign));
//        } catch (Exception e) {
//            throw new RuntimeException("SM2验签失败", e);
//        }
//    }
//
//    public static byte[] rsaSign(byte[] data, java.security.PrivateKey privateKey, String algorithm) {
//        return sign(data, privateKey, algorithm);
//    }
//
//    public static boolean rsaVerify(byte[] data, byte[] sign, java.security.PublicKey publicKey, String algorithm) {
//        return verify(data, sign, publicKey, algorithm);
//    }
//
//    /**
//     * 生成对称密钥（AES / DES / DESede / SM4）
//     */
//    public static byte[] generateSymmetricKey(String algorithm, int keySize) {
//        try {
//            javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(algorithm);
//            keyGenerator.init(keySize);
//            return keyGenerator.generateKey().getEncoded();
//        } catch (Exception e) {
//            throw new RuntimeException("生成对称密钥失败", e);
//        }
//    }
//
//    /**
//     * 生成非对称密钥对（RSA / DSA）
//     */
//    public static java.security.KeyPair generateKeyPair(String algorithm, int keySize) {
//        try {
//            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance(algorithm);
//            generator.initialize(keySize);
//            return generator.generateKeyPair();
//        } catch (Exception e) {
//            throw new RuntimeException("生成密钥对失败", e);
//        }
//    }
//
//    /**
//     * byte[] -> 对称密钥
//     */
//    public static javax.crypto.SecretKey toSecretKey(byte[] key, String algorithm) {
//        return new javax.crypto.spec.SecretKeySpec(key, algorithm);
//    }
//
//    /**
//     * Base64 -> 对称密钥
//     */
//    public static javax.crypto.SecretKey toSecretKey(String base64Key, String algorithm) {
//        return toSecretKey(Base64.getDecoder().decode(base64Key), algorithm);
//    }
//
//    /**
//     * 对称密钥 -> Base64
//     */
//    public static String secretKeyToBase64(javax.crypto.SecretKey key) {
//        return Base64.getEncoder().encodeToString(key.getEncoded());
//    }
//
//    /**
//     * Key -> byte[]
//     */
//    public static byte[] keyToBytes(java.security.Key key) {
//        return key.getEncoded();
//    }
//
//    /**
//     * Base64 -> 公钥（通用）
//     */
//    public static java.security.PublicKey loadPublicKey(String base64Key, String algorithm) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(base64Key);
//            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(bytes);
//            java.security.KeyFactory factory = java.security.KeyFactory.getInstance(algorithm);
//            return factory.generatePublic(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载公钥失败", e);
//        }
//    }
//
//    /**
//     * Base64 -> 私钥（通用）
//     */
//    public static java.security.PrivateKey loadPrivateKey(String base64Key, String algorithm) {
//        try {
//            byte[] bytes = Base64.getDecoder().decode(base64Key);
//            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(bytes);
//            java.security.KeyFactory factory = java.security.KeyFactory.getInstance(algorithm);
//            return factory.generatePrivate(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("加载私钥失败", e);
//        }
//    }
//
//    /**
//     * 公钥 -> Base64
//     */
//    public static String publicKeyToBase64(java.security.PublicKey key) {
//        return Base64.getEncoder().encodeToString(key.getEncoded());
//    }
//
//    /**
//     * 私钥 -> Base64
//     */
//    public static String privateKeyToBase64(java.security.PrivateKey key) {
//        return Base64.getEncoder().encodeToString(key.getEncoded());
//    }
//
//    /**
//     * Base64 -> PEM（公钥）
//     */
//    public static String toPemPublicKey(String base64) {
//        return "-----BEGIN PUBLIC KEY-----\n"
//                + wrapPem(base64)
//                + "\n-----END PUBLIC KEY-----";
//    }
//
//    /**
//     * Base64 -> PEM（私钥）
//     */
//    public static String toPemPrivateKey(String base64) {
//        return "-----BEGIN PRIVATE KEY-----\n"
//                + wrapPem(base64)
//                + "\n-----END PRIVATE KEY-----";
//    }
//
//    /**
//     * PEM -> Base64（去头尾）
//     */
//    public static String fromPem(String pem) {
//        return pem.replaceAll("-----\\w+ PUBLIC KEY-----", "")
//                .replaceAll("-----\\w+ PRIVATE KEY-----", "")
//                .replaceAll("\\s", "");
//    }
//
//    /**
//     * PEM 64列换行
//     */
//    private static String wrapPem(String base64) {
//        StringBuilder sb = new StringBuilder();
//        int i = 0;
//        while (i < base64.length()) {
//            int end = Math.min(i + 64, base64.length());
//            sb.append(base64, i, end).append("\n");
//            i = end;
//        }
//        return sb.toString().trim();
//    }
//
//    /**
//     * 校验对称密钥长度
//     */
//    public static void checkSymmetricKey(byte[] key, String algorithm) {
//        if (key == null) {
//            throw new IllegalArgumentException("密钥不能为空");
//        }
//
//        int len = key.length;
//
//        switch (algorithm) {
//            case "AES":
//                if (len != 16 && len != 24 && len != 32) {
//                    throw new IllegalArgumentException("AES密钥长度必须为16/24/32字节");
//                }
//                break;
//            case "DES":
//                if (len != 8) {
//                    throw new IllegalArgumentException("DES密钥长度必须为8字节");
//                }
//                break;
//            case "DESede":
//                if (len != 24) {
//                    throw new IllegalArgumentException("3DES密钥长度必须为24字节");
//                }
//                break;
//            case "SM4":
//                if (len != 16) {
//                    throw new IllegalArgumentException("SM4密钥长度必须为16字节");
//                }
//                break;
//            default:
//                throw new IllegalArgumentException("不支持的算法: " + algorithm);
//        }
//    }
//
//    /**
//     * 生成随机密钥（指定字节长度）
//     */
//    public static byte[] randomKey(int length) {
//        byte[] key = new byte[length];
//        new java.security.SecureRandom().nextBytes(key);
//        return key;
//    }
//
//    /**
//     * 生成随机IV
//     */
//    public static byte[] randomIv(int length) {
//        return randomKey(length);
//    }
//
//    /**
//     * 密钥 -> Hex
//     */
//    public static String keyToHex(java.security.Key key) {
//        return bytesToHex(key.getEncoded());
//    }
//
//    /**
//     * Hex -> 密钥
//     */
//    public static javax.crypto.SecretKey hexToSecretKey(String hex, String algorithm) {
//        return new javax.crypto.spec.SecretKeySpec(hexToBytes(hex), algorithm);
//    }
//
//    /**
//     * 编码与数据转换 + 通用能力 + 扩展能力
//     *
//     * @author Ateng
//     * @since 2026-04-21
//     */
//
//    /**
//     * byte[] -> Hex
//     */
//    public static String toHex(byte[] bytes) {
//        if (bytes == null) {
//            return null;
//        }
//        StringBuilder sb = new StringBuilder(bytes.length * 2);
//        for (byte b : bytes) {
//            String hex = Integer.toHexString(0xff & b);
//            if (hex.length() == 1) {
//                sb.append('0');
//            }
//            sb.append(hex);
//        }
//        return sb.toString();
//    }
//
//    /**
//     * Hex -> byte[]
//     */
//    public static byte[] fromHex(String hex) {
//        if (isEmpty(hex)) {
//            return null;
//        }
//        int len = hex.length();
//        byte[] result = new byte[len / 2];
//        for (int i = 0; i < len; i += 2) {
//            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
//        }
//        return result;
//    }
//
//    /**
//     * Base64 编码
//     */
//    public static String toBase64(byte[] data) {
//        if (data == null) {
//            return null;
//        }
//        return java.util.Base64.getEncoder().encodeToString(data);
//    }
//
//    /**
//     * Base64 解码
//     */
//    public static byte[] fromBase64(String base64) {
//        if (isEmpty(base64)) {
//            return null;
//        }
//        return java.util.Base64.getDecoder().decode(base64);
//    }
//
//    /**
//     * String -> byte[]
//     */
//    public static byte[] toBytes(String data) {
//        if (data == null) {
//            return null;
//        }
//        return data.getBytes(DEFAULT_CHARSET);
//    }
//
//    /**
//     * byte[] -> String
//     */
//    public static String toString(byte[] bytes) {
//        if (bytes == null) {
//            return null;
//        }
//        return new String(bytes, DEFAULT_CHARSET);
//    }
//
//    /**
//     * Key -> Base64
//     */
//    public static String keyToString(java.security.Key key) {
//        if (key == null) {
//            return null;
//        }
//        return toBase64(key.getEncoded());
//    }
//
//    /**
//     * Base64 -> SecretKey
//     */
//    public static javax.crypto.SecretKey stringToSecretKey(String base64Key, String algorithm) {
//        byte[] bytes = fromBase64(base64Key);
//        return new javax.crypto.spec.SecretKeySpec(bytes, algorithm);
//    }
//
//    /**
//     * 判断算法是否可用
//     */
//    public static boolean isAlgorithmSupported(String algorithm) {
//        try {
//            java.security.Security.getAlgorithms("Cipher").contains(algorithm);
//            javax.crypto.Cipher.getInstance(algorithm);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /**
//     * 统一异常包装
//     */
//    public static RuntimeException wrap(Throwable e, String message) {
//        return new RuntimeException(message, e);
//    }
//
//    /**
//     * 空值判断
//     */
//    public static boolean isEmpty(String str) {
//        return str == null || str.trim().isEmpty();
//    }
//
//    /**
//     * 空值保护
//     */
//    public static String safe(String str) {
//        return str == null ? "" : str;
//    }
//
//    /**
//     * 输入校验
//     */
//    public static void requireNonNull(Object obj, String message) {
//        if (obj == null) {
//            throw new IllegalArgumentException(message);
//        }
//    }
//
//    /**
//     * 多段数据处理（合并 byte[]）
//     */
//    public static byte[] merge(byte[]... arrays) {
//        if (arrays == null) {
//            return null;
//        }
//        int total = 0;
//        for (byte[] arr : arrays) {
//            if (arr != null) {
//                total += arr.length;
//            }
//        }
//        byte[] result = new byte[total];
//        int pos = 0;
//        for (byte[] arr : arrays) {
//            if (arr != null) {
//                System.arraycopy(arr, 0, result, pos, arr.length);
//                pos += arr.length;
//            }
//        }
//        return result;
//    }
//
//    /**
//     * 生成随机盐值
//     */
//    public static byte[] randomSalt(int length) {
//        byte[] salt = new byte[length];
//        new java.security.SecureRandom().nextBytes(salt);
//        return salt;
//    }
//
//    /**
//     * 摘要加盐（通用）
//     */
//    public static String digestWithSalt(String data, byte[] salt, String algorithm) {
//        byte[] merged = merge(toBytes(data), salt);
//        return toHex(digest(merged, algorithm));
//    }
//
//    /**
//     * 加密后 Base64 输出
//     */
//    public static String encryptToBase64(byte[] data, String algorithm, byte[] key, byte[] iv) {
//        byte[] encrypted = encrypt(data, algorithm, key, iv);
//        return toBase64(encrypted);
//    }
//
//    /**
//     * Base64 输入解密
//     */
//    public static byte[] decryptFromBase64(String base64, String algorithm, byte[] key, byte[] iv) {
//        byte[] decoded = fromBase64(base64);
//        return decrypt(decoded, algorithm, key, iv);
//    }
//
//    /**
//     * 签名输出 Hex
//     */
//    public static String signToHex(byte[] data, java.security.PrivateKey key, String algorithm) {
//        return toHex(sign(data, key, algorithm));
//    }
//
//    /**
//     * 签名输出 Base64
//     */
//    public static String signToBase64(byte[] data, java.security.PrivateKey key, String algorithm) {
//        return toBase64(sign(data, key, algorithm));
//    }
//
//    /**
//     * 文本签名（快捷）
//     */
//    public static String signText(String data, java.security.PrivateKey key, String algorithm) {
//        return sign(data, key, algorithm);
//    }
//
//    /**
//     * 文件签名
//     */
//    public static String signFile(java.io.File file, java.security.PrivateKey key, String algorithm) {
//        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
//            java.security.Signature signature = java.security.Signature.getInstance(algorithm);
//            signature.initSign(key);
//
//            byte[] buffer = new byte[4096];
//            int len;
//            while ((len = is.read(buffer)) != -1) {
//                signature.update(buffer, 0, len);
//            }
//            return toBase64(signature.sign());
//        } catch (Exception e) {
//            throw wrap(e, "文件签名失败");
//        }
//    }
//
//    /**
//     * 流签名
//     */
//    public static String signStream(java.io.InputStream is, java.security.PrivateKey key, String algorithm) {
//        try {
//            java.security.Signature signature = java.security.Signature.getInstance(algorithm);
//            signature.initSign(key);
//
//            byte[] buffer = new byte[4096];
//            int len;
//            while ((len = is.read(buffer)) != -1) {
//                signature.update(buffer, 0, len);
//            }
//            return toBase64(signature.sign());
//        } catch (Exception e) {
//            throw wrap(e, "流签名失败");
//        }
//    }
//
//    /**
//     * 支持配置化算法（简单策略入口）
//     */
//    public static String encryptWithAlgorithm(String data, String algorithm, byte[] key, byte[] iv) {
//        return encrypt(data, algorithm, new String(key, DEFAULT_CHARSET), iv);
//    }
//
//    public static String decryptWithAlgorithm(String data, String algorithm, byte[] key, byte[] iv) {
//        return decrypt(data, algorithm, new String(key, DEFAULT_CHARSET), iv);
//    }
//
//}
