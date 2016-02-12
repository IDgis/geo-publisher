with recursive layers as (
    select
        service_id,
        json_array_elements(content::json -> 'layers') -> 'layer' layer, 
        0 depth
    from publisher.published_service
    union all
    select
        service_id, 
        json_array_elements(layer -> 'layers') -> 'layer' layer,
        depth + 1 depth
    from layers
)
select 
	gl.identification service, 
	layer ->> 'name' layer_name
from layers l
join publisher.service s on s.id = l.service_id
join publisher.generic_layer gl on gl.id = s.generic_layer_id
where not exists (
	select * from publisher.published_service_dataset psd
	where psd.service_id = s.id
	and psd.layer_name = layer ->> 'name'
) and (layer ->> 'type') is not null;