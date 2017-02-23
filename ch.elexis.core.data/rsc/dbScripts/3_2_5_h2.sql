DELETE FROM BEHDL_DG_JOINT WHERE ID IN  (SELECT  e1.ID FROM BEHDL_DG_JOINT AS e1
JOIN BEHDL_DG_JOINT AS e2 
ON e1.DIAGNOSEID = e2.DIAGNOSEID AND e1.BEHANDLUNGSID = e2.BEHANDLUNGSID AND e1.id > e2.id);

ALTER TABLE BEHDL_DG_JOINT DROP PRIMARY KEY;
ALTER TABLE BEHDL_DG_JOINT ALTER COLUMN ID VARCHAR(25) NULL;
ALTER TABLE BEHDL_DG_JOINT ALTER COLUMN BEHANDLUNGSID VARCHAR(25) NOT NULL;
ALTER TABLE BEHDL_DG_JOINT ALTER COLUMN DIAGNOSEID VARCHAR(25) NOT NULL;
ALTER TABLE BEHDL_DG_JOINT ADD PRIMARY KEY ( BEHANDLUNGSID , DIAGNOSEID );