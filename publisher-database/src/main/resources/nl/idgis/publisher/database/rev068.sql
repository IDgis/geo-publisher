
alter table publisher.environment add constraint environment_identification_key unique(identification);
alter table publisher.environment add column url text;
update publisher.environment set url = 'http://' || environment.identification || '.example.com/geoserver/';
alter table publisher.environment alter column url set not null;

insert into publisher.version(id) values(68);
