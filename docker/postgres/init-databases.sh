#!/bin/bash
# ===================================================================
# PostgreSQL Initialization Script for SmartSure
# Creates all 5 service databases on first container startup.
# This script runs automatically when the postgres container starts
# for the first time (via docker-entrypoint-initdb.d).
# ===================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE auth_db;
    CREATE DATABASE policy_db;
    CREATE DATABASE claim_db;
    CREATE DATABASE admin_db;
    CREATE DATABASE payment_db;

    GRANT ALL PRIVILEGES ON DATABASE auth_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE policy_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE claim_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE admin_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE payment_db TO $POSTGRES_USER;
EOSQL

echo "All SmartSure databases created successfully."
