CREATE TABLE local_modalidade (
    local_id UUID NOT NULL REFERENCES local_partida(id) ON DELETE CASCADE,
    modalidade_id UUID NOT NULL REFERENCES modalidade(id),
    PRIMARY KEY (local_id, modalidade_id)
);
CREATE INDEX idx_local_modalidade_modalidade ON local_modalidade(modalidade_id);

-- Preserva os vínculos comprovados pelo histórico.
INSERT INTO local_modalidade(local_id, modalidade_id)
SELECT local_id, modalidade_id FROM partida
UNION
SELECT local_id, modalidade_id FROM agenda_disponibilidade;

-- Instalações com uma única modalidade mantêm os locais existentes disponíveis.
INSERT INTO local_modalidade(local_id, modalidade_id)
SELECT l.id, m.id FROM local_partida l CROSS JOIN modalidade m
WHERE (SELECT COUNT(*) FROM modalidade) = 1
  AND NOT EXISTS (SELECT 1 FROM local_modalidade lm WHERE lm.local_id = l.id);
