ALTER TABLE usuario
    DROP CONSTRAINT ck_usuario_status;

ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_status CHECK (
        status IN ('PENDENTE', 'ATIVO', 'BLOQUEADO', 'INATIVO', 'RECUSADO')
        );
