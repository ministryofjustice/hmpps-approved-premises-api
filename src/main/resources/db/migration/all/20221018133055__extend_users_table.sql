ALTER TABLE "users" DROP COLUMN is_active;
ALTER TABLE "users" DROP COLUMN distinguished_name;
ALTER TABLE "users" ADD COLUMN delius_username TEXT NOT NULL;
ALTER TABLE "users" ADD COLUMN delius_staff_identifier BIGINT NOT NULL;
