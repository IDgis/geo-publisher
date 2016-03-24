select gl.name, gl.title 
from publisher.dataset_copy dc
join publisher.dataset d on d.id = dc.dataset_id
join publisher.published_service_dataset psd on dc.dataset_id = d.id
join publisher.service s on s.id = psd.service_id
join publisher.generic_layer gl on gl.id = s.generic_layer_id
where not exists (
	select * from data.gt_pk_metadata
	where table_name = d.identification
)
group by gl.name, gl.title;