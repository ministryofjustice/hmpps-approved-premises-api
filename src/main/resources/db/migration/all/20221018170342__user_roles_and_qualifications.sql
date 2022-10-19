CREATE TABLE user_role_assignments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    role TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES "users"(id)
);

CREATE TABLE user_qualification_assignments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    qualification TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES "users"(id)
);
