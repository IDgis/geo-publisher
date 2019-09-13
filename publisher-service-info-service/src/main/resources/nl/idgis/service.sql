with recursive service_layer_structure as (
	select
		gl.identification service_id,
		null::int parent_layer_id,
		s.generic_layer_id child_layer_id,
		null::int style_id,
		null::int layer_order,
		array[]::int[] anchestors		
	from publisher.service s
	join publisher.generic_layer gl on gl.id = s.generic_layer_id
	union all
	select
		st.service_id,
		ls.parent_layer_id,
		ls.child_layer_id,
		ls.style_id style_id,
		ls.layer_order,
		st.anchestors || st.child_layer_id
	from service_layer_structure st
	join publisher.layer_structure ls on ls.parent_layer_id = st.child_layer_id
	where not st.child_layer_id = any(anchestors)
)
select
	sls.service_id,
	sls.anchestors,
	jsonb_agg(
		case
			when sdv.type = 'VECTOR' then 
				case
					when sls.style_id is not null then jsonb_build_object(
						'groupStyleRef', (
							select jsonb_build_object(
								'name', s.name,
								'id', s.identification)
							from publisher.style s
							where s.id = sls.style_id))
					else '{}'
				end ||
				jsonb_build_object(
					'tableName', d.identification,
					'styleRefs', (
						select jsonb_agg(
							jsonb_build_object(
								'name', s.name,
								'id', s.identification) 
							order by ls.style_order)
						from publisher.layer_style ls
						join publisher.style s on s.id = ls.style_id
						where ls.layer_id = ll.id
				))
			when sdv.type = 'RASTER' then jsonb_build_object('fileName', d.identification || '.tif')
			else '{}'
		end ||
		jsonb_build_object(
			'internal_id', sls.child_layer_id, 
			'id', gl.identification,
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
group by 1, 2
order by 1, 2
