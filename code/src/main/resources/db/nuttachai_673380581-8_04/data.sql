-- Optional, explicit nuttachai_673380581-8_04 fixture seed for PostgreSQL and H2.
-- Run manually after schema.sql. It never creates/updates parent rows or uses
-- assumed IDs. Prepare parent fixtures through their owners' normal workflow:
--   * exactly one CANCELLED booking with
--       purpose = '[nuttachai_673380581-8_04-demo] equipment-link fixture';
--   * exactly one equipment with name = 'nuttachai_673380581-8_04 demo equipment',
--       category = 'nuttachai_673380581-8_04-demo', and total_quantity >= 1.
-- The cancelled booking keeps this demonstration out of active reservations.
-- Only an otherwise empty fixture booking is seeded, with quantity 1.
-- Missing/ambiguous valid parents or pre-existing links produce ZERO inserts;
-- this is expected and must not be replaced with arbitrary live parent IDs.
-- Run one seed execution at a time while application writers are stopped.
-- Sequential re-runs preserve the same row and quantity (idempotent).
-- No ordinary live booking or equipment row is changed.

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
