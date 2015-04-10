
insert into publisher.environment (id, identification, name, confidential) values
	(nextval('publisher.environment_id_seq'), 'geoserver-public', 'Publieke services', false),
	(nextval('publisher.environment_id_seq'), 'geoserver-secure', 'Beveiligde services', true),
	(nextval('publisher.environment_id_seq'), 'geoserver-guaranteed', 'Gegarandeerde services', false);
