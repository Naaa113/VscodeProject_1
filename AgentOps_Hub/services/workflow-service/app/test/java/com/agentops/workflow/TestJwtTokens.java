package com.agentops.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class TestJwtTokens {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private TestJwtTokens() {}

  static String issue(String secret, String tenantId, String userId, List<String> permissions, Instant expiresAt) {
    Instant issuedAt = Instant.now();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload =
        Map.of(
            "sub", userId,
            "tenant_id", tenantId,
            "roles", List.of("operator"),
            "permissions", permissions,
            "iat", issuedAt.getEpochSecond(),
            "exp", expiresAt.getEpochSecond(),
            "jti", UUID.randomUUID().toString());
    String signingInput = encode(header) + "." + encode(payload);
    return signingInput + "." + sign(secret, signingInput);
  }

  private static String encode(Map<String, Object> value) {
    try {
      return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(value));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static String sign(String secret, String signingInput) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
