
create index if not exists source_dataset_version_log_source_dataset_version_id_create_time_idx on publisher.source_dataset_version_log(source_dataset_version_id, create_time);

insert into publisher.version(id) values(85);
