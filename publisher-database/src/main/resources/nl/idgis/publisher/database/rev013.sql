
insert into publisher.source_dataset_history(source_dataset_id, name, time, category_id, revision)
select id, name, update_time, category_id, revision
from publisher.source_dataset;
alter table publisher.source_dataset drop column name;
alter table publisher.source_dataset drop column update_time;
alter table publisher.source_dataset drop column category_id;
alter table publisher.source_dataset drop column revision;

drop table publisher.source_dataset_column;

alter table publisher.import_job add column source_dataset_history_id integer not null; 
alter table publisher.import_job
	add constraint import_job_source_dataset_history_id_fkey
		foreign key(source_dataset_history_id) references publisher.source_dataset_history(id);

alter table publisher.service_job add column source_dataset_history_id integer not null;
alter table publisher.service_job
	add constraint service_job_source_dataset_history_id_fkey
		foreign key(source_dataset_history_id) references publisher.source_dataset_history(id);
		
alter table publisher.source_dataset_history add column create_time timestamp;
update publisher.source_dataset_history set create_time = time;
alter table publisher.source_dataset_history drop column time;
alter table publisher.source_dataset_history alter column create_time set not null; 
alter table publisher.source_dataset_history alter column create_time set default now();

insert into publisher.version(id) values(13);
