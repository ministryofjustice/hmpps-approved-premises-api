CREATE TABLE room_characteristics (
    room_id UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (room_id, characteristic_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics(id)
)
