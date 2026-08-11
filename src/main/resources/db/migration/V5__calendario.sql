-- RN06: as partidas ocorrem apenas em dias e horários configurados.
CREATE TABLE dia_funcionamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dia_da_semana VARCHAR(20) NOT NULL,
    horario TIME NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_dia_funcionamento UNIQUE (dia_da_semana, horario),

    CONSTRAINT ck_dia_funcionamento_dia CHECK (
        dia_da_semana IN (
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
            'FRIDAY', 'SATURDAY', 'SUNDAY'
        )
    )
);

-- RN07: feriados e recessos impedem a criação de partidas.
-- Um feriado tem inicio = fim; um recesso cobre um intervalo.
CREATE TABLE excecao_calendario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    descricao VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_excecao_tipo CHECK (tipo IN ('FERIADO', 'RECESSO')),

    CONSTRAINT ck_excecao_periodo CHECK (fim >= inicio)
);

CREATE INDEX idx_excecao_periodo ON excecao_calendario (inicio, fim);

-- Configuração inicial sugerida pelo documento de requisitos:
-- segundas, quartas e sextas-feiras.
INSERT INTO dia_funcionamento (dia_da_semana, horario) VALUES
    ('MONDAY', '19:00'),
    ('WEDNESDAY', '19:00'),
    ('FRIDAY', '20:00');
