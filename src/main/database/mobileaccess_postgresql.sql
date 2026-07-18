CREATE TABLE ofMobileAccessCredential (
    username VARCHAR(64) PRIMARY KEY,
    algorithm VARCHAR(64) NOT NULL,
    iterations INTEGER NOT NULL,
    salt VARCHAR(128) NOT NULL,
    passwordHash VARCHAR(256) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt BIGINT NOT NULL,
    updatedAt BIGINT NOT NULL,
    revokedAt BIGINT NULL
);

INSERT INTO ofVersion (name, version) VALUES ('mobileaccess', 0);
