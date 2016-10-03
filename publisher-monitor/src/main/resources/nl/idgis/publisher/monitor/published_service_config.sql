with recursive layers as (
    select
        content::json ->> 'name' service_name,
        environment_id,
        json_array_elements(content::json -> 'layers') -> 'layer' layer, 
        0 depth
    from publisher.published_service
    union all
    select
        service_name, 
        environment_id,
        json_array_elements(layer -> 'layers') -> 'layer' layer,
        depth + 1 depth
    from layers
)
select service_name, layer ->> 'name' layer_name
from layers l
join publisher.environment e on e.id = l.environment_id
where e.identification = ?