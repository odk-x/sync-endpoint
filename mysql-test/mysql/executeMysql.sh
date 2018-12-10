#!/bin/bash

echo "Running SQL script"
mysql --user=root --password=mysqlPassword odk_unit < /tmp/create_db_and_user.sql
echo "Finished SQL script"