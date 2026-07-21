-- V1__create_tables.sql
CREATE TABLE event (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(200) NOT NULL,
                       event_date TIMESTAMP NOT NULL,
                       total_slots INT NOT NULL,
                       available_slots INT NOT NULL,
                       version BIGINT NOT NULL DEFAULT 0,
                       CONSTRAINT chk_slots_not_negative CHECK (available_slots >= 0)
);

CREATE TABLE registration (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              event_id BIGINT NOT NULL,
                              participant_name VARCHAR(150) NOT NULL,
                              email VARCHAR(150) NOT NULL,
                              idempotency_key VARCHAR(100) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              created_at TIMESTAMP NOT NULL,
                              CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES event(id),
                              CONSTRAINT uk_event_email UNIQUE (event_id, email),
                              CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE notification_outbox (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     registration_id BIGINT NOT NULL,
                                     channel VARCHAR(20) NOT NULL,
                                     status VARCHAR(20) NOT NULL,
                                     attempts INT NOT NULL DEFAULT 0,
                                     created_at TIMESTAMP NOT NULL,
                                     sent_at TIMESTAMP,
                                     CONSTRAINT fk_notification_registration FOREIGN KEY (registration_id) REFERENCES registration(id)
);