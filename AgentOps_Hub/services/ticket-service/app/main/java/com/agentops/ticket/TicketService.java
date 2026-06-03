package com.agentops.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

final class TicketService {
  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;
  private final H2TicketRepository repository;
  private final Clock clock;

  TicketService(H2TicketRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  TicketDetailResponse create(RequestContext context, CreateTicketRequest request) {
    try {
      validateCreate(request);
      Instant now = clock.instant();
      CustomerRecord customer = resolveCustomer(context, request, now);
      TicketRecord ticket =
          repository.createTicket(context.tenantId(), customer, request, context.userId(), now);
      repository.audit(context.tenantId(), context.userId(), ticket.id(), "TICKET_CREATE", true, null, context.traceId());
      return TicketDetailResponse.from(ticket);
    } catch (TicketException ex) {
      repository.audit(context.tenantId(), context.userId(), null, "TICKET_CREATE", false, ex.code(), context.traceId());
      throw ex;
    }
  }

  TicketListResponse list(RequestContext context, TicketSearchFilter filter) {
    try {
      validateFilter(filter);
      List<TicketRecord> all = repository.searchTickets(context.tenantId(), filter);
      int from = Math.min((filter.page() - 1) * filter.pageSize(), all.size());
      int to = Math.min(from + filter.pageSize(), all.size());
      List<TicketSummaryResponse> items = all.subList(from, to).stream().map(TicketSummaryResponse::from).toList();
      repository.audit(context.tenantId(), context.userId(), null, "TICKET_LIST", true, null, context.traceId());
      return new TicketListResponse(items, new PageMeta(filter.page(), filter.pageSize(), all.size()));
    } catch (TicketException ex) {
      repository.audit(context.tenantId(), context.userId(), null, "TICKET_LIST", false, ex.code(), context.traceId());
      throw ex;
    }
  }

  TicketDetailResponse get(RequestContext context, String ticketId) {
    try {
      if (isBlank(ticketId)) {
        throw new TicketException(ErrorCode.VALIDATION_FAILED);
      }
      TicketRecord ticket =
          repository.findTicket(context.tenantId(), ticketId).orElseThrow(() -> new TicketException(ErrorCode.RESOURCE_NOT_FOUND));
      repository.audit(context.tenantId(), context.userId(), ticket.id(), "TICKET_GET", true, null, context.traceId());
      return TicketDetailResponse.from(ticket);
    } catch (TicketException ex) {
      repository.audit(context.tenantId(), context.userId(), ticketId, "TICKET_GET", false, ex.code(), context.traceId());
      throw ex;
    }
  }

  static TicketSearchFilter filter(
      TicketStatus status,
      TicketPriority priority,
      String category,
      String customerId,
      Instant createdFrom,
      Instant createdTo,
      Instant slaDueBefore,
      String query,
      Integer page,
      Integer pageSize) {
    return new TicketSearchFilter(
        status,
        priority,
        blankToNull(category),
        blankToNull(customerId),
        createdFrom,
        createdTo,
        slaDueBefore,
        blankToNull(query),
        page == null ? DEFAULT_PAGE : page,
        pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
  }

  private CustomerRecord resolveCustomer(RequestContext context, CreateTicketRequest request, Instant now) {
    if (!isBlank(request.customer_id())) {
      return repository
          .findCustomer(context.tenantId(), request.customer_id())
          .orElseThrow(() -> new TicketException(ErrorCode.VALIDATION_FAILED));
    }
    return repository.createCustomer(context.tenantId(), request.customer(), context.userId(), now);
  }

  private static void validateCreate(CreateTicketRequest request) {
    if (request == null
        || isBlank(request.title())
        || isBlank(request.description())
        || request.priority() == null
        || isBlank(request.category())) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED);
    }
    boolean hasCustomerId = !isBlank(request.customer_id());
    boolean hasCustomerInput = request.customer() != null && !isBlank(request.customer().display_name());
    if (hasCustomerId == hasCustomerInput) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED);
    }
    if (request.sla_due_at() != null && request.sla_due_at().isBefore(Instant.EPOCH)) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private static void validateFilter(TicketSearchFilter filter) {
    if (filter.page() < 1 || filter.pageSize() < 1 || filter.pageSize() > MAX_PAGE_SIZE) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED);
    }
    if (filter.createdFrom() != null && filter.createdTo() != null && filter.createdFrom().isAfter(filter.createdTo())) {
      throw new TicketException(ErrorCode.VALIDATION_FAILED);
    }
  }

  private static String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
