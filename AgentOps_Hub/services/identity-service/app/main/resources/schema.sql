CREATE TABLE IF NOT EXISTS tenant (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS sys_user (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  username VARCHAR(120) NOT NULL,
  password_hash VARCHAR(512) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  CONSTRAINT uq_user_tenant_username UNIQUE (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS sys_role (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  code VARCHAR(120) NOT NULL,
  name VARCHAR(160) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  CONSTRAINT uq_role_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
  id VARCHAR(64) PRIMARY KEY,
  code VARCHAR(160) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id VARCHAR(64) NOT NULL,
  role_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id VARCHAR(64) NOT NULL,
  permission_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
);

CREATE TABLE IF NOT EXISTS identity_audit_log (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64),
  user_id VARCHAR(64),
  action VARCHAR(80) NOT NULL,
  success BOOLEAN NOT NULL,
  error_code VARCHAR(80),
  trace_id VARCHAR(120) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
