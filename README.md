# Soccer World Cup Update System
This project demonstrates proficiency in communication protocols, server-client architecture, and real-time event handling.
This project is a real-time update system for the Soccer World Cup. Users can connect to a server using the STOMP protocol to receive and send updates about the matches. The server, written in Java using a reactor pattern, handles all connections and ensures reliable communication between clients. Clients, implemented in C++, can log into specific channels to receive updates and also send their own updates.


## Features

- **Real-Time Updates:** Users receive real-time updates about the Soccer World Cup matches.
- **STOMP Protocol:** Communication between clients and the server is facilitated using the STOMP protocol.
- **Channel-based Communication:** Users can log into specific channels to receive updates relevant to their interests.
- **Summary Requests:** Users can request summaries of game events for a certain channel.

## Technologies Used

- **Server Language:** Java
- **Client Language:** C++
- **Communication Protocol:** STOMP (Simple Text Oriented Messaging Protocol)
- **Concurrency:** Reactor pattern for handling multiple client connections.

## Installation and Usage

- **Server Setup:** Compile and run the Java server. Ensure that the server is accessible to clients.
- **Client Setup:** Compile and run the C++ client. Connect to the server and log into specific channels to receive updates.


