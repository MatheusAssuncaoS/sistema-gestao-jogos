CREATE TABLE agenda_disponibilidade (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    local_id UUID NOT NULL REFERENCES local_partida(id),
    modalidade_id UUID NOT NULL REFERENCES modalidade(id),
    categoria_id BIGINT REFERENCES categoria(id),
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agenda_periodo CHECK (fim >= inicio)
);

CREATE TABLE agenda_horario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agenda_id UUID NOT NULL REFERENCES agenda_disponibilidade(id) ON DELETE CASCADE,
    dia_da_semana VARCHAR(20) NOT NULL,
    horario TIME NOT NULL,
    CONSTRAINT uk_agenda_horario UNIQUE (agenda_id, dia_da_semana, horario),
    CONSTRAINT ck_agenda_horario_dia CHECK (dia_da_semana IN (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
        'FRIDAY', 'SATURDAY', 'SUNDAY'
    ))
);

CREATE INDEX idx_agenda_vigencia ON agenda_disponibilidade (inicio, fim);
CREATE INDEX idx_agenda_escopo ON agenda_disponibilidade (local_id, modalidade_id, categoria_id);
