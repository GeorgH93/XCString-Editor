package com.xcstring.editor.util;

import java.security.SecureRandom;

public class SecureTokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte bv : b) {
            sb.append(String.format("%02x", bv));
        }
        return sb.toString();
    }
}
