ALTER TABLE excecao_calendario ADD COLUMN local_id UUID REFERENCES local_partida(id);
CREATE INDEX idx_excecao_local ON excecao_calendario(local_id);
