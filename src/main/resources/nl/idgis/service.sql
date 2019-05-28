with recursive service_layer_structure as (
	select
		s.id service_id,
		null::int parent_layer_id,
		s.generic_layer_id child_layer_id,
		null::integer layer_order,
		array[]::int[] anchestors		
	from publisher.service s
	union all
	select
		st.service_id,
		ls.parent_layer_id,
		ls.child_layer_id,
		ls.layer_order,
		st.anchestors || st.child_layer_id
	from service_layer_structure st
	join publisher.layer_structure ls on ls.parent_layer_id = st.child_layer_id
	where not st.child_layer_id = any(anchestors)
)
select
	sls.anchestors,
	jsonb_agg(
		case
			when sdv.type = 'VECTOR' then jsonb_build_object('dataSourceName', '"staging_data"."' || d.identification || '"')
			when sdv.type = 'RASTER' then jsonb_build_object('dataSourceName', d.identification || '.tif')
			else '{}'
		end ||
		jsonb_build_object(
			'id', sls.child_layer_id, 
			'name', gl.name,
			'type', coalesce(lower(sdv.type), 'group'),
			'title', gl.title) order by sls.layer_order) layers
from service_layer_structure sls
join publisher.generic_layer gl on gl.id = sls.child_layer_id
left join publisher.leaf_layer ll on ll.generic_layer_id = gl.id
left join publisher.dataset d on d.id = ll.dataset_id
left join publisher.last_import_job lij on lij.dataset_id = ll.dataset_id
left join publisher.import_job ij on ij.job_id = lij.job_id
left join publisher.source_dataset_version sdv on sdv.id = ij.source_dataset_version_id
where sls.service_id = ?
group by sls.anchestors
order by sls.anchestors
