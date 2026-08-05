CREATE TABLE modalidade (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_modalidade_nome UNIQUE (nome)
);

CREATE TABLE local_partida (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_local_partida_nome UNIQUE (nome)
);

CREATE TABLE partida (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modalidade_id UUID NOT NULL,
    local_id UUID NOT NULL,
    categoria_id BIGINT,
    inicio TIMESTAMPTZ NOT NULL,
    capacidade INTEGER NOT NULL DEFAULT 16,
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    inscricoes_abrem_em TIMESTAMPTZ,
    inscricoes_encerram_em TIMESTAMPTZ,
    escala_publicada BOOLEAN NOT NULL DEFAULT FALSE,
    criado_por UUID NOT NULL,
    versao INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_partida_modalidade
        FOREIGN KEY (modalidade_id) REFERENCES modalidade (id),

    CONSTRAINT fk_partida_local
        FOREIGN KEY (local_id) REFERENCES local_partida (id),

    CONSTRAINT fk_partida_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria (id),

    CONSTRAINT fk_partida_criador
        FOREIGN KEY (criado_por) REFERENCES usuario (id),

    CONSTRAINT ck_partida_capacidade CHECK (capacidade > 0),

    CONSTRAINT ck_partida_status CHECK (
        status IN (
            'RASCUNHO', 'ABERTA', 'LOTADA',
            'ENCERRADA', 'FINALIZADA', 'CANCELADA'
        )
    ),

    CONSTRAINT ck_partida_periodo_inscricao CHECK (
        inscricoes_encerram_em IS NULL
        OR inscricoes_abrem_em IS NULL
        OR inscricoes_encerram_em > inscricoes_abrem_em
    )
);

CREATE INDEX idx_partida_inicio ON partida (inicio);
CREATE INDEX idx_partida_status_inicio ON partida (status, inicio);

CREATE TABLE equipe (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partida_id UUID NOT NULL,
    nome VARCHAR(50) NOT NULL,
    cor VARCHAR(20) NOT NULL,
    capacidade INTEGER NOT NULL DEFAULT 8,

    CONSTRAINT fk_equipe_partida
        FOREIGN KEY (partida_id) REFERENCES partida (id),

    CONSTRAINT uk_equipe_partida_cor UNIQUE (partida_id, cor),

    CONSTRAINT ck_equipe_capacidade CHECK (capacidade > 0)
);

-- RN11: a estrutura é genérica; o futebol é apenas a primeira modalidade.
INSERT INTO modalidade (nome) VALUES ('Futebol');

INSERT INTO local_partida (nome, descricao) VALUES
    ('Campo principal', 'Campo oficial do clube');
