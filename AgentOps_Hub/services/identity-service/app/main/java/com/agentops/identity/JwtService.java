package com.agentops.identity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class JwtService {
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final byte[] secret;
  private final Clock clock;
  private final IdentityConfig config;

  JwtService(IdentityConfig config, Clock clock) {
    this.secret = config.jwtSecret().getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
    this.config = config;
  }

  TokenIssue issue(UserRecord user, List<String> roles, List<String> permissions) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(config.tokenTtl());
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload =
        Map.of(
            "sub", user.id(),
            "tenant_id", user.tenantId(),
            "roles", roles,
            "permissions", permissions,
            "iat", issuedAt.getEpochSecond(),
            "exp", expiresAt.getEpochSecond(),
            "jti", UUID.randomUUID().toString());
    String signingInput = encode(header) + "." + encode(payload);
    return new TokenIssue(signingInput + "." + sign(signingInput), expiresAt);
  }

  JwtClaims verify(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
    }
    String signingInput = parts[0] + "." + parts[1];
    if (!MessageDigestSupport.constantTimeEquals(sign(signingInput), parts[2])) {
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
    }
    try {
      Map<String, Object> payload =
          MAPPER.readValue(URL_DECODER.decode(parts[1]), new TypeReference<Map<String, Object>>() {});
      Instant expiresAt = Instant.ofEpochSecond(number(payload.get("exp")).longValue());
      if (!expiresAt.isAfter(clock.instant())) {
        throw new IdentityException(ErrorCode.AUTH_TOKEN_EXPIRED);
      }
      Instant issuedAt = Instant.ofEpochSecond(number(payload.get("iat")).longValue());
      return new JwtClaims(
          string(payload.get("sub")),
          string(payload.get("tenant_id")),
          stringList(payload.get("roles")),
          stringList(payload.get("permissions")),
          issuedAt,
          expiresAt,
          string(payload.get("jti")));
    } catch (IdentityException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IdentityException(ErrorCode.AUTH_FORBIDDEN, ex);
    }
  }

  private String encode(Map<String, Object> value) {
    try {
      return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(value));
    } catch (Exception ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private String sign(String signingInput) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IdentityException(ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  private static Number number(Object value) {
    if (value instanceof Number number) {
      return number;
    }
    throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
  }

  private static String string(Object value) {
    if (value instanceof String text && !text.isBlank()) {
      return text;
    }
    throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
  }

  private static List<String> stringList(Object value) {
    if (value instanceof List<?> list) {
      return list.stream().map(JwtService::string).toList();
    }
    throw new IdentityException(ErrorCode.AUTH_FORBIDDEN);
  }
}

record TokenIssue(String accessToken, Instant expiresAt) {}

final class MessageDigestSupport {
  private MessageDigestSupport() {}

  static boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
    if (leftBytes.length != rightBytes.length) {
      return false;
    }
    int diff = 0;
    for (int index = 0; index < leftBytes.length; index++) {
      diff |= leftBytes[index] ^ rightBytes[index];
    }
    return diff == 0;
  }
}
