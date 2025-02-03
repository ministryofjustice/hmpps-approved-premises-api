update bookings
set crn = upper(crn)
where crn ~ '[a-z]'
  and service = 'temporary-accommodation'