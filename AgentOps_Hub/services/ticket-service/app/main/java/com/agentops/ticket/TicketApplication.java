package com.agentops.ticket;

import java.time.Clock;

public final class TicketApplication {
  private TicketApplication() {}

  public static void main(String[] args) {
    TicketConfig config = TicketConfig.fromEnvironment();
    H2TicketRepository repository = new H2TicketRepository(config.dbUrl());
    repository.initialize();
    TicketAuthService authService = new TicketAuthService(new JwtVerifier(config.jwtSecret(), Clock.systemUTC()));
    TicketService ticketService = new TicketService(repository, Clock.systemUTC());
    int port = new TicketHttpServer(authService, ticketService).start(config.port());
    System.out.println("ticket-service listening on " + port);
  }
}
