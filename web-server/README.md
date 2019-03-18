##### The present document provides documentation regarding the available P2P-webserver APIs
##### Summarized below:

* Sign up
* Log in
* Log out
* Create Album
* Find users
* Add Photo to Album
* Add User to Album
* List Users belonging to Albums
* View Album

--------------------------------

**Sign up**
----

* **URL**

    /signup

* **Method:**

  `POST`
  
*  **URL Params**

    Not available;
    
* **Data Params**
    ```
    {
        "operation" : "signup"
        "username" : "a_username",
        "password" : "a_password",
    }
     ```
* **Success Response:**
  
   **Code:** 200 <br />
   **Content:**
    ```
    {
        "operation" : "signup"
        "code" : 200
        "message" : "OK"
    }
    ```
 
* **Error Response:**

    **Code:** 422 <br />
    **Content:**
    ```
    {
        "operation" : "signup"
        "code" : 422
        "message" : "unprocessable entity. request SHOULD NOT be repeated without modification.
        "reason" : "username already exists"
    }
    ```
    
    
   --------------------------------
   
   **Log in**
   ----
   
   * **URL**
   
       /login
   
   * **Method:**
   
     `POST`
     
   *  **URL Params**
   
       Not available;
       
   * **Data Params**
       ```
       {
           "operation" : "login"
           "username" : "a_username",
           "password" : "a_ciphered_password",
       }
        ```
   * **Success Response:**
     
      **Code:** 200 <br />
      **Content:**
       ```
       {
            "operation" : "login" 
            "code" : 200
            "message" : "OK"
       }
       ```
    
   * **Error Response:**
   
       **Code:** 401 <br />
       **Content:**
       ```
       {
            "operation" : "login"
            "code" : 401
            "message" : "unauthorized - invalid authentication request"
            "reason" : "username or password is incorrect"
       }
       ```
   
       
  --------------------------------
  
  **Log out**
  ----
  
  * **URL**
  
      /logout
  
  * **Method:**
  
    `POST`
    
  *  **URL Params**
  
      Not available;
      
  * **Data Params**
      ```
      {
          "operation" : "logout"
          "username" : "a_username",
      }
       ```
  * **Success Response:**
    
     **Code:** 200 <br />
     **Content:**
      ```
      {
           "operation" : "logout" 
           "code" : 200
           "message" : "OK"
      }
      ```
   
   --------------------------------
   
   **Create Album**
   ----
   
   * **URL**
   
       /newalbum
   
   * **Method:**
   
     `POST`
     
   *  **URL Params**
   
       Not available;
       
   * **Data Params**
       ```
       {
           "operation" : "newalbum"
           "username" : "a_username",
           "album_name" : "a_name",
           "slice_url" : "http://www.acloudprovider.com/a_album_slice"
       }
        ```
   * **Success Response:**
     
      **Code:** 200 <br />
      **Content:**
       ```
       {
            "operation" : "login" 
            "code" : 200
            "message" : "OK"
       }
       ```
    
   * **Error Response:**
   
       **Code:** 401 <br />
       **Content:**
       ```
       {
            "operation" : "login"
            "code" : 401
            "message" : "unauthorized - invalid authentication request"
            "reason" : "username or password is incorrect"
       }
       ```

