-- Test-only fixture seed consumed by BookingEquipmentSqlTest.
-- Never packaged as production resources or used to initialize a deployment.
-- Existing fixture parents must match exactly and the booking must be cancelled
-- and empty. Missing/ambiguous parents produce no writes; sequential re-runs
-- keep the original equipment link unchanged. IDs always come from real parents.
INSERT INTO booking_equipment (booking_id, equipment_id, quantity)
SELECT b.id, e.id, 1
FROM bookings b
CROSS JOIN equipment e
WHERE b.purpose = '[nuttachai_673380581-8_04-demo] equipment-link fixture'
  AND b.status = 'CANCELLED'
  AND e.name = 'nuttachai_673380581-8_04 demo equipment'
  AND e.category = 'nuttachai_673380581-8_04-demo'
  AND e.total_quantity >= 1
  AND (SELECT COUNT(*) FROM bookings
       WHERE purpose = '[nuttachai_673380581-8_04-demo] equipment-link fixture'
         AND status = 'CANCELLED') = 1
  AND (SELECT COUNT(*) FROM equipment
       WHERE name = 'nuttachai_673380581-8_04 demo equipment'
         AND category = 'nuttachai_673380581-8_04-demo'
         AND total_quantity >= 1) = 1
  AND NOT EXISTS (
      SELECT 1 FROM booking_equipment linked WHERE linked.booking_id = b.id
  );
