--liquibase formatted sql
--changeset slair:2026_01_31-02-create_roles_table

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);
