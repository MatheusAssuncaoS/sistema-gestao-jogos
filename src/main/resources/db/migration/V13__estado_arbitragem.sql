CREATE TABLE estado_arbitragem (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partida_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARACAO',
    segundos_base INTEGER NOT NULL DEFAULT 0,
    cronometro_iniciado_em TIMESTAMPTZ,
    dados_json TEXT NOT NULL DEFAULT '{"acrescimos":0,"escala":{},"gols":[],"punicoes":[]}',
    versao INTEGER NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_estado_arbitragem_partida
        FOREIGN KEY (partida_id) REFERENCES partida (id),

    CONSTRAINT ck_estado_arbitragem_status CHECK (
        status IN ('PREPARACAO', 'EM_ANDAMENTO', 'PAUSADA', 'FINALIZADA')
    ),

    CONSTRAINT ck_estado_arbitragem_segundos CHECK (segundos_base >= 0)
);

CREATE INDEX idx_estado_arbitragem_status ON estado_arbitragem (status);
