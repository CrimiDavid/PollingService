# A Java Application for Polling Data from an External Endpoint and Storing it in Redis

## Description

This project implements a series of pollers that periodically fetch data from a specified HTTP endpoint and push the retrieved information into a Redis instance. It is designed to monitor different aspects of an event, such as general event information, fight details, round data, and voting results.

The application uses a scheduled executor service to manage the polling intervals for different data types. It dynamically starts and stops certain pollers based on the status of the main event (e.g., starting fight-related pollers when an event is "Live").

## Features

- **Event Polling**: Periodically fetches general event information.
- **Dynamic Polling**: Starts and stops fight, round, and vote data pollers based on the event status.
- **Data Storage**: Connects to Redis to store the fetched data.
- **Configurable Polling Intervals**: Polling frequency can be configured for different data types.
- **Connection Pooling**: Uses Jedis connection pooling for efficient Redis interactions.

## Prerequisites

Before running this application, you need to have the following installed:

- Java Development Kit (JDK) 8 or later
- Maven or Gradle (for dependency management)
- A running Redis instance
- Access to the external HTTP endpoint that the application polls

## Setup and Installation

### 1. Clone the Repository

```bash
git clone <your_repository_url>
cd <your_repository_directory>
```

### 2. Configure Environment Variables

Create a .env file in the root directory of the project with the following variables:
```
REDIS_HOST=<your_redis_host>
REDIS_PORT=<your_redis_port>
REDIS_PASSWORD=<your_redis_password>
```

### 3. Add Dependencies
Using Maven

Add the following dependencies to your pom.xml file:

```
<dependencies>
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>5.1.2</version>
    </dependency>
    <dependency>
        <groupId>io.github.cdimascio</groupId>
        <artifactId>java-dotenv</artifactId>
        <version>5.2.2</version>
    </dependency>
</dependencies>
```

### 4. Build the Project
Using Maven
```
mvn clean install
```

### How to Run

1. Ensure your Redis instance is running and accessible.

2. Ensure the external HTTP endpoint is accessible.

3. Run the compiled Java application.

```
mvn exec:java -Dexec.mainClass="org.example.Main"
```
The application will start the EventInfoPoller, which will in turn manage the other pollers based on the event status.

Project Structure

- **AbstractPoller.java**: An abstract base class for all pollers, providing common functionality like scheduling and stopping.

- **Connections.java**: Handles HTTP requests to the external endpoint and interactions with Redis. Manages shared state like current event and fight IDs.

- **EventInfoPoller.java**: Polls for general event information and manages the lifecycle of the FightInfoPoller.

- **FightInfoPoller.java**: Polls for fight-specific information and manages the lifecycle of the RoundInfoPoller and VotesDataPoller.

- **Main.java**: The entry point of the application, initializes the client and the main event poller.

- **RoundInfoPoller.java**: Polls for round-specific information.

- **VotesDataPoller.java**: Polls for voting data.

### Dependencies

- **org.json**: For parsing JSON responses from the endpoint.

- **redis.clients.jedis**: A client library for interacting with Redis.

- **io.github.cdimascio.dotenv**: For loading configuration from a .env file.

- Java's built-in **HttpClient** (Java 11+).


## Potential Improvements

- **Implement more robust error handling and logging**  
  Enhance the application's resilience and debuggability by incorporating comprehensive error handling mechanisms and structured logging (e.g., using SLF4J with Logback).

- **Add configuration options for endpoint URLs and polling intervals via a configuration file instead of hardcoding**  
  Use a properties or YAML configuration file to externalize settings like endpoint URLs, Redis configuration, and polling intervals for easier maintenance and environment-specific setups.

- **Implement a more sophisticated way to manage the lifecycle of pollers based on more detailed event/fight states**  
  Introduce state machines or event-driven mechanisms to control poller activation and deactivation more precisely.

- **Add unit and integration tests**  
  Use testing frameworks like JUnit and Mockito to write automated tests for core components and their interactions, improving reliability and maintainability.

- **Consider using a more advanced scheduling library**  
  Replace `ScheduledExecutorService` with libraries such as Quartz or Spring Scheduler for more flexible and feature-rich scheduling capabilities.

- **Secure sensitive information (like Redis password) more robustly if deploying in a production environment**  
  Use secure secrets management tools or services (e.g., HashiCorp Vault, AWS Secrets Manager) instead of storing credentials in plain-text files.
