 /*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

CREATE TYPE applicant_state AS ENUM ('PENDING', 'ACCEPTED');

CREATE TABLE applicant (
  id                      TEXT PRIMARY KEY,
  category                TEXT NOT NULL,
  mimetype                TEXT NOT NULL,
  state                   applicant_state NOT NULL,
  timeOfCreation          TIMESTAMP WITH TIME ZONE DEFAULT clock_timestamp(),
  timeOfLastModification  TIMESTAMP WITH TIME ZONE,
  additionalInfo          JSONB
);
CREATE INDEX applicant_category_index ON applicant(category);
CREATE INDEX applicant_state_index ON applicant(state);

CREATE TABLE applicant_body (
    id   TEXT PRIMARY KEY REFERENCES applicant(id) ON DELETE CASCADE,
    body BYTEA NOT NULL
);