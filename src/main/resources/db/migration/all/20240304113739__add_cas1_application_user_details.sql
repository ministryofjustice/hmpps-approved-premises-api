CREATE TABLE cas_1_application_user_details (
id UUID NOT NULL,
name TEXT NOT NULL,
email TEXT NULL,
telephone_number TEXT NULL,

PRIMARY KEY (id)
);

ALTER TABLE approved_premises_applications
ADD applicant_cas1_application_user_details_id UUID NULL,
ADD case_manager_is_not_applicant Boolean NULL,
ADD case_manager_cas1_application_user_details_id UUID null,
ADD CONSTRAINT applicant_cas1_application_user_details_id_fkey FOREIGN KEY (applicant_cas1_application_user_details_id) REFERENCES cas_1_application_user_details(id),
ADD CONSTRAINT case_manager_cas1_application_user_details_id_fkey FOREIGN KEY (case_manager_cas1_application_user_details_id) REFERENCES cas_1_application_user_details(id);
