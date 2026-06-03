package com.agentops.identity;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordHasher {
  private static final int ITERATIONS = 120_000;
  private static final int KEY_LENGTH = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  String hash(char[] secret) {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    byte[] hash = pbkdf2(secret, salt, ITERATIONS);
    return "pbkdf2_sha256$"
        + ITERATIONS
        + "$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(hash);
  }

  boolean verify(char[] secret, String storedHash) {
    String[] parts = storedHash.split("\\$");
    if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
      return false;
    }
    int iterations = Integer.parseInt(parts[1]);
    byte[] salt = Base64.getDecoder().decode(parts[2]);
    byte[] expected = Base64.getDecoder().decode(parts[3]);
    byte[] actual = pbkdf2(secret, salt, iterations);
    if (actual.length != expected.length) {
      return false;
    }
    int diff = 0;
    for (int index = 0; index < actual.length; index++) {
      diff |= actual[index] ^ expected[index];
    }
    return diff == 0;
  }

  private byte[] pbkdf2(char[] secret, byte[] salt, int iterations) {
    try {
      PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, KEY_LENGTH);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }
}
