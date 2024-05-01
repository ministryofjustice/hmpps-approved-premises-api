ALTER TABLE referral_rejection_reasons
    ADD COLUMN sort_order integer NOT NULL DEFAULT 0;

Update referral_rejection_reasons
Set sort_order = 1
Where id = '21b8569c-ef2e-4059-8676-323098d16aa5';

Update referral_rejection_reasons
Set sort_order = 2
Where id = '11506230-49a8-48b5-bdf5-20f51324e8a5';

Update referral_rejection_reasons
Set sort_order = 3
Where id = 'a1c7d402-77b5-4335-a67b-eba6a71c70bf';

Update referral_rejection_reasons
Set sort_order = 4
Where id = '88c3b8d5-77c8-4c52-84f0-ec9073e4df50';

Update referral_rejection_reasons
Set sort_order = 5
Where id = '90e9d919-9a39-45cd-b405-7039b5640668';

Update referral_rejection_reasons
Set sort_order = 6
Where id = '155ee6dc-ac2a-40d2-a350-90b63fb34a06';

Update referral_rejection_reasons
Set sort_order = 7
Where id = 'b19ba749-408f-48c0-907c-11eace2dcf67';

Update referral_rejection_reasons
Set sort_order = 8
Where id = '311de468-078b-4c39-ae42-8d41575b7726';

Update referral_rejection_reasons
Set sort_order = 9
Where id = '85799bf8-8b64-4903-9ab8-b08a77f1a9d3';