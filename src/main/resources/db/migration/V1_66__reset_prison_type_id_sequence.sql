-- The prison_type id sequence has fallen behind existing ids, so adding a prison type fails with a duplicate key.
-- Move it past the highest id, never backwards.
select setval(
    pg_get_serial_sequence('prison_type', 'id'),
    greatest(
        coalesce(max(id), 1),
        coalesce(pg_sequence_last_value(pg_get_serial_sequence('prison_type', 'id')::regclass), 1)
    ),
    true
)
from prison_type;
