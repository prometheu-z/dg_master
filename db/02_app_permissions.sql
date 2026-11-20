-- Apply after 01_schema_objetos.sql, once dg_app has been created by the
-- environment using a unique password (for example MYSQL_USER/MYSQL_PASSWORD).
-- Existing MySQL volumes do not rotate this account's password from environment variables.
-- Remove the broad database-level privileges added by the MySQL image.
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'dg_app'@'%';

GRANT SELECT ON dg_master_db.* TO 'dg_app'@'%';
GRANT EXECUTE ON dg_master_db.* TO 'dg_app'@'%';

-- Direct DML is limited to operations not yet routed through stored procedures.
GRANT INSERT, UPDATE ON dg_master_db.cliente TO 'dg_app'@'%';
GRANT INSERT, DELETE ON dg_master_db.dependente TO 'dg_app'@'%';
GRANT INSERT ON dg_master_db.gerente TO 'dg_app'@'%';
GRANT INSERT ON dg_master_db.categoria TO 'dg_app'@'%';
GRANT INSERT, UPDATE, DELETE ON dg_master_db.jogo TO 'dg_app'@'%';
GRANT UPDATE ON dg_master_db.penalidade TO 'dg_app'@'%';
GRANT INSERT ON dg_master_db.aluguel_dependente TO 'dg_app'@'%';
