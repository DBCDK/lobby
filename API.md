Lobby API
---------

### Applicant resource

  ```json
  {
    "id": "42",
    "category": "bpf",
    "mimetype": "text/plain",
    "state": "PENDING",
    "body":"aGVsbG8gd29ybGQ=",
    "timeOfCreation": null,
    "timeOfLastModification": null,
    "additionalInfo": {
      "localId": "extId",
      "errors": [
        "err1",
        "err2"
      ]
    }
  }
  ```
* id - applicant ID (string)
* category - (string) applicant categorization
* mimetype - (string) applicant body mime-type
* state - (string:{ACCEPTED, PENDING}) applicant state
* body - (string: base64 encoded) applicant binary content
* timeOfCreation - (number) time-of-creation as number of milliseconds since January 1, 1970, 00:00:00 GMT
* timeOfLastModification - (number) time-of-last-modification as number of milliseconds since January 1, 1970, 00:00:00 GMT
* additionalInfo - (JSON object) additional information as arbitrary JSON object

### Create or replace applicant

Creates applicant resource with ID specified by the path or completely replaces and existing applicant.

* **URL**

  /v1/api/applicants/{id}

* **Method**

  `PUT`

* **Success Response**

  * **Code:** 201 Created
    * On create new.
  * **Code:** 200 Ok
    * On replace existing.

* **Error Response**  

  * **Code:** 400 Bad Request
    * When the request has malformed syntax.
  * **Code:** 422 Unprocessable Entity
    * When the syntax of the request entity is correct, but the
      server was unable to process the entity due to invalid data.

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: application/json" http://lobbyhost/v1/api/applicants/42 -d '{"id":"42","category":"bpf","mimetype":"text/plain","state":"PENDING","body":"aGVsbG8gd29ybGQ=","additionalInfo":{"localId": "extId"}}'
  ```
  
### Change applicant state

Changes state of applicant resource with ID specified by the path.

* **URL**

  /v1/api/applicants/{id}/state

* **Method**

  `PUT`

* **Success Response**

  * **Code:** 200 Ok

* **Error Response**

  * **Code:** 410 Gone
    * When an applicant with ID given by path can not be found.
  * **Code:** 422 Unprocessable Entity
    * When the syntax of the request entity is correct, but the
      server was unable to process the entity due to invalid data.

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: text/plain" http://lobbyhost/v1/api/applicants/42/state -d 'ACCEPTED'
  ```


### List applicants

Returns list of applicants (not including body content) matched by optional filters.

* **URL**

  /v1/api/applicants

* **Method**

  `GET`

* **Filters**
  * category - retrieval of a certain kind of applicant only.
  * state - retrieval of applicants with a certain state.

* **Success Response**

  * **Code:** 200 Ok
  * **Content-type**: application/json
 
* **Error Response**  

   * **Code:** 422 Unprocessable Entity
    * When the syntax of the request entity is correct, but the
      server was unable to process the entity due to invalid data.

* **Sample Call:**

  ```bash
  $ curl -v http://lobbyhost/v1/api/applicants?state=PENDING
  ```


### Get applicant content

Returns the BLOB content for the applicant resource with ID specified by the path.

* **URL**

  /v1/api/applicants/{id}/body

* **Method**

  `GET`

* **Success Response**

  * **Code:** 200 Ok
  * **Content-type**: Determined by the mime-type of the body content of the applicant

* **Error Response**  
TBD

* **Sample Call:**

  ```bash
  $ curl -v http://lobbyhost/v1/api/applicants/42/body
  ```