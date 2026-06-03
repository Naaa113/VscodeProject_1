import { describe, expect, it } from "vitest";
import { ApiError, type SessionContext } from "../api/clients";
import { MockAiTaskClient, MockAuthClient, MockTicketClient } from "./mockClients";

const session: SessionContext = {
  accessToken: "mock-web-console-token",
  tenantId: "tenant_demo",
  traceId: "trace_test"
};

describe("Phase 006 mock clients", () => {
  it("authenticates the contract-shaped mock user", async () => {
    const auth = new MockAuthClient();
    const login = await auth.login({
      tenant_id: "tenant_demo",
      username: "demo.user",
      password: "demo-password"
    });
    const user = await auth.currentUser({ accessToken: login.access_token, tenantId: login.tenant_id, traceId: "trace_test" });

    expect(user.permissions).toContain("ticket:read");
    expect(login.access_token).toContain("mock");
  });

  it("returns empty ticket state from query without inventing private fields", async () => {
    const tickets = new MockTicketClient();
    const response = await tickets.listTickets(session, "no-match");

    expect(response.items).toHaveLength(0);
    expect(response.page.total).toBe(0);
  });

  it("surfaces resource not found as a contract error", async () => {
    const tickets = new MockTicketClient();

    await expect(tickets.getTicket(session, "missing")).rejects.toMatchObject({
      error_code: "RESOURCE_NOT_FOUND",
      retryable: false
    } satisfies Partial<ApiError>);
  });

  it("keeps high-risk agent output in waiting approval without action commands", async () => {
    const aiTasks = new MockAiTaskClient();
    const task = await aiTasks.createTask(session, {
      task_type: "complaint_analysis",
      prompt: "Analyze complaints",
      priority: "high"
    });
    const steps = await aiTasks.listSteps(session, task.task_id);
    const report = await aiTasks.getReport(session, task.task_id);

    expect(task.status).toBe("waiting_approval");
    expect(task.approval_instance_id).toBeNull();
    expect(steps.items.some((step) => step.status === "waiting_human")).toBe(true);
    expect(report.citations.every((citation) => citation.source_uri?.startsWith("mock://"))).toBe(true);
    expect(report.citations.every((citation) => typeof citation.chunk_id === "string" && citation.chunk_id.length > 0)).toBe(true);
    expect(report.citations.every((citation) => !("mock_source" in citation))).toBe(true);
  });
});
