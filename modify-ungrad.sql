UPDATE user_ SET `id` = LOWER(`id`);
alter table behandlungen add Zeit char(8);

alter table faelle modify id varchar(40),
modify patientid varchar(40), 
modify garantid varchar(40),
modify kostentrid varchar(40),
modify grund varchar(255),
modify diagnosen varchar(255);


alter table behandlungen modify id varchar(40),
modify fallid varchar(40),
modify mandantid varchar(40),
modify rechnungsid varchar(40);

alter table artikel modify id varchar(40),
modify lieferantid varchar(40);

alter table artikel_details modify ARTICLE_ID varchar(40);
alter table artikelstamm_ch modify ID varchar(40),
modify LieferantID varchar(40);


alter table diagnosen modify id varchar(40);
alter table behdl_dg_joint modify ID varchar(40),
modify BehandlungsID varchar(40),
modify DiagnoseID varchar(40);


