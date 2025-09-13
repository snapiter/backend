-- V1__init_schema.sql
-- Initial schema for SnapIter backend (PostgreSQL)

-- 1) TRACKABLES
CREATE TABLE trackables (
    trackable_id           VARCHAR(255) PRIMARY KEY,
    name                   TEXT,
    website_title          TEXT NOT NULL DEFAULT '',
    website                TEXT NOT NULL DEFAULT '',
    host_name              TEXT NOT NULL DEFAULT '',
    icon                   TEXT NOT NULL DEFAULT '',
    position_type          TEXT NOT NULL DEFAULT 'ALL',
    created_at             TIMESTAMPTZ
);

-- 2) DEVICES
CREATE TABLE devices (
    id                     BIGSERIAL PRIMARY KEY,
    trackable_id           VARCHAR(255) NOT NULL,
    device_id              VARCHAR(255) NOT NULL,
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
    trackable_id           VARCHAR(255) NOT NULL,
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


CREATE TABLE trip (
    id              BIGSERIAL PRIMARY KEY,
    trackable_id    VARCHAR(255) NOT NULL,
    start_date      TIMESTAMPTZ NOT NULL,
    end_date        TIMESTAMPTZ,
    title           TEXT NOT NULL,
    description     TEXT,
    slug            TEXT NOT NULL,
    position_type   TEXT NOT NULL DEFAULT 'HOURLY',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    color           VARCHAR(16) NOT NULL DEFAULT '#648192',
    animation_speed BIGINT NOT NULL DEFAULT 10000,
    CONSTRAINT ux_trip_trackable_slug UNIQUE (trackable_id, slug)
);
