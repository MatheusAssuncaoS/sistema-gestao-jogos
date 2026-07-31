CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    peso INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_categoria_nome UNIQUE (nome)
);

CREATE TABLE jogador (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    matricula_associado VARCHAR(50),
    situacao_associativa VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    categoria_id BIGINT,
    aprovado_em TIMESTAMPTZ,
    aprovado_por UUID,

    CONSTRAINT uk_jogador_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_jogador_matricula UNIQUE (matricula_associado),

    CONSTRAINT fk_jogador_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),

    CONSTRAINT fk_jogador_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria (id),

    CONSTRAINT fk_jogador_aprovador
        FOREIGN KEY (aprovado_por) REFERENCES usuario (id),

    CONSTRAINT ck_jogador_situacao CHECK (
        situacao_associativa IN ('PENDENTE', 'REGULAR', 'IRREGULAR')
    )
);

CREATE INDEX idx_jogador_situacao ON jogador (situacao_associativa);

-- Séries do futebol (UC23). O peso serve ao balanceamento das equipes (RN10).
INSERT INTO categoria (nome, peso) VALUES
    ('Série A', 3),
    ('Série B', 2),
    ('Série C', 1);
