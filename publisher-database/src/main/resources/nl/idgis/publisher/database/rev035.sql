
alter table publisher.service alter column identification set not null;
alter table publisher.service alter column identification type varchar(80);
alter table publisher.service add constraint service_identification_key unique(identification);
alter table publisher.service alter column name set not null;
alter table publisher.service alter column name type varchar(80);
alter table publisher.service add constraint service_name_key unique(name);
alter table publisher.service alter column rootgroup_id set not null;

alter table publisher.generic_layer alter column identification set not null;
alter table publisher.generic_layer alter column name set not null;
alter table publisher.generic_layer alter column name type varchar(80);
alter table publisher.generic_layer add constraint generic_name_key unique(name);

alter table publisher.layer_structure drop column id;
alter table publisher.layer_structure alter column parent_layer_id set not null;
alter table publisher.layer_structure alter column child_layer_id set not null;
alter table publisher.layer_structure add primary key (parent_layer_id, child_layer_id);
alter table publisher.layer_structure add column style_id integer;
alter table publisher.layer_structure add constraint layer_structure_style_id_fk foreign key (style_id) references publisher.style(id);

alter table publisher.layer_style drop column id;
alter table publisher.layer_style alter column layer_id set not null;
alter table publisher.layer_style alter column style_id set not null;
alter table publisher.layer_style add primary key (layer_id, style_id);

alter table publisher.style alter column name type varchar(80);
alter table publisher.style alter column name set not null;
alter table publisher.style add constraint style_name_key unique(name);

alter table publisher.leaf_layer alter column generic_layer_id set not null;
alter table publisher.leaf_layer alter column dataset_id set not null;

alter table publisher.tiled_layer drop column name;
alter table publisher.tiled_layer alter column generic_layer_id set not null;

insert into publisher.version(id) values(35);
