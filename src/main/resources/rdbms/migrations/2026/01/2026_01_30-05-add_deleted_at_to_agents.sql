--liquibase formatted sql
--changeset slair:2026_01_30-05-add_deleted_at_to_agents

ALTER TABLE agents
    ADD COLUMN deleted_at TIMESTAMPTZ;
