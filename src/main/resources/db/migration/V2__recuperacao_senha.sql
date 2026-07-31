CREATE TABLE token_recuperacao_senha
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    usuario_id UUID         NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expira_em  TIMESTAMPTZ  NOT NULL,
    usado_em   TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_token_recuperacao UNIQUE (token),

    CONSTRAINT fk_token_recuperacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_token_recuperacao_usuario
    ON token_recuperacao_senha (usuario_id);