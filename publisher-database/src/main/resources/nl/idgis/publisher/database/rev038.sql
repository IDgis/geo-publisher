--
-- adding database constraints and some fixes
--
-- cascade delete
--
ALTER TABLE publisher.leaf_layer_keyword DROP CONSTRAINT IF EXISTS leaf_layer_keyword_leaf_layer_id_fkey;
ALTER TABLE publisher.leaf_layer_keyword ADD CONSTRAINT leaf_layer_keyword_leaf_layer_id_fkey FOREIGN KEY (leaf_layer_id) REFERENCES publisher.leaf_layer (id) ON DELETE CASCADE;

ALTER TABLE publisher.layer_style DROP CONSTRAINT IF EXISTS layer_style_layer_id_fkey;
ALTER TABLE publisher.layer_style ADD CONSTRAINT layer_style_layer_id_fkey FOREIGN KEY (layer_id) REFERENCES publisher.leaf_layer (id) ON DELETE CASCADE;
ALTER TABLE publisher.layer_style DROP CONSTRAINT IF EXISTS layer_style_style_id_fkey;
ALTER TABLE publisher.layer_style ADD CONSTRAINT layer_style_style_id_fkey FOREIGN KEY (style_id) REFERENCES publisher.style (id) ON DELETE CASCADE;

ALTER TABLE publisher.service_keyword DROP CONSTRAINT IF EXISTS service_keyword_service_id_fkey;
ALTER TABLE publisher.service_keyword ADD CONSTRAINT service_keyword_service_id_fkey FOREIGN KEY (service_id) REFERENCES publisher.service (id) ON DELETE CASCADE;

ALTER TABLE publisher.service DROP CONSTRAINT IF EXISTS service_generic_layer_id_fkey;
ALTER TABLE publisher.service ADD CONSTRAINT service_generic_layer_id_fkey FOREIGN KEY (generic_layer_id) REFERENCES publisher.generic_layer (id) ON DELETE CASCADE;

ALTER TABLE publisher.tiled_layer DROP CONSTRAINT IF EXISTS tiled_layer_generic_layer_id_fkey;
ALTER TABLE publisher.tiled_layer ADD CONSTRAINT tiled_layer_generic_layer_id_fkey FOREIGN KEY (generic_layer_id) REFERENCES publisher.generic_layer (id) ON DELETE CASCADE;

ALTER TABLE publisher.leaf_layer DROP CONSTRAINT IF EXISTS leaf_layer_dataset_id_fkey;
ALTER TABLE publisher.leaf_layer ADD CONSTRAINT leaf_layer_dataset_id_fkey FOREIGN KEY (dataset_id) REFERENCES publisher.dataset (id) ON DELETE CASCADE;
ALTER TABLE publisher.leaf_layer DROP CONSTRAINT IF EXISTS leaf_layer_generic_layer_id_fkey;
ALTER TABLE publisher.leaf_layer ADD CONSTRAINT leaf_layer_generic_layer_id_fkey FOREIGN KEY (generic_layer_id) REFERENCES publisher.generic_layer (id) ON DELETE CASCADE;

ALTER TABLE publisher.layer_structure DROP CONSTRAINT IF EXISTS layer_structure_child_layer_id_fkey;
ALTER TABLE publisher.layer_structure ADD CONSTRAINT layer_structure_child_layer_id_fkey FOREIGN KEY (child_layer_id) REFERENCES publisher.generic_layer (id) ON DELETE CASCADE;
ALTER TABLE publisher.layer_structure DROP CONSTRAINT IF EXISTS layer_structure_parent_layer_id_fkey;
ALTER TABLE publisher.layer_structure ADD CONSTRAINT layer_structure_parent_layer_id_fkey FOREIGN KEY (parent_layer_id) REFERENCES publisher.generic_layer (id) ON DELETE CASCADE;

--
-- fixes
--
alter table publisher.tiled_layer drop column enabled;
alter table publisher.style add column style_type varchar(20) default 'POINT'; 
update publisher.style set style_type = 'POINT';
alter table publisher.style alter column style_type set not null;
--
-- constraints
--
-- service
alter table publisher.service alter column identification set not null;
alter table publisher.service alter column identification type varchar(80);
alter table publisher.service add constraint service_identification_key unique(identification);
alter table publisher.service alter column name set not null;
alter table publisher.service alter column name type varchar(80);
alter table publisher.service add constraint service_name_key unique(name);
alter table publisher.service alter column generic_layer_id set not null;
-- tiledlayer
alter table publisher.tiled_layer alter column meta_width set default 4;
alter table publisher.tiled_layer alter column meta_height set default 4;
alter table publisher.tiled_layer alter column expire_cache set default 0;
alter table publisher.tiled_layer alter column expire_clients set default 0;
alter table publisher.tiled_layer alter column gutter set default 0;
-- style
alter table publisher.style alter column definition set not null;
-- xx_keyword
alter table publisher.leaf_layer_keyword alter column leaf_layer_id set not null;
alter table publisher.leaf_layer_keyword alter column keyword set not null;
alter table publisher.service_keyword alter column service_id set not null;
alter table publisher.service_keyword alter column keyword set not null;

insert into publisher.version(id) values(38);
