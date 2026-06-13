ALTER TABLE exercise_addr
    ADD COLUMN location POINT
        GENERATED ALWAYS AS (
            ST_GeomFromText(
                CONCAT('POINT(', longitude, ' ', latitude, ')'),
                4326,
                'axis-order=long-lat'
            )
        ) STORED NOT NULL SRID 4326;

CREATE SPATIAL INDEX idx_exercise_addr_location
    ON exercise_addr (location);

CREATE INDEX idx_exercise_date_addr
    ON exercise (date, addr_id);
