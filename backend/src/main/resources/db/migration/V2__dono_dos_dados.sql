-- Cada médica enxerga só os próprios pacientes, dentistas e agendamentos.
-- dono_id = usuário que cadastrou o registro. O administrador enxerga tudo.
-- Registros que já existiam passam a pertencer ao administrador.

ALTER TABLE pacientes    ADD COLUMN dono_id BIGINT;
ALTER TABLE dentistas    ADD COLUMN dono_id BIGINT;
ALTER TABLE agendamentos ADD COLUMN dono_id BIGINT;

UPDATE pacientes    SET dono_id = (SELECT MIN(id) FROM usuarios WHERE cargo = 'ADMIN');
UPDATE dentistas    SET dono_id = (SELECT MIN(id) FROM usuarios WHERE cargo = 'ADMIN');
UPDATE agendamentos SET dono_id = (SELECT p.dono_id FROM pacientes p WHERE p.id = agendamentos.paciente_id);

ALTER TABLE pacientes    ADD CONSTRAINT fk_pacientes_dono    FOREIGN KEY (dono_id) REFERENCES usuarios (id) ON DELETE RESTRICT;
ALTER TABLE dentistas    ADD CONSTRAINT fk_dentistas_dono    FOREIGN KEY (dono_id) REFERENCES usuarios (id) ON DELETE RESTRICT;
ALTER TABLE agendamentos ADD CONSTRAINT fk_agendamentos_dono FOREIGN KEY (dono_id) REFERENCES usuarios (id) ON DELETE RESTRICT;

-- CPF e CRO passam a ser únicos por médica (duas médicas podem atender a mesma pessoa
-- sem uma descobrir o cadastro da outra).
ALTER TABLE pacientes DROP CONSTRAINT uq_pacientes_cpf;
ALTER TABLE pacientes ADD CONSTRAINT uq_pacientes_dono_cpf UNIQUE (dono_id, cpf);
ALTER TABLE dentistas DROP CONSTRAINT uq_dentistas_cro;
ALTER TABLE dentistas ADD CONSTRAINT uq_dentistas_dono_cro UNIQUE (dono_id, cro);

CREATE INDEX ix_agendamentos_dono ON agendamentos (dono_id);
CREATE INDEX ix_dentistas_dono    ON dentistas (dono_id);
