select 'begin;' sql
union all
select 
	'alter table ' 
	|| table_schema 
	|| '."' 
	|| table_name 
	|| '" add column "'
	|| table_name
	|| '_id" serial;'
from information_schema.tables t
where table_schema in ('data', 'staging_data')
and table_name != 'gt_pk_metadata'
and table_type = 'BASE TABLE'
and not exists (
	select * from information_schema.columns c
	where c.table_schema = t.table_schema
	and c.table_name = t.table_name
	and c.column_name = c.table_name || '_id'
)
union all
select 
	unnest(
		array[
			'drop view data."'
			|| table_name
			|| '";', 
			'create view data."'
			|| table_name
			|| '" as select * from staging_data."'
			|| table_name
			|| '";'
		]
	)
from information_schema.tables
where table_schema = 'data' 
and table_name != 'gt_pk_metadata'
and table_type = 'VIEW'
union all
select 'commit;';