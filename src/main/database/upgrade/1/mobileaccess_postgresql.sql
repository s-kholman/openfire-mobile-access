CREATE TABLE ofMobileAccessAudit (
    id BIGSERIAL PRIMARY KEY,
    eventAt BIGINT NOT NULL,
    actor VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    targetUsername VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    details VARCHAR(1000) NULL
);

CREATE INDEX ofMobileAccessAudit_eventAt_idx ON ofMobileAccessAudit (eventAt);

UPDATE ofVersion SET version = 1 WHERE name = 'mobileaccess';
