CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE usuario
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(200) NOT NULL,
    senha_hash    VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_status CHECK (
        status IN ('PENDENTE', 'ATIVO', 'BLOQUEADO', 'INATIVO')
        )
);

CREATE TABLE papel
(
    id   SMALLSERIAL PRIMARY KEY,
    nome VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario_papel
(
    usuario_id UUID     NOT NULL,
    papel_id   SMALLINT NOT NULL,

    PRIMARY KEY (usuario_id, papel_id),
    CONSTRAINT fk_usuario_papel_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_papel_papel
        FOREIGN KEY (papel_id) REFERENCES papel (id)
);

INSERT INTO papel (nome)
VALUES ('JOGADOR'),
       ('ORGANIZADOR'),
       ('ADMINISTRADOR');