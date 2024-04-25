CREATE TABLE referral_rejection_reasons (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    service_scope TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

INSERT INTO referral_rejection_reasons(id,name,is_active,service_scope) VALUES
    ('21b8569c-ef2e-4059-8676-323098d16aa5','They have no recourse to public funds (NRPF)',true,'temporary-accommodation'),
    ('11506230-49a8-48b5-bdf5-20f51324e8a5','They’re not eligible (not because of NRPF)',true,'temporary-accommodation'),
    ('a1c7d402-77b5-4335-a67b-eba6a71c70bf','There was not enough time to place them',true,'temporary-accommodation'),
    ('88c3b8d5-77c8-4c52-84f0-ec9073e4df50','There was not enough time on their licence or post-sentence supervision (PSS)',true,'temporary-accommodation'),
    ('90e9d919-9a39-45cd-b405-7039b5640668','Their risk or needs cannot be safely managed in CAS3',true,'temporary-accommodation'),
    ('155ee6dc-ac2a-40d2-a350-90b63fb34a06','There’s no capacity',true,'temporary-accommodation'),
    ('85799bf8-8b64-4903-9ab8-b08a77f1a9d3','Other (please add)',true,'temporary-accommodation');