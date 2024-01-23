UPDATE
    approved_premises_applications
set
    ap_area_id = (
        SELECT
            ap_area_id
        from
            probation_regions
        where
                id = approved_premises_applications.probation_region_id
    )
where
    approved_premises_applications.probation_region_id is not null;