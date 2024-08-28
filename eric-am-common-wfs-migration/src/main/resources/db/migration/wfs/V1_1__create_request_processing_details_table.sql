DO $$
    BEGIN

        IF NOT EXISTS(
            SELECT schema_name
            FROM information_schema.schemata
            WHERE schema_name = 'wfs'
        )
        THEN
            EXECUTE 'CREATE SCHEMA wfs';
        END IF;

    END
$$;

--create types
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'processing_state_enum') THEN
            CREATE TYPE processing_state_enum AS ENUM ('STARTED', 'FINISHED');
        END IF;
    END
$$;

-- -----------------------------------------------------
-- Table `request_processing_details`
-- -----------------------------------------------------
DO $$
    BEGIN

        IF NOT EXISTS(
            SELECT table_name
            FROM information_schema.tables
            WHERE table_name = 'wfs'
        )
        THEN
            EXECUTE 'CREATE TABLE IF NOT EXISTS wfs.request_processing_details (
                     request_id VARCHAR UNIQUE,
                     request_hash VARCHAR NOT NULL,
                     response_code INTEGER,
                     response_headers VARCHAR,
                     response_body VARCHAR,
                     processing_state processing_state_enum NOT NULL,
                     retry_after INTEGER NOT NULL,
                     creation_time TIMESTAMP NOT NULL,

                     PRIMARY KEY (request_id)
                    );';
        END IF;

    END
$$;

