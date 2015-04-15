
create table publisher.published_service_dataset(
	service_id integer not null,
	dataset_id integer not null,
	
	constraint published_service_dataset_service_id_fk foreign key(service_id) references service(id),
	constraint published_service_dataset_dataset_id_fk foreign key(dataset_id) references dataset(id)
);

insert into publisher.version(id) values(55);