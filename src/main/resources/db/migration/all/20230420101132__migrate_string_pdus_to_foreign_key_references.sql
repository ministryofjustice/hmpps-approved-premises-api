ALTER TABLE temporary_accommodation_premises ADD COLUMN probation_delivery_unit_id UUID;
ALTER TABLE temporary_accommodation_premises ADD FOREIGN KEY (probation_delivery_unit_id) REFERENCES probation_delivery_units(id);

-- The following UPDATE statements match the PDU names used in the frontend (until it was updated to use the
-- reference data endpoint) to the official names within the database.
-- That list can be found here:
-- https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui/blob/8ed6b79a42ee7d398c577e4e6733ed273d0d29b7/server/data/pdus.json
--
-- 105 of the 108 names in the list are either exactly equal to or are prefixes of the official names, and so match
-- using `LIKE '<name>%'` in the `WHERE` clause.
-- One ('Newcastle Upon Tyne') is equal, but only when a case-insensitive comparison is used
-- (officially it is 'Newcastle upon Tyne'), which can be matched using `ILIKE` instead of `LIKE`.
-- Two do not match due to typographical errors, but a suitable prefix can be found which will successfully match
-- ('Redcar%' and 'Leicester%').

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '616497bf-3e6b-40e2-830b-8bfaeeaec157'
WHERE pdu ILIKE 'County Durham and Darlington%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '864013e9-70eb-4ea9-b375-fa529a0d5f75'
WHERE pdu ILIKE 'Gateshead and South Tyneside%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '4fe6036e-dd1a-4d4e-b698-53c55837ede2'
WHERE pdu ILIKE 'Stockton and Hartlepool%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '56cddbf7-d8c4-44c3-97f6-3230949045a7'
WHERE pdu ILIKE 'Sunderland%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '1b74c3ef-6533-4780-9faf-1dfdfef75cfe'
WHERE pdu ILIKE 'Newcastle Upon Tyne%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ec5e98a5-bcaf-46a3-a0af-2c5f1decba01'
WHERE pdu ILIKE 'North Tyneside and Northumberland%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '2f35498a-154c-4728-a340-a58bc42f1d48'
WHERE pdu ILIKE 'Barnsley and Rotherham%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'c4421153-bb31-4ffa-909d-eec4bdae2242'
WHERE pdu ILIKE 'Sheffield%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '44b0e884-1260-4130-9de5-4d67568d2723'
WHERE pdu ILIKE 'Doncaster%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '0eb0edf2-8eed-4df0-8c27-7945795ba514'
WHERE pdu ILIKE 'Leeds%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '428cc14c-0d31-4f69-bc24-8fed27523745'
WHERE pdu ILIKE 'Wakefield%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd26a7f00-78af-4ea2-b004-fb944e26b8d9'
WHERE pdu ILIKE 'Kirklees%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '31890865-2a58-4511-b40d-e31541ea79a8'
WHERE pdu ILIKE 'Bradford and Calderdale%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'c59a2c49-dae2-46b4-af64-a6021046da8e'
WHERE pdu ILIKE 'York%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '86d5d3cd-76a8-4362-b6ca-2e520dec540b'
WHERE pdu ILIKE 'North Yorkshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '362ca8a4-fa5c-479f-b39c-e2a42ec4bb3f'
WHERE pdu ILIKE 'Hull and East Riding%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f3e801a4-1d41-4989-8415-93d5e0707736'
WHERE pdu ILIKE 'North and North East Lincolnshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '74ecb4ed-5df0-47a3-8df6-6897236f1b87'
WHERE pdu ILIKE 'Nottingham City%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd3cda1be-4504-49fc-95db-16ea8d3953a5'
WHERE pdu ILIKE 'Nottinghamshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '6d720c1a-4dc3-4541-b0f5-bee02deb2cd1'
WHERE pdu ILIKE 'Derby City%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'be06d01c-a8d1-4040-9475-edacff9bba54'
WHERE pdu ILIKE 'Derbyshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f6eda286-fe8f-4ace-8390-8feb30bc8188'
WHERE pdu ILIKE 'East and West Lincolnshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '6f410070-e848-4f4f-a285-6b6fa36dc539'
WHERE pdu ILIKE 'Essex North%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f6fd8e4d-50a4-438d-83b8-335c538b8b66'
WHERE pdu ILIKE 'Essex South%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '3fa6a349-5f48-4c42-8d96-938bda727c35'
WHERE pdu ILIKE 'Suffolk%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'e5feabec-7f00-48ea-a9c0-27afdd492586'
WHERE pdu ILIKE 'Norfolk%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '385ad084-4da4-42e1-ba1c-6f0428bd4f8a'
WHERE pdu ILIKE 'Hertfordshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '0a33aeef-2581-4cfe-87a5-1870018e3a7d'
WHERE pdu ILIKE 'Northamptonshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ce1ee6e6-f510-4442-b9af-a658b94d3a82'
WHERE pdu ILIKE 'Bedfordshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '5e76bd5a-ac0f-4f11-aba9-b7824058680a'
WHERE pdu ILIKE 'Cambridgeshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f97a944d-72d4-4423-8b15-2db8e57bb012'
WHERE pdu ILIKE 'East Kent%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '1fb21be8-6f2b-483d-918f-9aad2e6e2838'
WHERE pdu ILIKE 'West Kent%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '25fb6114-ed80-460b-bd88-631b49b27304'
WHERE pdu ILIKE 'Surrey%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '81ec3d88-eecd-49d7-ad90-5dff98080dfb'
WHERE pdu ILIKE 'East Sussex%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '0cfab9ff-0547-41e7-af3d-20d040337f88'
WHERE pdu ILIKE 'West Sussex%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '5ca0224a-bd1b-4292-bd04-b78ec2875950'
WHERE pdu ILIKE 'Hampshire North and East%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '97276def-4a36-4754-a4fa-bccd72689755'
WHERE pdu ILIKE 'Hampshire South West%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '7063796d-4494-4c3a-8c52-f6a883168376'
WHERE pdu ILIKE 'Hampshire South and Isle of Wight%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '01b4559d-1ca1-42eb-a535-f21eb4842fb4'
WHERE pdu ILIKE 'Oxfordshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '4512987f-1997-450d-9c29-e2bb5b008426'
WHERE pdu ILIKE 'Buckinghamshire and Milton Keynes%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '046e67a2-0095-4bdf-b532-a009c067d211'
WHERE pdu ILIKE 'East Berkshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f189d30b-cb41-4b39-b793-aaa6494aa933'
WHERE pdu ILIKE 'West Berkshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '168814d9-38c1-4b57-9c99-cd41b08aaca9'
WHERE pdu ILIKE 'Gloucestershire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'c30a0405-fdc8-43be-b411-9e7d55a91108'
WHERE pdu ILIKE 'Swindon and Wiltshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '0c3bbfea-a314-4479-ae3f-9a90e1203acf'
WHERE pdu ILIKE 'Dorset%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ba31df91-b4b2-4f15-8a1e-8a137c4c22a7'
WHERE pdu ILIKE 'Bristol and South Gloucestershire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ae11b075-3915-4422-8d4f-d75c27ccbcec'
WHERE pdu ILIKE 'Plymouth%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'a24d0dad-41e5-4418-a74c-d795e02f5083'
WHERE pdu ILIKE 'Cornwall and Isles of Scilly%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'de1bf2c5-fd1b-4a73-b725-f4fc38a67dc8'
WHERE pdu ILIKE 'Devon and Torbay%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '24cb976b-1040-470e-a578-6998af259a26'
WHERE pdu ILIKE 'Somerset%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '13bda6e7-0b50-409e-9535-e016ac8999b6'
WHERE pdu ILIKE 'Bath and North Somerset%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '907fa816-cebd-4425-ac59-66ae88d2931d'
WHERE pdu ILIKE 'North Wales%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '7f6c388c-958a-4646-91ed-a5f040b7ed5f'
WHERE pdu ILIKE 'Gwent%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '4fb4cb8a-44a6-489d-bddb-122d0aae6f83'
WHERE pdu ILIKE 'Dyfed Powys%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'c38b9a56-bc95-42ef-abbe-6d9018dac4d4'
WHERE pdu ILIKE 'Cardiff and the Vale of Glamorgan%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f017624d-b939-47d1-89a8-f83435eddf68'
WHERE pdu ILIKE 'Swansea, Neath Port Talbot%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '574f29fb-3ca7-41ae-a697-fb6c5d2b025e'
WHERE pdu ILIKE 'Cwm Taf Morgannwg%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '1729ceec-e190-4437-bdf9-6e1d1de9780a'
WHERE pdu ILIKE 'Birmingham North, East and Solihull%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd922fdb6-bb13-4443-b757-9f21a8a01467'
WHERE pdu ILIKE 'Birmingham Central and South%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '359a2eaa-c1d3-4b5e-84e4-61f84c4ef706'
WHERE pdu ILIKE 'Dudley and Sandwell%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '4c7c3836-b514-413f-98a6-f0f63541f34f'
WHERE pdu ILIKE 'Walsall and Wolverhampton%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '27c740c9-e686-4d02-8ec5-dbf3ee47b69d'
WHERE pdu ILIKE 'Coventry%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'eb601c92-e48f-42e4-b41b-00b96226ca65'
WHERE pdu ILIKE 'Warwickshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ce151fa8-1075-4820-8b8b-add88a3608c6'
WHERE pdu ILIKE 'Staffordshire and Stoke%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '9405ee2c-aab9-4e47-9cf5-940a2d5b9691'
WHERE pdu ILIKE 'Worcestershire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '81fa0da4-f84d-41e2-8154-df27622b4bb9'
WHERE pdu ILIKE 'Herefordshire, Shropshire and Telford%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '6443b5b3-41d1-42a3-bff6-0352f43a9b6f'
WHERE pdu ILIKE 'Salford%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'fbaff245-5559-4637-b920-1fb53735eda9'
WHERE pdu ILIKE 'Manchester North%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '7f11c76e-ff6c-4bc1-8c68-deaa10e95b4a'
WHERE pdu ILIKE 'Manchester South%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'e8b7accf-80c9-405c-b741-b7cbd8c74448'
WHERE pdu ILIKE 'Bolton%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '8e4e4dfd-460a-4703-a8b6-07ae69076d5b'
WHERE pdu ILIKE 'Wigan%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '4780a07f-b3a5-4c1c-b44d-4216b16ddfc5'
WHERE pdu ILIKE 'Bury and Rochdale%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'a7c2e538-e8b7-4ed6-91bb-93bd3bcd2e2e'
WHERE pdu ILIKE 'Stockport and Trafford%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd721890a-1a52-4809-beb9-e9ecb8a162ee'
WHERE pdu ILIKE 'Tameside%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '5933fe35-b3ea-4b18-94f7-2a46ec731059'
WHERE pdu ILIKE 'Oldham%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '1c93ceae-c22c-46f5-972f-0e4ea7a933e4'
WHERE pdu ILIKE 'West Cheshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '15515a87-0297-482a-811f-e506673dcd5e'
WHERE pdu ILIKE 'East Cheshire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '18134a02-2515-4349-8f1a-c91598f67840'
WHERE pdu ILIKE 'Warrington and Halton%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '355e4c65-bb1c-4f92-b0a5-c5f7e467e222'
WHERE pdu ILIKE 'Knowsley and St Helens%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'c443978e-c4dc-410e-b912-e57709e5e07c'
WHERE pdu ILIKE 'Sefton%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '3f26af1e-0811-4a9c-9613-5a8cc3cfc11c'
WHERE pdu ILIKE 'Liverpool North%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd132b17b-76db-4f62-a1b9-a396cb4c753f'
WHERE pdu ILIKE 'Liverpool South%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '18ac2803-6d81-455c-9211-c459f03a8db7'
WHERE pdu ILIKE 'Cumbria%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ecadde6e-8f37-442f-8a7c-67072b85beda'
WHERE pdu ILIKE 'Wirral%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '0c417f99-eecf-48dc-8d95-b6842adae0ff'
WHERE pdu ILIKE 'North West Lancashire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '929198ec-0292-4657-ad55-1584353809c8'
WHERE pdu ILIKE 'Blackburn%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'b7f894e2-dce2-495d-b0bd-e5ac8d73f498'
WHERE pdu ILIKE 'East Lancashire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '66b547ef-771c-4012-9164-ec8f026c2c30'
WHERE pdu ILIKE 'Central Lancashire%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'a7bc01ec-d34e-4c02-aef9-dd8e39937be2'
WHERE pdu ILIKE 'Greenwich and Bexley%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '9b88cd1a-d310-4bca-a5a0-2ef3619a7332'
WHERE pdu ILIKE 'Lewisham and Bromley%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '40fbcb2d-9c3e-4669-a087-e35c5ee228bb'
WHERE pdu ILIKE 'Southwark%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '90a2ae06-afdc-483b-a292-7f8e427df89a'
WHERE pdu ILIKE 'Lambeth%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '2e3df9de-5875-44b6-8df7-88e4308e5c58'
WHERE pdu ILIKE 'Croydon%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '3faac803-19f4-4864-87a6-868a945b4378'
WHERE pdu ILIKE 'Kingston, Richmond and Hounslow%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '80f5736d-6402-4e93-abc6-c49423163aac'
WHERE pdu ILIKE 'Hammersmith, Fulham, Kensington, Chelsea and Westminster%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '876d7b90-ffb0-4281-96e2-49dd5937a654'
WHERE pdu ILIKE 'Camden and Islington%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '87b54120-8012-44d6-a1fa-e3a4635f3eb5'
WHERE pdu ILIKE 'Ealing and Hillingdon%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'db67fbeb-16d2-40f2-b228-e18e66bd36a9'
WHERE pdu ILIKE 'Wandsworth, Merton and Sutton%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '69eb45a3-b3ce-4a2b-9d57-e727039c3b7d'
WHERE pdu ILIKE 'Brent%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'ff3c44f7-eb45-4f74-bc03-dc87bff5f4df'
WHERE pdu ILIKE 'Harrow and Barnet%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '8ade0b5a-fbdb-4d4d-8cf7-e7846182f486'
WHERE pdu ILIKE 'Hackney and City%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '8b631d28-3688-4b78-b1a7-bb34c49cb4a9'
WHERE pdu ILIKE 'Tower Hamlets%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'd74e0504-f519-4c6a-a412-c39366a92d23'
WHERE pdu ILIKE 'Enfield and Haringey%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'cc27d96a-ae70-407f-a6df-bfb69dda42c9'
WHERE pdu ILIKE 'Newham%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'e2391e6e-027b-47a0-84f8-55700fa76291'
WHERE pdu ILIKE 'Redbridge and Waltham Forest%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = '854b93b5-0e60-471d-b714-549609187199'
WHERE pdu ILIKE 'Barking, Dagenham and Havering%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f5ca7e99-03a4-47dd-bad3-57722b35b5ea'
WHERE pdu ILIKE 'Redcar%';

UPDATE temporary_accommodation_premises
SET probation_delivery_unit_id = 'f5ac7be7-2605-4431-92e0-05778c3a0669'
WHERE pdu ILIKE 'Leicester%';

-- At this stage, any Temporary Accommodation premises that still has `probation_delivery_unit_id = NULL` is either
-- because `pdu` explicitly has the value 'Not specified' (as it predated the introduction of the `pdu` column) or
-- because it somehow was created with garbage data.
-- Either way, it should be safe to drop the `pdu` column, as the second case shouldn't be possible in production and
-- the first case can be (or already has been) resolved by users through the 'edit a premises' functionality.
-- However, the new column `probation_delivery_unit_id` should remain nullable, *just in case*.

ALTER TABLE temporary_accommodation_premises DROP COLUMN pdu;
