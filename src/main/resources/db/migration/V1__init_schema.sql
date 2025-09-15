-- V1__init_schema.sql
-- Initial schema for SnapIter backend (PostgreSQL)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) TRACKABLES
CREATE TABLE trackables (
    trackable_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT,
    website_title   TEXT NOT NULL DEFAULT '',
    website         TEXT NOT NULL DEFAULT '',
    host_name       TEXT NOT NULL UNIQUE,
    icon            TEXT NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ
);

-- 2) DEVICES
CREATE TABLE devices (
    id                     BIGSERIAL PRIMARY KEY,
    trackable_id           UUID NOT NULL,
    device_id              TEXT NOT NULL,
    created_at             TIMESTAMPTZ,
    last_reported_at       TIMESTAMPTZ,
    CONSTRAINT fk_devices_trackable
        FOREIGN KEY (trackable_id) REFERENCES trackables(trackable_id)
        ON DELETE CASCADE
);

-- Ensure a device_id can only exist once per trackable
CREATE UNIQUE INDEX ux_devices_trackable_device
    ON devices(trackable_id, device_id);

-- 3) POSITION REPORTS
CREATE TABLE position_report (
    id                     BIGSERIAL PRIMARY KEY,
    trackable_id           UUID NOT NULL,
    latitude               DOUBLE PRECISION NOT NULL,
    longitude              DOUBLE PRECISION NOT NULL,
    created_at             TIMESTAMPTZ,
    CONSTRAINT fk_position_report_trackable
        FOREIGN KEY (trackable_id) REFERENCES trackables(trackable_id)
        ON DELETE CASCADE
);

-- Fast lookups by (trackable, time)
CREATE INDEX idx_position_report_trackable_created
    ON position_report(trackable_id, created_at);

-- 4) TRIPS
CREATE TABLE trip (
    id              BIGSERIAL PRIMARY KEY,
    trackable_id    UUID NOT NULL,
    start_date      TIMESTAMPTZ NOT NULL,
    end_date        TIMESTAMPTZ,
    title           TEXT NOT NULL,
    description     TEXT,
    slug            TEXT NOT NULL,
    position_type   TEXT NOT NULL DEFAULT 'HOURLY',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    color           VARCHAR(16) NOT NULL DEFAULT '#648192',
    animation_speed BIGINT NOT NULL DEFAULT 10000,
    CONSTRAINT fk_trip_trackable
        FOREIGN KEY (trackable_id) REFERENCES trackables(trackable_id)
        ON DELETE CASCADE,
    CONSTRAINT ux_trip_trackable_slug UNIQUE (trackable_id, slug)
);
