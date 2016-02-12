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
select e.identification environment, count(*) layer_count from layers l
join publisher.published_service_environment pse on pse.service_id = l.service_id 
join publisher.environment e on e.id = pse.environment_id
group by e.identification;
