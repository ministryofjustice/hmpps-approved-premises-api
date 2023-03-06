-- ${flyway:timestamp}

insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('199334d3-fabb-432f-84c3-0e92eaf13f24', '*', 'Pub nearby', 'approved-premises') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('94021062-f692-4877-b6e8-f36c7ff87a18', '*', 'Not suitable for registered sex offenders (RSO)', 'approved-premises') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('7846fbf2-b423-4ccc-b23b-d8b866f86bde', '*', 'Men only', 'approved-premises') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('a862be08-a96a-4337-9f46-26286db8015f', '*', 'Wheelchair accessible', 'approved-premises') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('fffb3004-5f0a-4e88-8350-fb89a0168296', 'premises', 'Park nearby', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('684f919a-4c4a-4e80-9b3a-1dcd35873b3f', 'premises', 'Pub nearby', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('78c5d99b-9702-4bf2-a23d-c2a0cf3a017d', 'premises', 'School nearby', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('2fff6ede-7035-4e8d-81ad-5fcd894b99cf', 'premises', 'Men only', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('8221f1ad-3aaf-406a-b918-dbdef956ea17', 'premises', 'Women only', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('12e2e689-b3fb-469d-baec-2fb68e15e85b', 'room', 'Single bed', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('08b756e2-0b82-4f49-a124-35ea4ebb1634', 'room', 'Double bed', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('7dd3bac5-3d1c-4acb-b110-b1614b2c95d8', 'room', 'Shared kitchen', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('e730bdea-6157-4910-b1b0-450b29bf0c9f', 'room', 'Shared bathroom', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('2183d873-8270-4e93-8518-3a668a053689', 'room', 'Lift access', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('a2dd4661-c7fe-4294-b3fd-3fc877e62aa5', '*', 'Not suitable for registered sex offenders (RSO)', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('62c4d8cf-b612-4110-9e27-5c29982f9fcf', '*', 'Not suitable for arson offenders', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('99bb0f33-ff92-4606-9d1c-43bcf0c42ef4', '*', 'Floor level access', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
insert into "characteristics" ("id", "model_scope", "name", "service_scope") values ('d2f7796a-88e5-4e53-ab6d-dabb145b6a60', '*', 'Wheelchair accessible', 'temporary-accommodation') ON CONFLICT (id) DO NOTHING;;
