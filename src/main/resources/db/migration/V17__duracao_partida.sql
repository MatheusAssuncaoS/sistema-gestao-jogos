ALTER TABLE partida ADD COLUMN duracao_minutos INTEGER NOT NULL DEFAULT 60;
ALTER TABLE partida ADD CONSTRAINT ck_partida_duracao CHECK (duracao_minutos BETWEEN 1 AND 1440);
