CREATE TABLE rooms (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    notes TEXT,
    premises_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (premises_id) REFERENCES premises(id)
);

CREATE TABLE beds (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    room_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);
