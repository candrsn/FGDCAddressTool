-- support_tables.sql

CREATE TABLE project_info (
name      varchar(160),
sname     varchar(35),
initdate  timestamp default now(),
status    varchar(15),
laststep  varchar(40)
);

CREATE TABLE table_info (
id        integer,
name      varchar(35),
role      varchar(35),
layout    varchar(15),
loaddate  timestamp,
status    varchar(2)
);

CREATE TABLE publication_info (
pub_date  timestamp default now(),
pub_records integer
);

CREATE TABLE project_log (
log       varchar(200),
logdate   timestamp default now()
);


