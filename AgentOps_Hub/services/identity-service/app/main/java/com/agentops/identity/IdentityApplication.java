package com.agentops.identity;

import java.time.Clock;

public final class IdentityApplication {
  private IdentityApplication() {}

  public static void main(String[] args) {
    IdentityConfig config = IdentityConfig.fromEnvironment();
    PasswordHasher passwordHasher = new PasswordHasher();
    H2IdentityRepository repository = new H2IdentityRepository(config.dbUrl());
    repository.initialize();
    JwtService jwtService = new JwtService(config, Clock.systemUTC());
    AuthService authService = new AuthService(repository, passwordHasher, jwtService);
    int port = new IdentityHttpServer(authService).start(config.port());
    System.out.println("identity-service listening on " + port);
  }
}
