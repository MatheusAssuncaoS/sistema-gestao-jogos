ALTER TABLE excecao_calendario DROP CONSTRAINT ck_excecao_tipo;

ALTER TABLE excecao_calendario
    ADD CONSTRAINT ck_excecao_tipo CHECK (tipo IN ('FERIADO', 'RECESSO', 'BLOQUEIO'));
