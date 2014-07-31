
insert into publisher.harvest_log(datasource_id, event)
select id, 'GENERIC.REQUESTED' from publisher.data_source;