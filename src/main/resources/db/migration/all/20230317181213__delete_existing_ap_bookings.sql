DELETE FROM confirmations WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM arrivals WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM departures WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM non_arrivals WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM extensions WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM cancellations WHERE booking_id IN (SELECT id FROM bookings WHERE service = 'approved-premises');
DELETE FROM bookings WHERE service = 'approved-premises';
