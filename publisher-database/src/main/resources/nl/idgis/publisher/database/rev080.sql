
alter table publisher.style add column style_scheme_type text NOT NULL DEFAULT 'sld-scheme-1.0.0';

insert into publisher.version(id) values(80);