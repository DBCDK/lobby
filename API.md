Lobby API
---------

[Todo Description of newcomer resource]

### Create or replace newcomer

Creates newcomer resource with ID specified by the path or completely replaces and existing newcomer.

* **URL**

  /v1/api/newcomers/{id}

* **Method**

  `PUT`

* **Success Response**

  * **Code:** 201 Created (on create new)
  * **Code:** 200 Ok (on replace existing)

* **Error Response**  
TBD

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: application/json" http://lobbyhost/v1/api/newcomers/42 -d '{TBD}'
  ```
  
### Change newcomer state

Changes state of newcomer resource with ID specified by the path.

* **URL**

  /v1/api/newcomers/{id}/state

* **Method**

  `PUT`

* **Success Response**

  * **Code:** 200 Ok

* **Error Response**  
TBD

* **Sample Call:**

  ```bash
  $ curl -v -X PUT -H "Content-Type: text/plain" http://lobbyhost/v1/api/newcomers/42/state -d '{TBD}'
  ```


### List newcomers

Returns list of newcomers (not including body content) matched by optional filters.

* **URL**

  /v1/api/newcomers

* **Method**

  `GET`

* **Filters**
  * category - retrieval of a certain kind of newcomer only.
  * state - retrieval of newcomers with a certain state.
  * limit - upper bound on the the number of newcomers retrieved.

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
  $ curl -v http://lobbyhost/v1/api/newcomers?limit=2
  ```


### Get newcomer content

Returns the BLOB content for the newcomer resource with ID specified by the path.

* **URL**

  /v1/api/newcomers/{id}/body

* **Method**

  `GET`

* **Success Response**

  * **Code:** 200 Ok
  * **Content-type**: Determined by the mime-type of the body content of the newcomer 

* **Error Response**  
TBD

* **Sample Call:**

  ```bash
  $ curl -v http://lobbyhost/v1/api/newcomers/42/body
  ```