ALTER TABLE excecao_calendario
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_excecao_ativo_periodo
    ON excecao_calendario (ativo, inicio, fim);
