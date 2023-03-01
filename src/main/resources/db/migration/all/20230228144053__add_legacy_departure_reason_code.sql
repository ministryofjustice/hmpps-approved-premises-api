ALTER TABLE departure_reasons ADD COLUMN legacy_delius_reason_code TEXT;

UPDATE departure_reasons SET legacy_delius_reason_code = 'J' WHERE id = 'ea6f21d9-e658-487f-9765-d8b09187df93';
UPDATE departure_reasons SET legacy_delius_reason_code = 'Q' WHERE id = '5bb2a91b-a33a-425e-8503-2ba69d7cabe7';
UPDATE departure_reasons SET legacy_delius_reason_code = 'H' WHERE id = '861b3403-7dd9-440f-86e9-0c164ca0812b';
UPDATE departure_reasons SET legacy_delius_reason_code = 'X' WHERE id = '9a68687d-944b-4960-9de2-7645635910f3';
UPDATE departure_reasons SET legacy_delius_reason_code = 'A' WHERE id = 'c81c42fe-f7af-4d16-b67d-93f0b4cbd338';
UPDATE departure_reasons SET legacy_delius_reason_code = 'D' WHERE id = 'f5b57d36-1b3c-4777-93a2-72ff59cf7674';
UPDATE departure_reasons SET legacy_delius_reason_code = 'F' WHERE id = 'b80cbd68-c223-40af-b82b-5f7cee6f6fd7';
UPDATE departure_reasons SET legacy_delius_reason_code = 'B' WHERE id = 'ff751e28-f957-4941-8049-5b44c6da878e';
UPDATE departure_reasons SET legacy_delius_reason_code = 'E' WHERE id = 'd6e3f1db-01a7-4ce4-b7cb-f1ff5454d62e';
UPDATE departure_reasons SET legacy_delius_reason_code = 'G' WHERE id = 'd86b6e2e-0b95-459a-a85a-065729149f0a';
UPDATE departure_reasons SET legacy_delius_reason_code = 'C' WHERE id = '761cfbe1-c102-4fe9-b943-bf131d550c90';
UPDATE departure_reasons SET legacy_delius_reason_code = 'P' WHERE id = '69eac20f-c3b9-4ea3-87fd-814c3f1fd117';
UPDATE departure_reasons SET legacy_delius_reason_code = 'V' WHERE id = '5b7662e3-f0d9-401d-baeb-d8ad391dc8bb';
UPDATE departure_reasons SET legacy_delius_reason_code = 'K' WHERE id = '6589f8fb-d377-4bc4-ace9-d3a2ce02f97b';
UPDATE departure_reasons SET legacy_delius_reason_code = 'N' WHERE id = '9c848d97-afe7-4da9-bd8b-f01897330e62';
UPDATE departure_reasons SET legacy_delius_reason_code = 'W' WHERE id = '60d030db-ce2d-4f1a-b24f-42ce15d268f7';
UPDATE departure_reasons SET legacy_delius_reason_code = 'O' WHERE id = 'f4d00e1c-8bfd-40e9-8241-a7d0f744e737';
