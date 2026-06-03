package com.agentops.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TicketServiceTest {
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), ZoneOffset.UTC);

  @Test
  void createTicketPersistsCustomerTenantAuditAndOpenStatus() {
    Fixture fixture = fixture();
    RequestContext context = context("tenant_demo", "user_demo");

    TicketDetailResponse created = fixture.service.create(context, createRequest("complaint", "normal"));

    assertEquals("tenant_demo", created.tenant_id());
    assertEquals("open", created.status().name());
    assertEquals("normal", created.priority().name());
    assertEquals("complaint", created.category());
    assertEquals("Example Customer", created.customer().display_name());
    assertEquals("user_demo", created.audit().created_by());
    assertEquals(1, fixture.repository.auditCount("TICKET_CREATE", true));
  }

  @Test
  void listTicketsFiltersByStatusPriorityCategoryAndTimeRange() {
    Fixture fixture = fixture();
    RequestContext context = context("tenant_demo", "user_demo");
    fixture.service.create(context, createRequest("complaint", "normal"));
    fixture.service.create(context, createRequest("billing", "high"));

    TicketListResponse response =
        fixture.service.list(
            context,
            TicketService.filter(
                TicketStatus.open,
                TicketPriority.normal,
                "complaint",
                null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-02T00:00:00Z"),
                Instant.parse("2026-06-03T00:00:00Z"),
                "delayed",
                1,
                20));

    assertEquals(1, response.page().total());
    assertEquals("complaint", response.items().get(0).category());
    assertEquals(1, fixture.repository.auditCount("TICKET_LIST", true));
  }

  @Test
  void detailsAreTenantScopedAndCrossTenantLooksNotFound() {
    Fixture fixture = fixture();
    TicketDetailResponse created = fixture.service.create(context("tenant_a", "user_a"), createRequest("complaint", "urgent"));

    TicketException exception =
        assertThrows(
            TicketException.class,
            () -> fixture.service.get(context("tenant_b", "user_b"), created.ticket_id()));

    assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.code());
    assertEquals(1, fixture.repository.auditCount("TICKET_GET", false));
  }

  @Test
  void createRequiresExactlyOneCustomerSource() {
    Fixture fixture = fixture();
    RequestContext context = context("tenant_demo", "user_demo");
    CreateTicketRequest invalid =
        new CreateTicketRequest(
            "Title",
            "Description",
            TicketPriority.normal,
            "complaint",
            null,
            null,
            Instant.parse("2026-06-02T08:00:00Z"));

    TicketException exception = assertThrows(TicketException.class, () -> fixture.service.create(context, invalid));

    assertEquals(ErrorCode.VALIDATION_FAILED, exception.code());
    assertTrue(fixture.repository.auditCount("TICKET_CREATE", false) > 0);
  }

  private static CreateTicketRequest createRequest(String category, String priority) {
    return new CreateTicketRequest(
        "Delayed response",
        "Customer reported a delayed response.",
        TicketPriority.valueOf(priority),
        category,
        null,
        new CreateCustomerInput("Example Customer", "crm-1001"),
        Instant.parse("2026-06-02T08:00:00Z"));
  }

  private static RequestContext context(String tenantId, String userId) {
    return new RequestContext(tenantId, userId, List.of("operator"), List.of("ticket:read", "ticket:create"), "trace-test");
  }

  private static Fixture fixture() {
    H2TicketRepository repository =
        new H2TicketRepository("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    repository.initialize();
    return new Fixture(repository, new TicketService(repository, CLOCK));
  }

  private record Fixture(H2TicketRepository repository, TicketService service) {}
}
