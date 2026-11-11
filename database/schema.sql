-- Schéma MySQL 8 du projet "Banquier"
-- L'ordre compte : une table doit exister avant d'être référencée par une clé étrangère.

CREATE DATABASE IF NOT EXISTS banquier CHARACTER SET utf8mb4;
USE banquier;

-- Les utilisateurs et leur rôle
CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,                     -- hash BCrypt, jamais le mot de passe
  role ENUM('CLIENT','CAISSIER','AUDITEUR','ADMIN') NOT NULL,
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Les comptes : le solde ne peut jamais être négatif
CREATE TABLE accounts (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_number VARCHAR(20)   NOT NULL UNIQUE,
  owner_name     VARCHAR(100)  NOT NULL,
  currency       CHAR(3)       NOT NULL DEFAULT 'CDF',
  balance        DECIMAL(18,2) NOT NULL DEFAULT 0.00,      -- DECIMAL : exact, jamais d'arrondi flottant
  status ENUM('ACTIVE','BLOCKED','CLOSED') NOT NULL DEFAULT 'ACTIVE',
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  owner_user_id  BIGINT NULL,                              -- le propriétaire (pour limiter un CLIENT à ses comptes)
  CONSTRAINT chk_balance_positive CHECK (balance >= 0),
  CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Les virements : la clé d'idempotence est UNIQUE (pas de double virement)
CREATE TABLE transfers (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  idempotency_key      VARCHAR(64)   NOT NULL UNIQUE,
  from_account_id      BIGINT        NOT NULL,
  to_account_id        BIGINT        NOT NULL,
  amount               DECIMAL(18,2) NOT NULL,
  status ENUM('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
  created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reverses_transfer_id BIGINT NULL UNIQUE,                 -- si c'est une annulation : le virement annulé (UNE seule annulation possible)
  CONSTRAINT chk_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_different_accounts CHECK (from_account_id <> to_account_id),
  FOREIGN KEY (from_account_id) REFERENCES accounts(id),
  FOREIGN KEY (to_account_id)   REFERENCES accounts(id),
  CONSTRAINT fk_transfers_reverses FOREIGN KEY (reverses_transfer_id) REFERENCES transfers(id)
) ENGINE=InnoDB;

-- Le journal comptable : chaque virement écrit un débit et un crédit, jamais modifiés
CREATE TABLE ledger_entries (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  transfer_id   BIGINT NOT NULL,
  account_id    BIGINT NOT NULL,
  entry_type    ENUM('DEBIT','CREDIT') NOT NULL,
  amount        DECIMAL(18,2) NOT NULL,
  balance_after DECIMAL(18,2) NOT NULL,                    -- solde juste après le mouvement
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_entry_amount CHECK (amount > 0),
  FOREIGN KEY (transfer_id) REFERENCES transfers(id),
  FOREIGN KEY (account_id)  REFERENCES accounts(id)
) ENGINE=InnoDB;

-- Le journal d'audit : qui a fait quoi, quand, avec quel résultat (y compris les refus)
CREATE TABLE audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  username    VARCHAR(50)  NOT NULL,
  action      VARCHAR(50)  NOT NULL,
  entity_type VARCHAR(50)  NOT NULL,
  entity_id   VARCHAR(50)  NULL,
  outcome ENUM('SUCCESS','REJECTED') NOT NULL,
  details     VARCHAR(500) NULL
) ENGINE=InnoDB;

-- Verrous : l'audit est en lecture seule (ni modification, ni suppression)
CREATE TRIGGER audit_log_no_update BEFORE UPDATE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log est en lecture seule : modification interdite';

CREATE TRIGGER audit_log_no_delete BEFORE DELETE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log est en lecture seule : suppression interdite';

-- Comptes de démonstration (les propriétaires sont liés après le premier démarrage, voir le README)
INSERT INTO accounts (account_number, owner_name, balance) VALUES
  ('ACC-001', 'Péguy', 1000000),
  ('ACC-002', 'Marie',  1000000);