create view publisher.dataset_active_notification as
select 
	ds.id as dataset_id,
	ds.identification as dataset_identification,
	ds.name as dataset_name,
	
	n.id as notification_id,
	n.type as notification_type,
	
	coalesce(nr.result, 'UNDETERMINED') as notification_result,
	
	j.id as job_id,
	j.type as job_type,
	j.create_time as job_create_time
from 
	publisher.notification n
	left join publisher.notification_result nr on (nr.notification_id = n.id)
	join publisher.job j on (j.id = n.job_id)
	join publisher.import_job ij on (ij.job_id = j.id)
	join publisher.dataset ds on (ds.id = ij.dataset_id)
where
	(nr.result is null or nr.result <> 'OK')
	and not exists (
		select
			*
		from
			publisher.notification n2
			join publisher.job j2 on (j2.id = n2.job_id)
			join publisher.import_job ij2 on (ij2.job_id = j2.id)
			join publisher.dataset ds2 on (ij2.dataset_id = ds2.id)
		where
			ds2.id = ds.id
			and j2.create_time > j.create_time
			and n2.type = n.type
	);
	
insert into publisher.version(id) values(19);	