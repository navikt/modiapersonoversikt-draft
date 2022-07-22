CREATE TABLE owneruuid(
    owner       VARCHAR                     NOT NULL,
    uuid        UUID        UNIQUE          NOT NULL,
    created     TIMESTAMP   DEFAULT NOW()   NOT NULL
);

CREATE INDEX owneruuid_owner_idx ON owneruuid USING HASH(owner);
CREATE INDEX owneruuid_uuid_idx ON owneruuid USING HASH(uuid);
