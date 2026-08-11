-- Database própria do appointment-service (padrão "database per service").
-- Os dois serviços compartilham a mesma instância PostgreSQL por economia de recursos,
-- mas nenhum enxerga as tabelas do outro: a integração é exclusivamente via HTTP.
--
-- ATENÇÃO: scripts em /docker-entrypoint-initdb.d só executam quando o volume de dados
-- está vazio. Se o volume pgdata já existir, rode `docker compose down -v` antes.

CREATE DATABASE appointmentsdb;
