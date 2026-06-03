CREATE TABLE IF NOT EXISTS customer (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  external_ref VARCHAR(160),
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_by VARCHAR(64),
  updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ticket (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  title VARCHAR(240) NOT NULL,
  description CLOB NOT NULL,
  status VARCHAR(40) NOT NULL,
  priority VARCHAR(40) NOT NULL,
  category VARCHAR(120) NOT NULL,
  sla_due_at TIMESTAMP WITH TIME ZONE,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_by VARCHAR(64),
  updated_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT fk_ticket_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE INDEX IF NOT EXISTS idx_customer_tenant ON customer(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ticket_tenant_created ON ticket(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_tenant_status ON ticket(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_ticket_tenant_priority ON ticket(tenant_id, priority);
CREATE INDEX IF NOT EXISTS idx_ticket_tenant_category ON ticket(tenant_id, category);

CREATE TABLE IF NOT EXISTS ticket_audit_log (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  ticket_id VARCHAR(64),
  action VARCHAR(80) NOT NULL,
  success BOOLEAN NOT NULL,
  error_code VARCHAR(80),
  trace_id VARCHAR(120) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
