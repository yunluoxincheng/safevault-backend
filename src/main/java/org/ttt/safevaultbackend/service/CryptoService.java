package org.ttt.safevaultbackend.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密服务
 */
@Service
public class CryptoService {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final String RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding";
    private static final int GCM_TAG_LENGTH = 128; // 位
    private static final int GCM_IV_LENGTH = 12;   // 字节
    private static final int AES_KEY_SIZE = 256;   // 位

    // 签名时间戳有效期（毫秒）
    private static final long SIGNATURE_TIMESTAMP_VALIDITY = 5 * 60 * 1000; // 5分钟

    /**
     * 生成用户密钥对（RSA）
     */
    public KeyPair generateUserKeyPair() throws Exception {
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 生成会话密钥（AES-256）
     */
    public SecretKey generateSessionKey() throws NoSuchAlgorithmException {
        javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * 使用接收方公钥加密会话密钥
     */
    public String encryptSessionKey(SecretKey sessionKey, PublicKey receiverPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey);
        byte[] encryptedKey = cipher.doFinal(sessionKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * 使用接收方私钥解密会话密钥
     */
    public SecretKey decryptSessionKey(String encryptedSessionKey, PrivateKey receiverPrivateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, receiverPrivateKey);
        byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedSessionKey));
        return new SecretKeySpec(decryptedKey, "AES");
    }

    /**
     * 使用会话密钥加密数据
     * 返回格式：Base64(IV + ciphertext)
     */
    public String encryptData(String plaintext, SecretKey sessionKey) throws Exception {
        // 生成随机 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 加密
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 组合 IV 和密文
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * 使用会话密钥解密数据
     * 输入格式：Base64(IV + ciphertext)
     */
    public String decryptData(String encryptedData, SecretKey sessionKey) throws Exception {
        // 解码 Base64
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);

        // 提取 IV 和密文
        byte[] iv = Arrays.copyOfRange(decodedData, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(decodedData, GCM_IV_LENGTH, decodedData.length);

        // 解密
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, parameterSpec);
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * 生成分享签名（HMAC-SHA256）
     */
    public String generateShareSignature(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * 验证分享签名
     */
    public boolean verifyShareSignature(String data, String signature, String secret) throws Exception {
        String expectedSignature = generateShareSignature(data, secret);
        return expectedSignature.equals(signature);
    }

    /**
     * 使用公钥验证签名
     * 
     * @param data 原始数据
     * @param signature Base64编码的签名
     * @param publicKey 用户公钥
     * @return 验证是否成功
     */
    public boolean verifySignature(String data, String signature, String publicKey) {
        try {
            // 解码公钥
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            // 验证签名
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证签名和时间戳
     * 
     * @param data 原始数据
     * @param signature Base64编码的签名
     * @param publicKey 用户公钥（Base64编码）
     * @param timestamp 时间戳（毫秒）
     * @return 验证是否成功
     */
    public boolean verifySignatureWithTimestamp(String data, String signature, String publicKey, Long timestamp) {
        // 验证时间戳
        if (timestamp == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > SIGNATURE_TIMESTAMP_VALIDITY) {
            return false;
        }

        // 验证签名
        return verifySignature(data, signature, publicKey);
    }
}
