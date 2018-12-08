UPDATE mysql.user SET Password=PASSWORD('odk_unit') WHERE User='root';
FLUSH PRIVILEGES;
CREATE USER 'odk_unit'@'odkdatabase' IDENTIFIED BY 'odk_unit';
CREATE DATABASE odk_unit;
GRANT ALL PRIVILEGES ON odk_unit.* TO 'odk_unit'@'odkdatabase' WITH GRANT OPTION;