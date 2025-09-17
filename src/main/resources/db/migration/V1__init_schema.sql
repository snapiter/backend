-- V1__init_schema.sql
-- Initial schema for SnapIter backend (PostgreSQL)



-- MAGIC LINKS
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
  user_id UUID UNIQUE NOT NULL,
  email CITEXT UNIQUE NOT NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  display_name TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_at TIMESTAMPTZ
);

CREATE TABLE magic_links (
  id BIGSERIAL PRIMARY KEY,
  email CITEXT NOT NULL,
  user_id UUID,
  token_hash TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ
);

CREATE INDEX ON magic_links (email);
CREATE INDEX ON magic_links (expires_at);


-- V2__refresh_tokens.sql
CREATE TABLE refresh_tokens (
  id               BIGSERIAL PRIMARY KEY,
  user_id          UUID NOT NULL,
  token_hash       TEXT NOT NULL UNIQUE,
  issued_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at       TIMESTAMPTZ NOT NULL,
  revoked_at       TIMESTAMPTZ,
  replaced_by      TEXT,
  user_agent       TEXT,
  ip               TEXT,
  last_used_at     TIMESTAMPTZ
);

CREATE INDEX ON refresh_tokens (user_id);
CREATE INDEX ON refresh_tokens (expires_at);


CREATE TABLE device_tokens (
  id          BIGSERIAL PRIMARY KEY,
  device_id   TEXT NOT NULL UNIQUE,   -- one active token per device
  token_hash  TEXT NOT NULL UNIQUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at  TIMESTAMPTZ
);


-------------------------------------



CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE trackables (
    id              BIGSERIAL PRIMARY KEY,
    trackable_id    TEXT NOT NULL UNIQUE,
    name            TEXT,
    website_title   TEXT NOT NULL DEFAULT '',
    website         TEXT NOT NULL DEFAULT '',
    host_name       TEXT NOT NULL UNIQUE,
    icon            TEXT NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ,
    user_id         UUID NOT NULL,
    CONSTRAINT fk_trackables_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);


-- 2) DEVICES
CREATE TABLE devices (
    id                     BIGSERIAL PRIMARY KEY,
    trackable_id           TEXT NOT NULL,
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
    trackable_id            TEXT NOT NULL,
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
    trackable_id    TEXT NOT NULL,
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

