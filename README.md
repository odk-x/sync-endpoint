# ODK-X Sync Endpoint

ODK-X Sync Endpoint is an actively maintained project that serves as an implementation of ODK-X Cloud Endpoints. It runs a server inside a Docker container that implements the ODK-X REST Protocol, facilitating data synchronization and application file management with ODK-X Android applications.
The developer [wiki](https://github.com/odk-x/tool-suite-X/wiki) (including release notes) and [issues tracker](https://github.com/odk-x/tool-suite-X/issues) are located under the [**ODK-X Tool Suite**](https://github.com/odk-x) project.

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
   - [Recommended](#recommended)
3. [Installation](#installation)
   - [Cloud-based Setup](#cloud-based-setup)
   - [Manual Setup (on local infrastructure)](#manual-setup-on-local-infrastructure)
4. [ODK-X Sync Endpoint Server Technologies](#odk-x-sync-endpoint-server-technologies)
5. [ODK-X Synchronization Protocol](#odk-x-synchronization-protocol)
6. [Authentication](#authentication)
7. [HTTPS](#https)
8. [LDAP](#ldap)
   - [Default LDAP Accounts](#default-ldap-accounts)
   - [Advanced LDAP Configuration](#advanced-ldap-configuration)
9. [Managing Identity through DHIS2](#managing-identity-through-dhis2)
10. [Contributing to ODK-X Sync Endpoint](#contributing-to-odk-x-sync-endpoint)
11. [Links for Users](#links-for-users)
12. [Warnings](#warnings)

## Setting up Your Environment

#### Prerequisites

 - Java 8 JRE + JDK (Java 11 not supported yet)
 - Apache Maven (>= 3.3.3)
 - Docker (>= 18.09.0)

#### Recommended 
 
 - a Java IDE (e.g. Eclipse or IntelliJ IDEA)


Currently a MAJOR REVISION of the Maven enviornment with Docker setup is being applied to the repository. The documentation will be revised afterwards.

To build artifacts and run tests use the "mvn clean install" command. 

## Installation

You can install ODK-X Sync Endpoint either in a cloud-based environment or on your local infrastructure.

### Cloud-based Setup

[Learn more about setting up ODK-X Sync Endpoint in a cloud-based environment](https://docs.odk-x.org/sync-endpoint-cloud-setup/#sync-endpoint-cloud-setup).

### Manual Setup (on local infrastructure)

[Learn more about manually setting up ODK-X Sync Endpoint on your local infrastructure](https://docs.odk-x.org/sync-endpoint-manual-setup/#sync-endpoint-manual-setup).

---

## ODK-X Sync Endpoint Server Technologies

ODK-X Sync Endpoint is composed of various micro-services that work together inside a Docker swarm. These services ensure the proper functioning of Sync Endpoint, and they include:

- **nginx**: Routes incoming web requests to the appropriate microservices.
- **Sync-Endpoint REST Interface**: Provides the ODK-X REST synchronization protocol.
- **Sync-Endpoint Web UI**: Offers a user interface for the sync-endpoint server.
- **PostgreSQL**: Acts as the default database for Sync-Endpoint, but it can integrate with other databases.
- **phpLDAPadmin**: Provides a web interface for user and group management in OpenLDAP.
- **OpenLDAP**: Authenticates users and manages user roles.

![Sync Endpoint Microservices](/docs/images/endpoint-docker-swarm.png)

---

## ODK-X Synchronization Protocol

ODK-X's synchronization protocol is based on a REST architecture, ensuring data synchronization across multiple devices. It processes data updates at the row level to minimize conflicts and allows for safe API request retries in case of network timeouts.

Learn more about the [ODK-X Sync Protocol](https://docs.odk-x.org/odk-2-sync-protocol/).

---

## Authentication

ODK-X Sync Endpoint integrates with an LDAP directory or Active Directory for user authentication and role management. Basic Authentication is the only supported authentication method.

---

## HTTPS

HTTPS (Hyper Text Transfer Protocol Secure) is used to secure communication between systems. Sync Endpoint supports automatic certificate provisioning via domain validation and letsencrypt. For advanced users, external certificates can be added.

Learn more about HTTPS and certificates [here](https://www.digicert.com/resources/beginners-guide-to-tls-ssl-certificates-whitepaper-en-2019.pdf).

---

## LDAP

### Default LDAP Accounts

- **Admin Account**:
  - Username: cn=admin,dc=example,dc=org
  - Password: admin

- **Readonly Account**:
  - Username: cn=readonly,dc=example,dc=org
  - Password: readonly

These accounts are used for LDAP authentication and user information retrieval.

### Advanced LDAP Configuration

You can configure LDAP settings by modifying the `ldap.env` file and `security.properties` file located in the `sync-endpoint-default-setup` directory.

Learn more about advanced LDAP configuration [here](https://docs.odk-x.org/sync-endpoint/?highlight=group#advanced).

---

## Managing Identity through DHIS2

Sync Endpoint allows you to manage identity through DHIS2 integration. You can configure DHIS2 authentication settings in the `security.properties` file. Sync Endpoint can be set up to use DHIS2 credentials for authentication.

---

## Contributing to ODK-X Sync Endpoint

If you’re new to ODK-X you can check out the documentation:
- [https://docs.odk-x.org](https://docs.odk-x.org)

Once you’re up and running, you can choose an issue to start working on from here: 
- [https://github.com/odk-x/tool-suite-X/issues](https://github.com/odk-x/tool-suite-X/issues)

Issues tagged as [good first issue](https://github.com/odk-x/tool-suite-X/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) should be a good place to start.

Pull requests are welcome, though please submit them against the development branch. We prefer verbose descriptions of the change you are submitting. If you are fixing a bug please provide steps to reproduce it or a link to a an issue that provides that information. If you are submitting a new feature please provide a description of the need or a link to a forum discussion about it.

---

## Links for Users

This document is aimed at helping developers and technical contributors. For information on how to get started as a user of ODK-X, see our [online documentation](https://docs.odk-x.org), or to learn more about the Open Data Kit project, visit [https://odk-x.org](https://odk-x.org).

---

## Warnings

- The database and LDAP directory provided in this setup are meant only for testing and evaluation purposes. For production, configure a production-ready database and LDAP directory.
- Refer to Docker Swarm documentation for running a production-ready Swarm.
- Consider hosting Sync Endpoint on a commercial cloud provider for optimal performance, or consult your System Administrator for on-premise hosting.
- Regularly back up your data and test backups to prevent data loss.

---

For more detailed information and tutorials, visit the official [ODK-X Sync Endpoint Documentation](https://docs.odk-x.org/sync-endpoint/?highlight=group#odk-x-sync-endpoint).
