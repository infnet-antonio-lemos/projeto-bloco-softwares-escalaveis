INSERT INTO owners (name, email, phone, address) VALUES
  ('Alice Souza',  'alice@email.com',  '(11) 99001-0001', 'Rua das Flores, 10'),
  ('Bruno Lima',   'bruno@email.com',  '(21) 99002-0002', 'Av. Central, 200'),
  ('Carla Mendes', 'carla@email.com',  '(31) 99003-0003', 'Praça da Paz, 5');

INSERT INTO pets (name, species, breed, birth_date, owner_id) VALUES
  ('Rex',    'DOG',    'Labrador',       '2020-03-15', 1),
  ('Mimi',   'CAT',    'Siamese',        '2019-07-20', 1),
  ('Thor',   'DOG',    'Golden Retriever','2021-01-10', 2),
  ('Coco',   'BIRD',   'Canary',         '2022-05-05', 2),
  ('Luna',   'CAT',    'Persian',        '2018-11-30', 3),
  ('Pipo',   'RABBIT', 'Holland Lop',    '2023-02-14', 3);
