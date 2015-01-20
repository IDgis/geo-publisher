
alter table publisher.source_dataset_version alter column name drop not null;
alter table publisher.source_dataset_version alter column category_id drop not null;
alter table publisher.source_dataset_version alter column revision drop not null;
alter table publisher.source_dataset_version add column type varchar(80) not null;

create table publisher.source_dataset_version_log(
	source_dataset_version_id integer not null references publisher.source_dataset_version(id),
	level character varying(80) not null,
	type character varying(80) not null,
	content character varying(1000),
	create_time timestamp not null default now()
);

insert into publisher.version(id) values(26);
