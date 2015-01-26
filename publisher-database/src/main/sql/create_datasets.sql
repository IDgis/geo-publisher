insert into publisher.dataset(identification, source_dataset_id, name, uuid, file_uuid)
select sd.identification, sd.id, sd.identification, uuid_in(md5(random()::text)::cstring), uuid_in(md5(random()::text)::cstring)
from publisher.source_dataset sd
where exists (
	select * from publisher.source_dataset_version sdv
	where sdv.type = 'VECTOR' and sdv.source_dataset_id = sd.id
);

insert into publisher.dataset_column(dataset_id, index, name, data_type)
select d.id, sdvc.index, sdvc.name, sdvc.data_type
from publisher.source_dataset sd
join publisher.dataset d on d.source_dataset_id = sd.id
join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id
join publisher.source_dataset_version_column sdvc on sdvc.source_dataset_version_id = sdv.id
where sdv.type = 'VECTOR';