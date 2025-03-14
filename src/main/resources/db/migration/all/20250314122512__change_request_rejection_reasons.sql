ALTER TABLE public.cas1_change_request_rejection_reasons DROP CONSTRAINT cas1_change_request_rejection_reasons_unique;
ALTER TABLE public.cas1_change_request_rejection_reasons ADD CONSTRAINT cas1_change_request_rejection_reasons_unique UNIQUE (code,change_request_type);

INSERT INTO cas1_change_request_rejection_reasons(id, code, change_request_type, archived) VALUES
('a5085e1e-c817-4820-8cf9-36f12fbfac32','suitableApNotAvailable','PLANNED_TRANSFER',false),
('744888d1-f8c7-4b7a-9350-ec77b63bb1f6','transferNoLongerRequired','PLANNED_TRANSFER',false),
('a9484070-b215-455c-a667-942e49fc64c8','seniorManagementDecision','PLANNED_TRANSFER',false),
('685eb67e-1b80-41b0-a991-d5667d7ecfb0','noSuitableApAvailable','APPEAL',false),
('e6c945ac-7c80-48ae-a07b-b6728261f2fc','appealNoLongerRequired','APPEAL',false),
('ecc50973-b984-432e-8101-cd65dce54bcb','seniorManagementDecision','APPEAL',false);
