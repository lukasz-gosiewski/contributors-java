# Prerequisites

Project was run and tested on ArchLinux, below instructions should work in any default Linux command line.
To build and run this project you need to have Java 11 installed in the system. Below commands are working with assumption, that Java 11 is the default JDK.

# How to build and run

Project is shipped with embedded web server and gradle wrapper. There are few most interesting commands available, as follows:

  * `./gradlew build` - Used to build the project (tests are run accordingly)
  * `./gradlew build -x test` - Building the project without running tests
  * `./gradlew bootRun` - Running project in place, without rebuilding it
  * `./gradlew test` - Executing available tests

When project is started web container is set to respond on `8080` by default. 

# Config

Project is using Github API to gather data. Without authorization rate limits are low, so it is possible to use Github Personal Access Token.
To use the token, env variable `GH_TOKEN` must be set in the context.
