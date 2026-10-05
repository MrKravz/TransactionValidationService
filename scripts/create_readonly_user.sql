-- Create read-only user for PostgreSQL MCP server inspection
-- Usage: psql -U postgres -d transaction_validation_service_db -v READONLY_PASS="${DB_READONLY_PASSWORD:-readonly_pass}" -f create_readonly_user.sql

DO $$
DECLARE
    pwd text := :'READONLY_PASS';
BEGIN
    IF pwd IS NULL OR pwd = '' OR pwd = ':\'READONLY_PASS\'' THEN
        pwd := 'readonly_pass';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readonly_user') THEN
        EXECUTE format('CREATE USER readonly_user WITH PASSWORD %L', pwd);
    ELSE
        EXECUTE format('ALTER USER readonly_user WITH PASSWORD %L', pwd);
    END IF;
END
$$;

-- Grant permissions to read-only user
GRANT CONNECT ON DATABASE transaction_validation_service_db TO readonly_user;
GRANT USAGE ON SCHEMA public TO readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO readonly_user;

-- Ensure future tables created by liquibase/postgres are also readable by readonly_user
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO readonly_user;
