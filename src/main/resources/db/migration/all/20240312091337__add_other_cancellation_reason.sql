insert into "cancellation_reasons"
    ("id", "is_active", "name", "service_scope")
values
    (
        '1d6f3c6e-3a86-49b4-bfca-2513a078aba3',
        true,
        'Other',
        'approved-premises'
    );

ALTER TABLE "cancellations" ADD COLUMN "other_reason" TEXT NULL;