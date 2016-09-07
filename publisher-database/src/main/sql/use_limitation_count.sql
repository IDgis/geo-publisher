with use_limitation as (
select xpath(
	'//gmd:useLimitation/gco:CharacterString/text()', 
	convert_from(document, 'UTF-8')::xml, 
	ARRAY[
		ARRAY['gmd', 'http://www.isotc211.org/2005/gmd'],
		ARRAY['gco', 'http://www.isotc211.org/2005/gco']])::text[] use_limitation
from publisher.source_dataset_metadata)
select 
	use_limitation @> ARRAY['Downloadable data'] downloadable_data,
	use_limitation @> ARRAY['Geoportaal extern'] geoportaal_extern,
	count(*)
from use_limitation
group by 1, 2;