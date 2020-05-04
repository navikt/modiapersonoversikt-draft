CREATE TABLE draft(
    owner       VARCHAR                     NOT NULL,
    content     VARCHAR                     NOT NULL,
    context     JSONB                       NOT NULL,
    created     TIMESTAMP   DEFAULT NOW()   NOT NULL
);

CREATE INDEX draft_owner_idx ON draft USING HASH(owner);
CREATE INDEX draft_context_idx ON draft USING GIN(context);
