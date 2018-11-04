## Prerequisites

Same as [sync-endpoint-containers](https://github.com/opendatakit/sync-endpoint-containers)

## Build

1. Follow instructions on [sync-endpoint-containers](https://github.com/opendatakit/sync-endpoint-containers) to build `odk/sync_endpoint`
2. Follow instructions on https://github.com/opendatakit/sync-endpoint-web-ui to build `odk/sync-web-ui`
3. Build `db-bootstrap` with `docker build -t odk/db-bootstrap db-bootstrap`
4. Build `openldap` with `docker build -t odk/openldap openldap`
5. Build `phpldapadmin` with `docker build -t odk/phpldapadmin phpldapadmin`

## Run

1. `docker stack deploy -c docker-compose.yml syncldap` to deploy all services
2. Navigate to `https://127.0.0.1:40000` (or whichever IP address and port you set up when you initialized your Docker stack) and create a user. See the [LDAP](#ldap) section below for detail  
   Note: Your browser might warn you about invalid certificate 
3. The Sync Endpoint will take around 30s to start then it will be running at `http://127.0.0.1`

If you don't want the database bootstrap script to run, set the `DB_BOOTSTRAP` environment variable in `db.env` to `false`.

## Clean up

1. Remove the stack with, `docker stack rm syncldap`
2. Remove volumes with, `docker volume rm $(docker volume ls -f "label=com.docker.stack.namespace=syncldap" -q)`

## Configuration

`config` and `docker-compose.yml` hold configuration for different parts. Refer to the individual files for options. 

`jdbc.properties` and `docker-compose.yml` are configured to use PostgreSQL by default but MySQL and MSSQL are also supported. 

## LDAP

The default admin account is `cn=admin,dc=example,dc=org`. The default password is `admin`, it can be changed with the `LDAP_ADMIN_PASSWORD` environment variable in `ldap.env`.

The default readonly account is `cn=readonly,dc=example,dc=org`. The defualt password is `readonly`, it can be changed with the `LDAP_READONLY_USER_PASSWORD` environment variable in `ldap.env`. This account is used by the Sync Endpoint to retrieve user information. 

#### Creating users (with phpLDAPadmin)

1. Click `login` on the left and login as admin
2. Expand the tree view on the left until you see `ou=people`
3. Click on `ou=people` and choose `Create a child entry`
4. Choose the `Generic: User Account` template
5. Fill out the form and click create object
6. Refer to the section below on assigning this user to groups 

A password is required for users to log in to Sync endpoint. 

The `gidNumber` attribute is used by Sync endpoint to determine a user's default group. 

#### Creating groups (with phpLDAPadmin)

1. Click `login` on the left and login as admin
2. Expand the tree view on the left until you see `ou=groups`
3. Click on `ou=default_prefix` and choose `Create a child entry`
4. Choose the `Generic: Posix Group` template
5. Fill out the form and click create object  
   Note: the group name must start with the group prefix, in this case the group prefix is `default_prefix`, e.g. `default_prefix my-new-group`

#### Assigning users to groups (with phpLDAPadmin)

1. Click `login` on the left and login as admin
2. Expand the tree view on the left until you see `ou=default_prefix`, then expand `ou=default_prefix`
3. This list is all the groups under `ou=default_prefix`
4. Click on the group that you want to assign users to 
5. If the `memberUid` section is not present, 
    1. Choose `Add new attribute`
    2. Choose `memberUid` from the dropdown, then enter `uid` of the user you want to assign
    3. Click update object at the bottom to update
6. If the `memberUid` section is present, 
    1. Navigate to the `memberUid` section 
    2. Click modify group members to manage members

#### Using `ldap-utils`

The `ldap-service` container has `ldap-utils` installed. If you'd prefer, you may use that toolset to administer the LDAP directory as well. Use this command to access them, `docker exec $(docker ps -f "label=com.docker.swarm.service.name=${STACK_NAME}_sync" --format '{{.ID}}') <LDAPTOOL> <ARGS>`

## Advanced Configuration 

#### Using a Different Database or LDAP directory 

See [here](http://opendatakit-dev.cs.washington.edu/2_0_tools/release/current_release/cloud_endpoints).

#### Managing Identity through DHIS2 

1. Modify [config/sync-endpoint/security.properties](security.properties) to fill in the `Settings for DHIS2 Authentication` section
2. Set `security.server.authenticationMethod` in `security.properties` to `dhis2`
3. [OPTIONAL] Remove OpenLDAP and phpLDAPadmin from [docker-compose.yml](docker-compose.yml)

After restarting your Sync Endpoint server, you will be able to login to Sync Endpoint using the same credentials you use for your DHIS2 server. DHIS2 organization units and groups, with membership preserved, will be converted to Sync Endpoint groups and accesible through the Sync Endpoint REST API. 

## **Warnings**

 - The database and the LDAP directory set up here are meant only for testing and evaluation. When running in production you should configure a production ready database and a production ready LDAP directory. Using the pre-configured database and directory in production can result in poor performance and degraded availabiltiy.
 - You should refer to Docker Swarm documentation on running a production ready Swarm.
 - We recommend that you host Sync Endpoint on a commercial cloud provider (e.g. Google Cloud Platform, Amazon AWS, Microsoft Azure, etc.) If you want to host Sync Endpoint on premise, you should consult your System Administrator for appropriate hardware.
 - Always make regular backups and test your backups to prevent potential data loss. 

 See the [Advanced Configuration](#advanced-configuration) for instructions on swapping out the database or LDAP directory. 

## Notes

The OpenLDAP container is from [osixia/openldap](https://github.com/osixia/docker-openldap)

The phpLDAPadmin container is from [osixia/phpldapadmin](https://github.com/osixia/docker-phpLDAPadmin)

Refer to their respecitve documentations for usage information. 
