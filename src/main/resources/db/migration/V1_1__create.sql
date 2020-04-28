CREATE TABLE draft(
    owner       VARCHAR                     NOT NULL,
    content     VARCHAR                     NOT NULL,
    context     JSONB                       NOT NULL,
    created     TIMESTAMP   DEFAULT NOW()   NOT NULL
);
