#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER integration_test_monitor WITH PASSWORD 'integration_test_monitor_password';
EOSQL