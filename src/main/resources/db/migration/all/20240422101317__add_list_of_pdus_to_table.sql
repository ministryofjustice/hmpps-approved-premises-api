Alter Table probation_delivery_units
DROP CONSTRAINT probation_delivery_units_name_key;
Alter Table probation_delivery_units
    ADD CONSTRAINT probation_delivery_units_name_key
        UNIQUE (name,probation_region_id);

INSERT INTO probation_delivery_units (id, name, probation_region_id,delius_code) VALUES
     ('9b40dc3c-0e1b-4b94-b937-5ab54d1d35ce','All Wales','afee0696-8df3-4d9f-9d0c-268f17772e2c','N03ALL'),
     ('224d8d90-039c-42e3-8b8c-57b4e563ba43','Corporate Services','afee0696-8df3-4d9f-9d0c-268f17772e2c','N03COSE'),
     ('ddd80a7c-db4f-41ee-9af5-b8373185cddf','Public Protection Community','afee0696-8df3-4d9f-9d0c-268f17772e2c','N03PPC'),
     ('e219063b-d149-499f-bc08-b828430a5940','Public Protection Residential','afee0696-8df3-4d9f-9d0c-268f17772e2c','N03PPR'),
     ('59f389cd-a298-4b32-826c-6a6692ae9d01','Stakeholder Engagement London','d73ae6b5-041e-4d44-b859-b8c77567d893','N07700'),
     ('17b6100d-d528-41ab-bde1-2e7fb592a032','All London','d73ae6b5-041e-4d44-b859-b8c77567d893','N07ALL'),
     ('de229de9-8c4f-45b6-a644-3c60a2838e10','Accredited Programmes and Structured Interventions','d73ae6b5-041e-4d44-b859-b8c77567d893','N07APSI'),
     ('0573d602-6297-414a-a2f1-2fcb82f82aff','Corporate Services','d73ae6b5-041e-4d44-b859-b8c77567d893','N07COSE'),
     ('8e37a19c-0066-4384-8ee8-8da762edbdbe','Headquarters','d73ae6b5-041e-4d44-b859-b8c77567d893','N07HDQT'),
     ('d11d5fa9-8836-4fcb-ba51-bf2174b1e977','IAPS Level 2(N07)','d73ae6b5-041e-4d44-b859-b8c77567d893','N07IAP'),
     ('c4c3f248-0483-416f-9375-a19848bca3a2','Public Protection London','d73ae6b5-041e-4d44-b859-b8c77567d893','N07LPP'),
     ('70791e30-775a-4a1a-bbec-9c5c06444b33','Public Protection Community','d73ae6b5-041e-4d44-b859-b8c77567d893','N07PPC'),
     ('fd716a5c-c88c-4e2b-9133-52474478baf2','Public Protection Residential','d73ae6b5-041e-4d44-b859-b8c77567d893','N07PPR'),
     ('1ece3a7c-a295-4221-ae84-f345a867738c','Performance and Quality','d73ae6b5-041e-4d44-b859-b8c77567d893','N07PQU'),
     ('22105203-2402-4232-b267-58716776a26d','Unpaid Work and Attendance Centres','d73ae6b5-041e-4d44-b859-b8c77567d893','N07UPAC'),
     ('aa8f57cc-34f9-4173-97b8-aa2ee6f97c36','Default Unallocated Borough','d73ae6b5-041e-4d44-b859-b8c77567d893','N21UNA'),
     ('6f83f557-126b-47fb-b2bb-16cc467c42b3','YOS','d73ae6b5-041e-4d44-b859-b8c77567d893','N21YOS'),
     ('3ff6710f-a2b2-4671-ab30-811f17719002','YOS NE','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N32YOS'),
     ('8725c641-58e5-4c34-b577-d51b5a695cd2','YOS YaTH','a02b7727-63aa-46f2-80f1-e0b05b31903c','N33YOS'),
     ('1961d2c9-420d-4c75-aa89-93f493ab528d','Bail Information Services','dafc93d2-2dec-42d1-84d9-190e9ccda638','N41EFT'),
     ('ff8d5b68-5bb9-40f2-abc4-4713a40107fa','Effective Practice Team','dafc93d2-2dec-42d1-84d9-190e9ccda638','N41EPT'),
     ('54fccf82-5de7-4b69-aec5-788676b8f009','Prevent','dafc93d2-2dec-42d1-84d9-190e9ccda638','N41PVT'),
     ('b881f2c2-fc44-4b24-be7e-b5d167c9d3da','All Cluster - NSD','dafc93d2-2dec-42d1-84d9-190e9ccda638','N43ALL'),
     ('561e6f65-25cb-48c7-a08b-a028c1bf5a64','All Greater Manchester','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50ALL'),
     ('89ac816b-7c0a-48db-8e0a-b3aa39dd209e','Accredited Programmes and Structured Interventions','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50APSI'),
     ('40967e19-1296-457a-b242-199efe7ac7ac','Personality Disorder Projects','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50PDP'),
     ('f10d5aa3-24bd-4eb0-8a51-60277b8a8cfe','Public Protection Community','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50PPC'),
     ('058f4ef2-f3d7-4ea4-a381-fb97c351981e','Public Protection Residential','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50PPGM'),
     ('17135000-56ae-4f2b-baa0-cd252466fe30','Performance and Quality','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50PQU'),
     ('0eaa47ef-0979-4d05-acdf-35c942801734','Unpaid Work and Attendance Centres','f6db2e41-040e-47c7-8bba-a345b6d35ca1','N50UPAC'),
     ('edd667da-253f-4442-8431-cef3921f922f','All North West','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51ALL'),
     ('c6c6a542-7329-4a8f-a547-6cd8b5b6ca2d','Accredited Programmes and Structured Interventions','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51APSI'),
     ('0300a776-e1d1-4b75-bf24-d21a4203035e','Corporate Services','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51COSE'),
     ('8d90a0e1-1b04-4016-af58-ddadc89212a0','Merseyside Resettle','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51MER'),
     ('5b117464-6a73-4921-8358-6e711bb492e3','Personality Disorder Projects','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51PDP'),
     ('1b40c0c2-6b95-4efa-85be-97de0240a1cb','Public Protection Community','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51PPC'),
     ('138af871-b5d5-4d1e-aaa2-53e04a21d294','Public Protection Residential','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51PPR'),
     ('6da9919c-f663-4ede-adaa-3755829e0974','Performance and Quality','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51PQU'),
     ('cafef201-29bb-461c-b417-793584772d31','Unpaid Work and Attendance Centres','a02b7727-63aa-46f2-80f1-e0b05b31903c','N51UPAC'),
     ('06965c77-69a0-4b35-85e7-2c10d8f30c63','All West Midlands','734261a0-d053-4aed-968d-ffc518cc17f8','N52ALL'),
     ('16a0bc4b-2b25-4feb-9f07-815123fa0b72','Accredited Programmes and Structured Interventions','734261a0-d053-4aed-968d-ffc518cc17f8','N52APSI'),
     ('437c3a5b-0a04-4bae-bc37-e4f71632a8e9','Birmingham Courts and Centralised Functions','734261a0-d053-4aed-968d-ffc518cc17f8','N52BCCF'),
     ('3e6f452b-3f98-472c-a5b9-527bc6b6165f','Community Integration','734261a0-d053-4aed-968d-ffc518cc17f8','N52COIN'),
     ('61cc5ee9-b5f7-409e-abca-f863087f656b','Corporate Services','734261a0-d053-4aed-968d-ffc518cc17f8','N52COSE'),
     ('676e2de6-be51-4f01-afeb-df3fa5ca4970','Headquarters','734261a0-d053-4aed-968d-ffc518cc17f8','N52HDQT'),
     ('22dae7a5-75ac-486b-ae2a-88a11fc4d58e','Public Protection Residential','734261a0-d053-4aed-968d-ffc518cc17f8','N52PPWM'),
     ('351e1ae6-ffdf-4cd4-b38a-f94c6a2d1302','Performance and Quality','734261a0-d053-4aed-968d-ffc518cc17f8','N52PQU'),
     ('96cae017-6ed9-4f53-bebb-3949a9d4d378','Personality Disorder Projects','734261a0-d053-4aed-968d-ffc518cc17f8','N52PROS'),
     ('b59481af-de82-452b-98f0-ebdea9b061ab','Public Protection Community','734261a0-d053-4aed-968d-ffc518cc17f8','N52STEM'),
     ('a8114323-6c43-4b60-9f65-d2018afc106f','Unpaid Work and Attendance Centres','734261a0-d053-4aed-968d-ffc518cc17f8','N52UPAC'),
     ('f36354c7-71d1-4c56-941b-bc31976713ce','All East Midlands Region','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53ALL'),
     ('9e706d14-593d-4229-bd00-90b29dfd94c7','Corporate Services','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53COSE'),
     ('556042d0-d752-452a-9834-5f4984a9662a','Accredited Programmes and Structured Interventions','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53MSOU'),
     ('7d1fbc4a-9e56-4bf9-8d52-80d5d1d2cef7','Public Protection Residential','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53PPEM'),
     ('05c0137b-45d2-466f-8bd4-4eb0fd8a9e56','Performance and Quality','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53PQU'),
     ('868e76d1-47a2-4948-9521-bc7c8f18d648','Public Protection Community','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53STEM'),
     ('8dcf08c4-2b69-4414-8639-30c9b6e726b0','Unpaid Work and Attendance Centres','0544d95a-f6bb-43f8-9be7-aae66e3bf244','N53UPAC'),
     ('5cf7c1ce-ec1e-451e-8140-686fd4e17311','All North East','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54ALL'),
     ('240e0814-a219-4d79-bb63-035f7f7a23b4','Accredited Programmes and Structured Interventions','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54APSI'),
     ('597a0c6b-a824-4004-9f64-4f7f7c603c7e','Community Integration','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54COIN'),
     ('86790971-2a66-4854-a704-97efe059cc54','Corporate Services','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54COSE'),
     ('99e9cb68-beed-4d9d-9bdc-73b4d1aa48bc','Headquarters','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54HDQT'),
     ('a09a49fa-0556-4d9c-a3b5-5f6f1e5315ea','Personality Disorder Projects - North East','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54PDPN'),
     ('30907ca9-5347-45e2-a914-5f2dc0a376e9','Public Protection Community','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54PPC'),
     ('4373dd31-7fb1-4f03-b419-4e8ddf452472','Public Protedtion Residential','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54PPU'),
     ('78312715-8a55-48c6-ae73-5bfbf5beb591','Performance and Quality','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54PQU'),
     ('25a5788f-f937-4c5f-a489-de9a07869121','Unallocated Level(N54)','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54UAT'),
     ('cd09671a-1022-4e44-add2-347b377038e8','Unpaid Work and Attendance Centres','c5acff6c-d0d2-4b89-9f4d-89a15cfa3891','N54UPAC'),
     ('979d6b1a-ac29-4fc2-8cd1-59b2545426b7','All YaTH','5e44b880-df20-4751-938f-a14be5fe609d','N55ALL'),
     ('2d9ba220-1b90-4986-bcb8-ff9b66824823','Accredited Programmes and Structured Interventions','5e44b880-df20-4751-938f-a14be5fe609d','N55APSI'),
     ('065a438a-2a6b-451d-b6da-f561e9c62c64','Community Integration','5e44b880-df20-4751-938f-a14be5fe609d','N55COIN'),
     ('8873403a-c1d6-445d-b617-8f856f8fa7a7','Corporate Services','5e44b880-df20-4751-938f-a14be5fe609d','N55COSE'),
     ('a80f2c7c-9b62-4d13-8a62-377aed14a879','Headquarters','5e44b880-df20-4751-938f-a14be5fe609d','N55HDQT'),
     ('02730cca-f6d0-423b-83f7-7153caae41a9','Personality Disorder Projects','5e44b880-df20-4751-938f-a14be5fe609d','N55PDP'),
     ('1e3d51bf-30aa-49da-b076-f74a0f845668','Public Protection Community','5e44b880-df20-4751-938f-a14be5fe609d','N55PPC'),
     ('7e2791a7-593d-4637-8741-f8d6485b3297','Public Protection Residential','5e44b880-df20-4751-938f-a14be5fe609d','N55PPU'),
     ('613760da-4f7b-4253-a57b-dac0f8d5edd4','Performance and Quality','5e44b880-df20-4751-938f-a14be5fe609d','N55PQU'),
     ('5d208717-7a19-425e-aaff-ca8bdda7da94','Unpaid Work and Attendance Centres','5e44b880-df20-4751-938f-a14be5fe609d','N55UPAC'),
     ('d7819d1d-fc1a-4a69-8c48-4821602791dc','All East of England','ca979718-b15d-4318-9944-69aaff281cad','N56ALL'),
     ('3a9a1729-e005-4789-bbaf-98cec179ceb4','Corporate Services','ca979718-b15d-4318-9944-69aaff281cad','N56CEOE'),
     ('1b1849b1-b6dd-48b9-bc11-a648519252c9','Headquarters','ca979718-b15d-4318-9944-69aaff281cad','N56HDQT'),
     ('01b34631-aedd-4dc0-952f-9ef9a24959a8','Personality Disorder Projects','ca979718-b15d-4318-9944-69aaff281cad','N56PDP'),
     ('2437aa26-25db-49a8-b300-136c06b7c8c2','Public Protection Community','ca979718-b15d-4318-9944-69aaff281cad','N56PPC'),
     ('f7f90426-e5f0-4518-b792-87294c17e934','Public Protection Residential','ca979718-b15d-4318-9944-69aaff281cad','N56PPR'),
     ('f879e8fa-2621-4085-a385-88d77c35774d','Performance and Quality','ca979718-b15d-4318-9944-69aaff281cad','N56PQU'),
     ('964f8376-049b-4b8b-b7a6-f3fe47f34349','Unpaid Work and Attendance Centres','ca979718-b15d-4318-9944-69aaff281cad','N56UPAC'),
     ('c4bf6c78-5525-49fa-b1e9-01af3e619728','All KSS','db82d408-d440-4eb5-960b-119cb33427cd','N57ALL'),
     ('c8278fde-ac25-4c72-8bf3-6ae6b0749977','Accredited Programmes and Structured Interventions','db82d408-d440-4eb5-960b-119cb33427cd','N57APSI'),
     ('bbd343a6-7855-4cc8-97f6-cee20928036b','Corporate Services','db82d408-d440-4eb5-960b-119cb33427cd','N57COSE'),
     ('b9a84bd9-059a-4e28-abdc-23d139079775','Enforcement','db82d408-d440-4eb5-960b-119cb33427cd','N57ENF'),
     ('6f4e6435-7aca-4c98-ae1a-408204e39c47','Personality Disorder Projects','db82d408-d440-4eb5-960b-119cb33427cd','N57PDP'),
     ('ae4fb8f9-0652-4683-9f1a-4b7bb1364039','Public Protection Residential','db82d408-d440-4eb5-960b-119cb33427cd','N57PPR'),
     ('9045c2a8-ad55-493d-8d27-f1deae8ef12a','Performance and Quality','db82d408-d440-4eb5-960b-119cb33427cd','N57PQU'),
     ('3fbf5932-13b3-4073-8faa-e0304ceb4687','Unpaid Work and Attendance Centres','db82d408-d440-4eb5-960b-119cb33427cd','N57UPAC'),
     ('db2b28cd-0b0e-46da-8bbf-7b8aab7bd6f2','All South West','43606be0-9836-441d-9bc1-5586de9ac931','N58ALL'),
     ('41dde4c9-4a1a-429b-af76-31efead0b070','Accredited Programmes and Structured Interventions','43606be0-9836-441d-9bc1-5586de9ac931','N58APSI'),
     ('2fa55454-fb4b-440e-9eaa-3ac74bb34285','Corporate Services','43606be0-9836-441d-9bc1-5586de9ac931','N58COSE'),
     ('8c4db588-0be1-448d-8ac5-664c19b28292','Headquarters','43606be0-9836-441d-9bc1-5586de9ac931','N58HDQT'),
     ('fa132a25-82f8-4030-bddb-7c16e2cbeac0','Personality Disorder Projects','43606be0-9836-441d-9bc1-5586de9ac931','N58PDP'),
     ('34739a78-2672-485f-9e82-59297ce53013','Public Protection Community','43606be0-9836-441d-9bc1-5586de9ac931','N58PPC'),
     ('7d329dff-7382-42c7-8b1d-885c3e5ffcc7','Public Protection Residential','43606be0-9836-441d-9bc1-5586de9ac931','N58PPR'),
     ('fb0b548e-af24-46e7-80d3-fea5838a9ea5','Performance and Quality','43606be0-9836-441d-9bc1-5586de9ac931','N58PQU'),
     ('8a632afc-688c-439a-aaf7-60f0829548bf','Unpaid Work and Attendance Centres','43606be0-9836-441d-9bc1-5586de9ac931','N58UPAC'),
     ('8751a9f5-2c2e-4cbf-bbb3-841995375aad','Performance and Quality','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59ADM'),
     ('aa4ec533-ec71-4ed3-b082-76d07505a9f5','All South Central','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59ALL'),
     ('af95549a-10a8-43dd-ac05-b8600f39d2ea','Accredited Programmes and Structured Interventions','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59APSI'),
     ('a8b6598f-cc1d-4ad7-baa6-b766caa49843','Enforcement','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59ENF'),
     ('efe93531-c721-4df6-b8f3-3e520659757a','Personality Disorder Projects','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59PDP'),
     ('34805a24-cb0c-4c34-a2e3-bb62ddd0f844','Public Protection Community','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59PPC'),
     ('467afdc2-3a61-4449-83e6-c7dfbd909256','Public Protection Residential','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59PPR'),
     ('876dee68-4ab4-418c-8f92-c68fa86507d2','Unpaid Work and Attendance Centres','6b4a1308-17af-4c1a-a330-6005bec9e27b','N59UPAC');