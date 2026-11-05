ALTER TABLE organization
    ADD COLUMN phone    VARCHAR(255),
    ADD COLUMN website  VARCHAR(255),
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE organization SET email = lower(trim(email));

ALTER TABLE organization
    ADD CONSTRAINT uq_organization_email UNIQUE (email);

ALTER TABLE campaign
    ADD COLUMN ends_at    TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN category   VARCHAR(255) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now();

CREATE INDEX idx_campaign_category ON campaign (category);
CREATE INDEX idx_campaign_active_ends_at ON campaign (ends_at) WHERE status = 'ACTIVE';
