CREATE TABLE notify_guest_list_users (
    user_id UUID NOT NULL,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
