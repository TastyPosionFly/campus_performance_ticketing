CREATE TABLE IF NOT EXISTS organization_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_organization
        FOREIGN KEY (organization_id)
        REFERENCES organization(id)
        ON DELETE CASCADE
);