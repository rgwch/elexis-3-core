ALTER TABLE USERCONFIG ADD PRIMARY KEY (USERID , PARAM);

UPDATE VK_PREISE SET ID = MID(MD5(UUID()), 1, 25) WHERE ID is NULL;
ALTER TABLE VK_PREISE ADD PRIMARY KEY (ID);