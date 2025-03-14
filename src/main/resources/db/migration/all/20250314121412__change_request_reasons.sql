ALTER TABLE public.cas1_change_request_reasons DROP CONSTRAINT cas1_change_request_reasons_unique;
ALTER TABLE public.cas1_change_request_reasons ADD CONSTRAINT cas1_change_request_reasons_unique UNIQUE (code,change_request_type);

INSERT INTO cas1_change_request_reasons(id, code, change_request_type, archived) VALUES
('6d711c72-c913-4fce-9f58-7321a4e03f04','extendingThePlacementNoCapacityAtCurrentAp','PLANNED_TRANSFER',false),
('029d5071-05c5-407b-a42d-30fb0e56b1ee','placementPrioritisation','PLANNED_TRANSFER',false),
('dffde82d-3a64-44b1-94e2-20abb5c832bf','movingPersonCloserToResettlementArea','PLANNED_TRANSFER',false),
('ea685d59-099b-441e-af05-106bb61bfefc','conflictWithStaff','PLANNED_TRANSFER',false),
('f977d166-af76-46f8-aaff-861327a176e3','localCommunityIssue','PLANNED_TRANSFER',false),
('f4a09788-7c14-4300-856b-75ebd4e9610f','riskToResident','PLANNED_TRANSFER',false),
('151fe5a8-42e8-4da0-b125-da92e59a2452','publicProtection','PLANNED_TRANSFER',false),
('a744c1d0-96e6-4bac-831c-717453760ad1','apClosure','PLANNED_TRANSFER',false),
('bfba6c00-7d9c-4c63-a94a-c10ce2f720fd','other','PLANNED_TRANSFER',false),
('b62224cb-2742-499c-a018-09864ffaca68','staffConflictOfInterest','APPEAL',false),
('77ddf8e3-4d1b-4da2-85f0-ad520bfb5e6e','exclusionZoneOrProximityToVictim','APPEAL',false),
('7c02ab59-df56-48f8-9560-68ff6a0fed5c','offenceNotAccepted','APPEAL',false),
('c99d2661-8778-4bb2-b723-ca93b57c264e','apCannotMeetSpecificNeeds','APPEAL',false),
('af90d667-c33f-4624-8e60-b8692db97d80','residentMixOrNonAssociates','APPEAL',false),
('6e404a58-622b-4d2d-af99-6584a7d820ec','roomRequestedNotAvailable','APPEAL',false),
('276a3989-8d0d-4b70-a7af-a299625ca8af','apIsFullOrOverbooked','APPEAL',false);
