-- Read-only: no statement text, credentials or customer data are returned.
SET NOCOUNT ON;
SELECT name AS database_name, is_read_committed_snapshot_on,
       snapshot_isolation_state_desc
FROM sys.databases WHERE name = N'Cotizador';

SELECT r.session_id, r.blocking_session_id, r.status, r.command,
       r.wait_type, r.wait_time AS wait_duration_ms, r.total_elapsed_time AS request_duration_ms,
       r.cpu_time AS cpu_ms, r.reads, r.logical_reads, r.writes,
       r.open_transaction_count
FROM sys.dm_exec_requests AS r
WHERE r.database_id = DB_ID(N'Cotizador') AND r.session_id <> @@SPID
ORDER BY r.blocking_session_id DESC, r.total_elapsed_time DESC;

SELECT s.session_id, s.status, s.open_transaction_count,
       s.transaction_isolation_level, s.last_request_start_time, s.last_request_end_time
FROM sys.dm_exec_sessions AS s
WHERE s.is_user_process = 1 AND s.database_id = DB_ID(N'Cotizador') AND s.session_id <> @@SPID
ORDER BY s.open_transaction_count DESC, s.session_id;

SELECT wt.session_id, wt.blocking_session_id, wt.wait_type, wt.wait_duration_ms
FROM sys.dm_os_waiting_tasks AS wt
JOIN sys.dm_exec_sessions AS s ON s.session_id = wt.session_id
WHERE s.database_id = DB_ID(N'Cotizador') AND s.session_id <> @@SPID
ORDER BY wt.wait_duration_ms DESC;
GO
