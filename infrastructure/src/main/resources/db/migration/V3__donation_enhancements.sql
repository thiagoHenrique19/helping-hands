CREATE SEQUENCE donation_receipt_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE donation
    ADD COLUMN anonymous      BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN message        VARCHAR(500),
    ADD COLUMN payment_method VARCHAR(255) NOT NULL DEFAULT 'PIX',
    ADD COLUMN receipt_code   VARCHAR(255);

UPDATE donation
SET receipt_code = 'DON-' || lpad(nextval('donation_receipt_seq')::text, 8, '0')
WHERE receipt_code IS NULL;

ALTER TABLE donation
    ALTER COLUMN receipt_code SET NOT NULL,
    ADD CONSTRAINT uq_donation_receipt_code UNIQUE (receipt_code),
    ADD CONSTRAINT chk_donation_minimum_amount CHECK (amount >= 1.00);

CREATE INDEX idx_donation_campaign_created_at ON donation (campaign_id, created_at DESC);
CREATE INDEX idx_donation_campaign_donor ON donation (campaign_id, lower(donor_email)) WHERE anonymous = FALSE;
