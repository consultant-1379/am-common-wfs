#!/bin/sh
#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

echo "cleanup flyway migration table"
flyway_table="flyway_schema_history"

db_connection="postgresql://${PG_APP_USER}:${PGPASSWORD}@${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"

psql "${db_connection}" -c "delete from ${flyway_table} where script='V7_18_1__create_request_processing_details_table.sql'"

# Check the exit status of the psql command
if [ $? -eq 0 ]; then
    echo "Cleanup finished successfully."
else
    echo "Error dropping schema. Please check the script and try again."
fi

#-----------------------run migration---------------------------
export FLYWAY_URL=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
export FLYWAY_USER=${PG_APP_USER}
export FLYWAY_PASSWORD=${PGPASSWORD}

set -- "camunda" "wfs"

for schema in "$@"
do
fl_location="classpath:db/migration/$schema"
config_file_name="/tmp/$schema.conf"

cat > "${config_file_name}" <<EOF
flyway.url=${FLYWAY_URL}
flyway.user=${FLYWAY_USER}
flyway.password=${FLYWAY_PASSWORD}
flyway.locations=${fl_location}

EOF

echo "Flyway configuration file '${schema}.conf' created successfully."
  if [ "${schema}" = "camunda" ]; then
    /flyway/flyway migrate  -configFiles="${config_file_name}" -loggers=console -baselineOnMigrate=true
  else
    /flyway/flyway migrate  -configFiles="${config_file_name}" -loggers=console -baselineOnMigrate=true -defaultSchema=${schema}
  fi

  if [ $? -eq 0 ]; then
    echo "Migration successful for schema '${schema}'."
  else
    echo "Migration failed for schema '${schema}'."
    exit 1
  fi
done
