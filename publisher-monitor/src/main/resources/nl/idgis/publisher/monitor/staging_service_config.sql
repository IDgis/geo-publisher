with recursive layers as (
    select
        gl.name service_name,
        gl.id layer_id,
        gl.name layer_name,
        0 layer_order,
        0 depth
    from publisher.service s
    join publisher.generic_layer gl on gl.id = s.generic_layer_id
    union all
    select
        l.service_name,
        gl.id layer_id,
        gl.name layer_name,
        ls.layer_order,
        l.depth + 1 depth
    from layers l
    join publisher.layer_structure ls on ls.parent_layer_id = l.layer_id
    join publisher.generic_layer gl on gl.id = ls.child_layer_id
)
select service_name, layer_name
from layers l
where depth > 0
order by layer_order