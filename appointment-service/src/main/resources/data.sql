-- Seed data (idempotent): só insere quando a tabela está vazia, para que restarts
-- contra um banco persistente (Postgres, ddl-auto=update) não dupliquem linhas.
-- Mesmo padrão portável H2/Postgres usado pelo monolito.
--
-- pet_id/owner_id referenciam registros do petclinic-backend (outro banco, sem FK).
-- pet_name/owner_name são snapshots: em produção viriam do Feign no momento do
-- agendamento; aqui replicam o seed do monolito (Rex/Alice, Thor/Bruno, Luna/Carla).

INSERT INTO appointments (pet_id, owner_id, pet_name, owner_name, scheduled_at, veterinarian, reason, notes, status)
SELECT * FROM (VALUES
  (1, 1, 'Rex',  'Alice Souza',  TIMESTAMP '2026-09-10 09:00:00', 'Dra. Helena Prado',  'Vacinação anual',       'Reforço V10',                    'SCHEDULED'),
  (3, 2, 'Thor', 'Bruno Lima',   TIMESTAMP '2026-09-11 14:30:00', 'Dr. Marcos Vieira',  'Consulta de rotina',    'Acompanhamento de peso',         'SCHEDULED'),
  (5, 3, 'Luna', 'Carla Mendes', TIMESTAMP '2026-08-01 10:00:00', 'Dra. Helena Prado',  'Retorno dermatológico', 'Tratamento concluído com êxito', 'COMPLETED')
) AS seed(pet_id, owner_id, pet_name, owner_name, scheduled_at, veterinarian, reason, notes, status)
WHERE NOT EXISTS (SELECT 1 FROM appointments);
