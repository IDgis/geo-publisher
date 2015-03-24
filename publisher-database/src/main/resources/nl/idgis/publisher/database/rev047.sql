
alter table publisher.layer_structure alter column layer_order drop default;
alter table publisher.layer_structure alter column layer_order set not null;

alter table publisher.layer_structure drop constraint if exists layer_structure_pkey; -- PostgreSQL
alter table publisher.layer_structure drop constraint if exists constraint_40; -- H2

alter table publisher.layer_structure add constraint layer_structure_pkey primary key(parent_layer_id, layer_order);

insert into publisher.version(id) values(47);
