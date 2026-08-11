CREATE TABLE inscricao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partida_id UUID NOT NULL,
    jogador_id UUID NOT NULL,
    status VARCHAR(25) NOT NULL,
    data_solicitacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_confirmacao TIMESTAMPTZ,
    data_promocao TIMESTAMPTZ,
    data_cancelamento TIMESTAMPTZ,
    motivo_cancelamento VARCHAR(255),
    cancelado_por UUID,

    CONSTRAINT fk_inscricao_partida
        FOREIGN KEY (partida_id) REFERENCES partida (id),

    CONSTRAINT fk_inscricao_jogador
        FOREIGN KEY (jogador_id) REFERENCES jogador (id),

    CONSTRAINT fk_inscricao_cancelado_por
        FOREIGN KEY (cancelado_por) REFERENCES usuario (id),

    CONSTRAINT ck_inscricao_status CHECK (
        status IN (
            'CONFIRMADA', 'LISTA_ESPERA', 'CANCELADA',
            'PRESENTE', 'AUSENTE'
        )
    )
);

-- RN14: um jogador só pode ter uma inscrição ATIVA por partida, mas pode se
-- inscrever de novo depois de cancelar. Um UNIQUE comum impediria a
-- reinscrição; o índice parcial deixa as linhas canceladas de fora e ainda
-- preserva o histórico de cancelamentos.
CREATE UNIQUE INDEX uk_inscricao_ativa
    ON inscricao (partida_id, jogador_id)
    WHERE status <> 'CANCELADA';

-- Contagem de confirmados por partida: consulta do caminho crítico da
-- inscrição, executada dentro do lock.
CREATE INDEX idx_inscricao_partida_status
    ON inscricao (partida_id, status);

-- Ordem da lista de espera (RN12, Marco 2). O índice parcial mantém o
-- tamanho mínimo, indexando apenas quem está na fila.
CREATE INDEX idx_inscricao_lista_espera
    ON inscricao (partida_id, data_solicitacao)
    WHERE status = 'LISTA_ESPERA';
