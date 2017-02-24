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

## Deploying to Heroku - Step by Step Setup Guide

> Please note that the following example names are already in use and are just for illustration purposes. 

### Prerequisites
* Git
* Heroku CLI (for detailed information refer to [Heroku CLI][4])

### Clone Repo and Create Heroku App
1. execute `git clone https://github.com/messenger4j/messenger4j-spring-boot-quickstart-template.git`
2. execute `cd messenger4j-spring-boot-quickstart-template`
3. execute `heroku create <heroku app name>`, e.g. `heroku create messenger4j-heroku-app`

### Create Facebook Page
4. open `https://www.facebook.com/pages/create`
5. select a suitable Category, e.g. `Brand or Product` --> `Product / Service`
6. enter a Name (the name of the Chatbot), e.g. `Messenger4j Demo`

### Create and Configure Facebook App
7. open `https://developers.facebook.com/apps`
8. click the 'Add a New App' button
9. enter the Display Name, e.g. `messenger4j-fb-app`
10. select the Category: 'Apps for Messenger'
11. click the 'Create App ID' button
12. Section 'Token Generation': Select your created FB Page, e.g. `Messenger4j Demo`
13. copy the 'Page Access Token' to the clipboard
14. execute `heroku config:set MESSENGER_PAGE_ACCESS_TOKEN=<page access token>`
15. navigate to 'Dashboard'
16. copy the 'App Secret' to the clipboard
17. execute `heroku config:set MESSENGER_APP_SECRET=<app secret>`
18. use a randomly generated string as 'Verify Token', e.g. `retgdkfjsjklsklj34qdfs`
19. execute `heroku config:set MESSENGER_VERIFY_TOKEN=<verify token>`
20. execute `git push heroku master`
21. navigate back to 'Messenger'
22. Section 'Webhooks': Click the 'Setup Webhooks' button
23. enter the Callback URL: `https://<heroku app name>.herokuapp.com/callback`, e.g. `https://messenger4j-heroku-app.herokuapp.com/callback`
24. enter the generated Verify Token, e.g. `retgdkfjsjklsklj34qdfs`
25. select the following Subscription Fields: `messages`, `messaging_postbacks`, `messaging_optins`, `message_deliveries`, `message_reads`, `messaging_account_linking`, `message_echoes`
26. click the 'Verify and Save' button
27. Section 'Webhooks': Select your created FB Page to subscribe your webhook to the page events, e.g. `Messenger4j Demo`
28. click the 'Subscribe' button

### Test your new Chatbot
1. open `https://www.messenger.com`
2. search for your Chatbot using the name of your created FB Page, e.g. `Messenger4j Demo`
3. send a message, e.g. `Hello Chatbot`, `receipt`, `gif`, `generic`

> For more information on how to deploy Spring Boot Apps to Heroku please refer to the [Spring Boot Heroku Guide][3]. 

## License
This project is licensed under the terms of the [MIT license](LICENSE).


[1]: https://github.com/fbsamples/messenger-platform-samples
[2]: https://developers.facebook.com/docs/messenger-platform/guides/setup
[3]: https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku
[4]: https://devcenter.heroku.com/articles/heroku-cli