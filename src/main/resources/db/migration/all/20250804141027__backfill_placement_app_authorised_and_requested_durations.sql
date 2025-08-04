update placement_applications
set requested_duration_days = duration
where requested_duration_days is null and duration is not null;

update placement_applications
set authorised_duration_days = duration
where authorised_duration_days is null and decision = 'ACCEPTED';
