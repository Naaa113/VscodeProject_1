import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import type { AgentStep, AiTask, ErrorResponse } from "../api/contracts";
import { ReportPanel, ReportPreview, StepTimeline } from "./App";
import { mockAgentSteps, mockReport, mockWaitingApprovalTask } from "../mocks/mockFixtures";

const resourceNotFoundError: ErrorResponse = {
  error_code: "RESOURCE_NOT_FOUND",
  message: "Report was not found or is not accessible.",
  trace_id: "trace_report_missing",
  retryable: false
};

describe("Phase 006 app review fixes", () => {
  it("renders report risks from task blocking state and waiting-human steps", () => {
    const markup = renderToStaticMarkup(
      <ReportPreview report={mockReport} task={mockWaitingApprovalTask} steps={mockAgentSteps} />
    );

    expect(markup).toContain("风险");
    expect(markup).toContain("Mock risk policy requires human confirmation before any follow-up action.");
    expect(markup).toContain("Detected urgent SLA and customer-impact risk. No action command was created.");
    expect(markup).toContain("chunk_mock_01");
    expect(markup).toContain("Mock");
  });

  it("renders step error details for failed steps", () => {
    const failedStep: AgentStep = {
      task_id: "task_failed",
      run_id: "run_failed",
      step_id: "step_failed",
      agent_name: "Retriever",
      step_name: "Fetch knowledge",
      status: "failed",
      started_at: "2026-06-01T08:10:04Z",
      finished_at: "2026-06-01T08:10:08Z",
      error: {
        error_code: "DOWNSTREAM_UNAVAILABLE",
        message: "Knowledge search is temporarily unavailable.",
        trace_id: "trace_failed_step",
        retryable: true
      }
    };

    const markup = renderToStaticMarkup(<StepTimeline steps={[failedStep]} />);

    expect(markup).toContain("DOWNSTREAM_UNAVAILABLE");
    expect(markup).toContain("Knowledge search is temporarily unavailable.");
    expect(markup).toContain("可重试");
  });

  it("renders waiting approval report state separately from generic errors", () => {
    const markup = renderToStaticMarkup(
      <ReportPanel
        task={mockWaitingApprovalTask}
        steps={mockAgentSteps}
        reportState={{ status: "error", error: resourceNotFoundError }}
      />
    );

    expect(markup).toContain("等待人工确认");
    expect(markup).toContain("Mock risk policy requires human confirmation before any follow-up action.");
    expect(markup).not.toContain("Report was not found or is not accessible.");
  });

  it("renders a not-ready state when a running task has no report yet", () => {
    const runningTask: AiTask = {
      ...mockWaitingApprovalTask,
      status: "running",
      blocking_reason: null
    };

    const markup = renderToStaticMarkup(
      <ReportPanel task={runningTask} steps={mockAgentSteps} reportState={{ status: "error", error: resourceNotFoundError }} />
    );

    expect(markup).toContain("报告尚未生成");
    expect(markup).toContain("任务仍在处理中，报告尚未生成。");
  });

  it("renders a dedicated not-found state for missing reports", () => {
    const completedTask: AiTask = {
      ...mockWaitingApprovalTask,
      status: "success",
      blocking_reason: null
    };

    const markup = renderToStaticMarkup(
      <ReportPanel task={completedTask} steps={mockAgentSteps} reportState={{ status: "error", error: resourceNotFoundError }} />
    );

    expect(markup).toContain("报告不存在或不可访问");
    expect(markup).toContain("RESOURCE_NOT_FOUND");
    expect(markup).toContain("trace_report_missing");
  });
});
