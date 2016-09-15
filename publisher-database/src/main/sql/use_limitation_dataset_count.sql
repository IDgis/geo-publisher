with use_limitation as (
select xpath(
	'//gmd:useLimitation/gco:CharacterString/text()', 
	convert_from(document, 'UTF-8')::xml, 
	ARRAY[
		ARRAY['gmd', 'http://www.isotc211.org/2005/gmd'],
		ARRAY['gco', 'http://www.isotc211.org/2005/gco']])::text[] use_limitation,
	source_dataset_id
from publisher.source_dataset_metadata), dataset_count as (
select 
	use_limitation @> ARRAY['Downloadable data'] downloadable_data,
	use_limitation @> ARRAY['Geoportaal extern'] geoportaal_extern, (	
		select greatest(count(*), 1)
		from publisher.dataset d 
		where d.source_dataset_id = use_limitation.source_dataset_id
		and exists (
			select * from publisher.published_service_dataset psd
			where psd.dataset_id = d.id
		)
	) dataset_count
from use_limitation)
select downloadable_data, geoportaal_extern, sum(dataset_count) count
from dataset_count
group by 1, 2