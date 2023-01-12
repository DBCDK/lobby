Lobby service
=============

The purpose of this microservice is to provide a means for storing and retrieving applicant
records before they are accepted into the DBC systems infrastructure.

### Configuration

**Environment variables**

* LOBBY_DB_URL database URL (USER:PASSWORD@HOST:PORT/DBNAME) of the underlying lobby store.

### API

The service exposes a RESTful [API](https://raw.githubusercontent.com/DBCDK/lobby/master/API.md).

### Development

**Requirements**

To build this project JDK 1.8 and Apache Maven is required.

To start a local instance, docker is required.

**Scripts**
* clean - clears build artifacts
* build - builds artifacts
* test - runs unit and integration tests
* validate - analyzes source code and javadoc
* start - starts localhost instance
* stop - stops localhost instance

```bash
./scripts/clean && ./scripts/build && ./scripts/test && ./scripts/validate
./scripts/start-database
```
```bash
./scripts/start-server
```
...use service...
```bash
./scripts/stop
```
(and ctrl+c in the postgresql container)

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
