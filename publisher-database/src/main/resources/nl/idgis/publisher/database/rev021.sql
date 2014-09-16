
create index dataset_source_dataset_id_idx on publisher.dataset(source_dataset_id);

create index dataset_column_dataset_id_idx on publisher.dataset_column(dataset_id);

create index harvest_job_data_source_id_idx on publisher.harvest_job(data_source_id);
create index harvest_job_job_id_idx on publisher.harvest_job(job_id);

create index import_job_dataset_id_idx on publisher.import_job(dataset_id);
create index import_job_job_id_idx on publisher.import_job(job_id);
create index import_job_source_dataset_version_id_idx on publisher.import_job(source_dataset_version_id);

create index import_job_column_import_job_id_idx on publisher.import_job_column(import_job_id);

create index job_log_job_state_id_idx on publisher.job_log(job_state_id);

create index job_state_job_id_idx on publisher.job_state(job_id);

create index notification_job_id_idx on publisher.notification(job_id);

create index notification_result_notification_id_idx on publisher.notification_result(notification_id);

create index service_job_dataset_id_idx on publisher.service_job(dataset_id);
create index service_job_job_id_idx on publisher.service_job(job_id);
create index service_job_source_dataset_version_id_idx on publisher.service_job(source_dataset_version_id);

create index source_dataset_data_source_id_idx on publisher.source_dataset(data_source_id);

create index source_dataset_version_category_id_idx on publisher.source_dataset_version(category_id);
create index source_dataset_version_source_dataset_id_idx on publisher.source_dataset_version(source_dataset_id);

create index source_dataset_version_column_source_dataset_version_id on publisher.source_dataset_version_column(source_dataset_version_id);

insert into publisher.version(id) values(21);
