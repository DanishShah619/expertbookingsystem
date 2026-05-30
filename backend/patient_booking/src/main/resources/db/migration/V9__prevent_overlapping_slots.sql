CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE time_slots
    ADD CONSTRAINT ex_time_slots_no_overlap
    EXCLUDE USING gist (
        expert_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    );
