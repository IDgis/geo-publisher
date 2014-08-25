
alter table publisher.source_dataset_history 
	rename to source_dataset_version;
alter table publisher.source_dataset_column_history 
	rename to source_dataset_version_column;
	
alter table publisher.source_dataset_version_column add column source_dataset_version_id integer;
update publisher.source_dataset_version_column set source_dataset_version_id = source_dataset_history_id; 
alter table publisher.source_dataset_version_column drop column source_dataset_history_id;
alter table publisher.source_dataset_version_column
  add constraint source_dataset_version_column_source_dataset_version_id_fkey 
	  foreign key (source_dataset_version_id) 
	  	references publisher.source_dataset_version (id);
	  	
alter table publisher.import_job add column source_dataset_version_id integer;
update publisher.import_job set source_dataset_version_id = source_dataset_history_id;
alter table publisher.import_job drop column source_dataset_history_id;
alter table publisher.import_job
  add constraint import_job_source_dataset_version_id_fkey 
	  foreign key (source_dataset_version_id) 
	  	references publisher.source_dataset_version (id);
	  	
alter table publisher.service_job add column source_dataset_version_id integer;
update publisher.service_job set source_dataset_version_id = source_dataset_history_id;
alter table publisher.service_job drop column source_dataset_history_id;
alter table publisher.service_job
  add constraint service_job_source_dataset_version_id_fkey 
	  foreign key (source_dataset_version_id) 
	  	references publisher.source_dataset_version (id);

insert into publisher.version(id) values(15);
