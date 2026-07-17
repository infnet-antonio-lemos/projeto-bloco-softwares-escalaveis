-- Seed data (idempotent): each block only inserts when its table is empty,
-- so restarts against a persistent DB (Postgres, ddl-auto=update) don't duplicate rows.
-- The "SELECT * FROM (VALUES ...) WHERE NOT EXISTS" pattern works in both H2 and Postgres.

INSERT INTO owners (name, email, phone, address)
SELECT * FROM (VALUES
  ('Alice Souza',  'alice@email.com',  '(11) 99001-0001', 'Rua das Flores, 10'),
  ('Bruno Lima',   'bruno@email.com',  '(21) 99002-0002', 'Av. Central, 200'),
  ('Carla Mendes', 'carla@email.com',  '(31) 99003-0003', 'Praça da Paz, 5')
) AS seed(name, email, phone, address)
WHERE NOT EXISTS (SELECT 1 FROM owners);

INSERT INTO pets (name, species, breed, birth_date, owner_id)
SELECT * FROM (VALUES
  ('Rex',    'DOG',    'Labrador',        DATE '2020-03-15', 1),
  ('Mimi',   'CAT',    'Siamese',         DATE '2019-07-20', 1),
  ('Thor',   'DOG',    'Golden Retriever', DATE '2021-01-10', 2),
  ('Coco',   'BIRD',   'Canary',          DATE '2022-05-05', 2),
  ('Luna',   'CAT',    'Persian',         DATE '2018-11-30', 3),
  ('Pipo',   'RABBIT', 'Holland Lop',     DATE '2023-02-14', 3)
) AS seed(name, species, breed, birth_date, owner_id)
WHERE NOT EXISTS (SELECT 1 FROM pets);
