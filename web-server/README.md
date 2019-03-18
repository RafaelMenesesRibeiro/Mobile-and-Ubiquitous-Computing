##### Documentation regarding the available P2P-webserver APIs
##### Summarized below:

* Sign Up
* Log In
* Log Out
* Create Album
* Find Users
* Add Users to Album
* List Album Members
* View Album

--------------------------------

**Sign Up**
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
        "operation" : "signup",
        "username" : "a_username",
        "password" : "a_password"
    }
     ```
* **Success Response:**
  
   **Code:** 200 <br />
   **Content:**
    ```
    {
        "operation" : "signup",
        "code" : 200,
        "message" : "OK",
    }
    ```
 
* **Error Response:**

    **Code:** 422 <br />
    **Content:**
    ```
    {
        "operation" : "signup",
        "code" : 422,
        "message" : "unprocessable entity. request SHOULD NOT be repeated without modification",
        "reason" : "username already exists"
    }
    ```
        
--------------------------------

**Log In**
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
       "operation" : "login",
       "username" : "a_username",
       "password" : "a_ciphered_password"
   }
    ```
* **Success Response:**
 
  **Code:** 200 <br />
  **Content:**
   ```
   {
        "operation" : "login",
        "code" : 200,
        "message" : "OK"
   }
   ```

* **Error Response:**

   **Code:** 401 <br />
   **Content:**
   ```
   {
        "operation" : "login",
        "code" : 401,
        "message" : "request has not been applied because it lacks valid authentication credentials",
        "reason" : "username or password is incorrect"
   }
   ```
       
  --------------------------------
  
  **Log Out**
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
      "operation" : "logout",
      "username" : "a_username"
  }
   ```
* **Success Response:**

    **Code:** 200 <br />
    **Content:**
  ```
  {
       "operation" : "logout",
       "code" : 200,
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
       "operation" : "newalbum",
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
        "operation" : "newalbum", 
        "album_name" : "name_in_post_request",
        "album_id" : 9991310417811031,
        "code" : 200,
        "message" : "OK"
   }
   ```

* **Error Response:**

   **Code:** 403 <br />
   **Content:**
   ```
   {
        "operation" : "newalbum",
        "code" : 403,
        "message" : "server understood the request but refuses to authorize it",
        "reason" : "invalid album name"
   }
   ```

   --------------------------------
   
**Find Users**
----

* **URL**

   /findusers

* **Method:**

    `GET`
 
*  **URL Params**

    ***Required:***
    
        username_pattern=[integer]

* **Data Params**

    Not Available;
        
* **Success Response:**
 
  **Code:** 200 <br />
  **Content:**
   ```
   {
        "operation" : "findusers", 
        "found_users" : [
            {
                "a_user" : [ "an_album_id", "another_album_id"],
                "another_user" : [ ]          
            }
        ]
        "code" : 200,
        "message" : "OK"
   }
   ```

* **Error Response:**

   **Code:** 404 <br />
   **Content:**
   ```
   {
        "operation" : "findusers",
        "code" : 404,
        "message" : "resource not found",
        "reason" : "no users match the existing search parameter"
   }
   ```
       
--------------------------------

**Add Users to Album**
----
  
* **URL**

  /adduserstoalbum

* **Method:**

    `POST`

*  **URL Params**

  Not available;
  
* **Data Params**
  ```
  {
      "operation" : "adduserstoalbum",
      "album_id" :  13125618841614,
      "usernames" : [ "a_username", "another_username" ]
  }
   ```
* **Success Response:**

    **Code:** 200 <br />
    **Content:**
  ```
  {
       "operation" : "adduserstoalbum", 
       "album_id" : 13125618841614,
       "added_users" : [ "a_username", "another_username" ],
       "code" : 200,
       "message" : "OK"
  }
  ```

* **Error Response:**

  **Code:** 404 <br />
  **Content:**
   ```
   {
        "operation" : "adduserstoalbum",
        "code" : 404,
        "message" : "resource not found",
        "reason" : "album does not exists"
   }
   ```
   
   OR
       
     **Code:** 400 <br />
     **Content:**
  ```
  {
       "operation" : "adduserstoalbum",
       "code" : 400,
       "message" : "bad request",
       "reason" : "one or more users do not exists or could not be added"
  }
  ```
      
--------------------------------

**List Album Members**
----
   
* **URL**

   /albummembers

* **Method:**

    `GET`
 
*  **URL Params**

    ***Required:***
    
        album_id=[integer]

* **Data Params**

    Not Available;
 
* **Success Response:**
 
  **Code:** 200 <br />
  **Content:**
   ```
   {
        "operation" : "albummembers", 
        "album_id" : 13125618841614,
        "album_members" : [ "a_username", "another_username" ],
        "code" : 200,
        "message" : "OK"
   }
   ```

* **Error Response:**

   **Code:** 404 <br />
    ```
    {
         "operation" : "albummembers",
         "code" : 404,
         "message" : "resource not found",
         "reason" : "album does not exists"
    }
    ```
 --------------------------------
 
 **View Album**
 ----
    
 * **URL**
 
    /viewalbum
 
 * **Method:**
 
    `GET`
  
 *  **URL Params**
 
     ***Required:***
     
         album_id=[integer]
 
 * **Data Params**
 
     Not Available;
  
 * **Success Response:**
  
   **Code:** 200 <br />
   **Content:**
    ```
    {
         "operation" : "viewalbum", 
         "album_id" : 114618841614,
         "slice_urls" : [ "https://drive.google.com/a_album_slice", "https://dropbox.com/a_album_slice" ],
         "code" : 200,
         "message" : "OK"
    }
    ```
 
 * **Error Response:**
 
    **Code:** 404 <br />
     ```
     {
          "operation" : "albummembers",
          "code" : 404,
          "message" : "resource not found",
          "reason" : "album does not exists"
     }
     ```
    
