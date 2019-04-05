# Mobile-and-Ubiquitous-Computing

# P2Photo



## 1 - Introduction

Build P2Photo, a mobile application that allows the users to share photos with their friends in a privacy-preserving way.  
This will be achieved by ensuring that the application provider will be involved only in maintaining group membership, i.e., allowing users to create new albums, finding new users, and adding or removing users from albums.  
In contrast, all operations involving publishing photos in albums and reading photos from albums must be performed without the provider's awareness.  
Concretely, this means that the photos themselves must be stored and shared between users without the mediation of the provider.  
To this endm the system must support two architectures: cloud-based (for the intermediate checkpoint) and wireless P2P (for the final checkpoint).



## 2 - Specification


### 2.1 - Baseline Functionality

The basic architecture of P2Photo relies on a central server and a client mobile application.

#### 2.1.1 - Mobile Application Functionality

The P2Photo client is a mobile Android application that users install and run on their devices and allow users to perform the following functions:

1. Sign up
2. Log in/out
3. Create albums
4. Find users
5. Add photos to albums
6. Add users to albums
7. List user's albums
8. View album

The sign up operation (F1) allows users to create a new user account in the system; the user must enter a username and a password. The client then contacts the P2Photo server, which must ensure that the new username is unique and adds the user to the user database. If the operation is successful, the user can then log in and start a new session on the client device.

To log into the system (F2), the account credentials must be validated by the server. If they are valid, the server must generate a new session id, keep an internal record associating that session id to the username and return the new session id to the client. However, if the credentials are invalid, the server must return an error. The logout function (F2) ends the current session.

Users can create an album (F3). This operation creates an albumn catalog file in the P2Photo server containing the URL of an album-slice catalog of the creating user. The album-slice is the part of an album that contains the photos contributed by a user to an album. An album-slice is stored in its owner's cloud storage. It includes all the photos that the user has contributed to an album and a text file (the catalog) grouping all the URLs pointing to those photos. 

P2Photo allows logged in users to find other users to create albums (F4). With the usernames returned by the serer, a user can add those other users to the album membership (F6).

Once a user is invited to an album, the application adds an album-slice to the album.

#### 2.1.2 - Cloud-backed architecture

The photos are maintained in storage space allocated on the cloud.  
When the user sign in to P2Photo for the first time, they associate the P2Photo account with an account on Dropbox, Google Drive, or similar cloud storage provider, which P2Photo will use for private storage.  
When a user publishes a photo on a given album, that photo will be stored on the user's private storage. In order to retrieve that photo, the other members of the group with acess permissions to that album will be given a direct URL to the photo. This mechanism will allows members to publish and read photos without involving the P2Photo server.  
The P2Photo server only needs to maintain the metadata about group membership of each album as well as a list of all P2Photo users. That metadata includes a list of user ids, a list of each user's albums and for each user's album, a URL that points to a special file located in the user's private storage space. This special file, named catalog, contains the list of the URLs that point to the phtos published by that user in that specific album. Thus, in order for a user to list all the photos available on a given album, the P2Photo mobile application needs simply to retrieve the album membership from the P2Photo server, and then, for each member, download its respective catalog, parse it, and download all the photos published by that member using the URL contained in the catalog.  

It is assumed that photos cannot be removed, albums deleted or users removed from an album's membership.

#### 2.1.3 - Wireless P2P architecture

ONLY IN FINAL CHECKPOINT

#### 2.1.4 - Server

The server is a web server which is in charge of maintaining the user list, managing album membership and maintaining the album catalogs.


### 2.2 - Advanced Features

1. Security: Design and implementation of a security mechanism to encrypt the catalog files and prevent a malicious application provider from retrieving user's catalogs and photos from their private stores.
2. Availability: Design and implementation of  replication protocol for increasing photo availability in the presence of disconnections. A simple solution solution would be for each device to maintain a reserved sace for caching, and use it for keeping replicas of photos published by other users. However, solutions that involve the partial or total replication of the albums can be considered.


### 2.3 - Testing mechanisms

The mobile app should produce a timestamped log of all operations it performs (including all detections of nearby devices) and provide a GUI mechanism to display that log. The server should produce a timestamped log of all operations it performs and the mobile app should display the server log. If the availability advanced feature is implemented the mobile app GUI should include a mechanism to restrict the amount of storage to store other user's album-slices.



## 3 - Implementation

The target platform for P2Photo is Android version >= 4.0.  
Communication between album group members should be based on WiFi Direct.  
Java.  
Android APIs use is unrestricted. However, third party libraries are not allowed.



## 4 - Development Stages

1. GUI Design: study the requirements of the project and design the graphical user interface of the application. Create an activity wireframe of the application.
2. GUI Implementation: implement all graphical components of your application including the navigation between screens. At this point, do not worry about networking. Use hard-coded data to simulate interaction with the server. Make sure to design your application in a modular fashion.
3. Implement cloud-based architecture: implement the central server and extend the mobile application in order to communicate with the server. Implement the interaction with the cloud storage service.
4. Implement peer to peer architecture: complete the baseline functionality of the project by implementing WiFi Direct communication. You only need to use Termite at this point
5. Advanced features: implement advanced features regarding security and availability
