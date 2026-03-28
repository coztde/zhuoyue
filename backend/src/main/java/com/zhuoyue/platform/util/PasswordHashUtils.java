package com.zhuoyue.platform.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 密码哈希工具。
 * 当前不引入额外安全框架，直接使用 JDK 自带的 PBKDF2 做密码摘要，
 * 既能满足“默认密码不能明文落库”的要求，也能保持依赖简单稳定。
 */
public final class PasswordHashUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 65536;

    private static final int KEY_LENGTH = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHashUtils() {
    }

    public static String hashPassword(String rawPassword) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS, KEY_LENGTH);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equalsIgnoreCase(parts[0])) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2].getBytes(StandardCharsets.UTF_8));
        byte[] expected = Base64.getDecoder().decode(parts[3].getBytes(StandardCharsets.UTF_8));
        byte[] actual = pbkdf2(rawPassword, salt, iterations, expected.length * 8);

        if (expected.length != actual.length) {
            return false;
        }

        int result = 0;
        for (int index = 0; index < expected.length; index++) {
            result |= expected[index] ^ actual[index];
        }
        return result == 0;
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec keySpec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(keySpec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("密码摘要计算失败", exception);
        }
    }
}
