# messenger4j-spring-boot-quickstart-template

This project is an application skeleton for a Facebook Messenger Chatbot using Java, Spring Boot, and messenger4j.
You can use it to quickly bootstrap your chatbot projects and write your first Messenger Chatbot within minutes.

It's a pre-configured Maven project containing a sample chatbot application and all required dependencies.

The sample application is a port of the official Messenger Platform [NodeJS Showcase][1].
For information on how to setup your chatbot you can follow along with the [Setup Guide][2].

## Getting Started

### Prerequisites
* Git
* JDK 8 or later
* Maven 3.0 or later

### Clone
To get started you can simply clone this repository using git:
```
git clone https://github.com/messenger4j/messenger4j-spring-boot-quickstart-template.git
cd messenger4j-spring-boot-quickstart-template
```

### Configuration
In order to get your chatbot working you have to provide the following settings:
```
messenger4j.appSecret = ${MESSENGER_APP_SECRET}
messenger4j.verifyToken = ${MESSENGER_VERIFY_TOKEN}
messenger4j.pageAccessToken = ${MESSENGER_PAGE_ACCESS_TOKEN}
```
The configuration is located in `src/resources/application.properties`.

With the default configuration you can provide these values through the environment variables `MESSENGER_APP_SECRET`, `MESSENGER_VERIFY_TOKEN`,
and `MESSENGER_PAGE_ACCESS_TOKEN`.

### Build an executable JAR
You can run the application from the command line using:
```
mvn spring-boot:run
```
Or you can build a single executable JAR file that contains all the necessary dependencies, classes, and resources with:
```
mvn clean package
```
Then you can run the JAR file with:
```
java -jar target/*.jar
```

*Instead of `mvn` you can also use the maven-wrapper `./mvnw` to ensure you have everything necessary to run the Maven build.*

## Deploying to Heroku
For instructions on how to deploy this project to Heroku please refer to the [Spring Boot Heroku Guide][3]. 

## License
This project is licensed under the terms of the [MIT license](LICENSE).


[1]: https://github.com/fbsamples/messenger-platform-samples
[2]: https://developers.facebook.com/docs/messenger-platform/guides/setup
[3]: https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku