Lobby API
---------

[Todo Description of applicant resource]

### Create or replace applicant

Creates applicant resource with ID specified by the path or completely replaces and existing applicant.

* **URL**

  /v1/api/applicants/{id}

* **Method**

  `PUT`

* **Success Response**

  * **Code:** 201 Created (on create new)
  * **Code:** 200 Ok (on replace existing)

* **Error Response**  
TBD

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: application/json" http://lobbyhost/v1/api/applicants/42 -d '{TBD}'
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
TBD

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: text/plain" http://lobbyhost/v1/api/applicants/42/state -d '{TBD}'
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
  **Content:**
    ```json
    [
        {
          TBD
        }
    ]
    ```
 
* **Error Response**  
TBD

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