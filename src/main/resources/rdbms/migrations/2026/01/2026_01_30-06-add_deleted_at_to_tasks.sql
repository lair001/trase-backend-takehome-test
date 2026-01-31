--liquibase formatted sql
--changeset slair:2026_01_30-06-add_deleted_at_to_tasks

ALTER TABLE tasks
    ADD COLUMN deleted_at TIMESTAMPTZ;
