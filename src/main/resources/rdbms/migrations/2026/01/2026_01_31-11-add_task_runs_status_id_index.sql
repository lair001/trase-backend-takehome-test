--liquibase formatted sql
--changeset slair:2026_01_31-11-add_task_runs_status_id_index

CREATE INDEX idx_task_runs_status_id ON task_runs(status, id);
