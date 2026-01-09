package org.ttt.safevaultbackend.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
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

    /**
     * 生成用户密钥对（ECDH）
     */
    public KeyPair generateUserKeyPair() throws Exception {
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(521); // 使用 P-521 曲线
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
}
