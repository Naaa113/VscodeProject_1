CREATE TABLE IF NOT EXISTS approval_instance (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  requested_by VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_id VARCHAR(128) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  risk_reason CLOB NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_fingerprint VARCHAR(128) NOT NULL,
  action_input_json CLOB NOT NULL,
  citations_json CLOB,
  action_command_id VARCHAR(64),
  trace_id VARCHAR(128) NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uq_approval_idempotency UNIQUE (tenant_id, action_type, idempotency_key)
);

CREATE TABLE IF NOT EXISTS approval_record (
  id VARCHAR(64) PRIMARY KEY,
  approval_instance_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  decision VARCHAR(32) NOT NULL,
  decided_by VARCHAR(128) NOT NULL,
  reason CLOB NOT NULL,
  action_summary CLOB NOT NULL,
  trace_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_approval_record_instance FOREIGN KEY (approval_instance_id) REFERENCES approval_instance(id)
);

CREATE TABLE IF NOT EXISTS action_command (
  id VARCHAR(64) PRIMARY KEY,
  approval_instance_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  trace_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  executed_at TIMESTAMP WITH TIME ZONE,
  result_payload_json CLOB,
  error_json CLOB,
  CONSTRAINT uq_action_command_approval UNIQUE (approval_instance_id),
  CONSTRAINT fk_action_command_instance FOREIGN KEY (approval_instance_id) REFERENCES approval_instance(id)
);
