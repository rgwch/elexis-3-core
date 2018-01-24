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


alter table at_medevit_elexis_gdt_protokoll modify ID varchar(40),
	modify PatientID varchar(40);

alter table at_medevit_elexis_impfplan modify ID varchar(40),
	modify Patient_ID varchar(40);

alter table at_medevit_elexis_inbox modify ID varchar(40);
alter table at_medevit_elexis_labmap modify ID varchar(40);
alter table at_medevit_elexis_loinc modify ID varchar(40);
alter table at_medevit_elexis_medindex_article modify ID varchar(40);
alter table at_medevit_elexis_medindex_product modify ID varchar(40);
alter table at_medevit_medelexis_vat_ch modify ID varchar(40);

alter table auf modify id varchar(40),
	modify patientid varchar(40),
	modify fallid varchar(40),
	modify BriefID varchar(40);

alter table bbs modify id varchar(40), modify authorid varchar(40);

alter table bestellung_entry modify ID varchar(40),
	modify ARTICLE_ID varchar(40),
	modify stock varchar(40),
	modify PROVIDER varchar(40);

alter table bildanzeige modify ID varchar(40),
	modify PatID varchar(40);

alter table briefe modify id varchar(40),
	modify absenderid varchar(40),
	modify destid varchar(40),
	modify behandlungsid varchar(40),
	modify patientid varchar(40);

alter table ch_elexis_messages modify ID varchar(25),
	modify origin varchar(40),
	modify destination varchar(40);

alter table dbimage modify ID varchar(40);

alter table eigenleistungen modify ID varchar(40);

alter table ek_preise modify id varchar(40);

alter table elexisbefunde modify id varchar(40),
	modify patientid varchar(40);

alter table esrrecords modify ID varchar(40),
	modify RECHNUNGSID varchar(40),
	modify PATIENTID varchar(40),
	modify MANDANTID varchar(40);


alter table etiketten modify ID varchar(40),
	modify Image varchar(40);

alter table etiketten_objclass_link modify sticker varchar(40);

alter table etiketten_object_link modify obj varchar(40),
	modify etikette varchar(40);

alter table heap modify ID varchar(40);

alter table kontakt_adress_joint modify id varchar(40),
	modify myid varchar(40),
	modify otherid varchar(40),
	modify bezug varchar(255);

alter table kontakt_order_management modify ID varchar(40),
	modify KONTAKT_ID varchar(40);

alter table konto modify id varchar(40),
	modify patientid varchar(40),
	modify RechnungsID varchar(40),
	modify ZahlungsID varchar(40);

