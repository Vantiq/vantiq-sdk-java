# Integration Tests

The tests in this folder require the Vantiq server to have a test namespace
loaded into Vantiq with a specific set of types, procedures, etc that
reside in the `project` folder.

## Vantiq Project Setup

The following project must be loaded into an existing Vantiq server for the integration tests.  The
[Vantiq CLI](https://dev.vantiq.com/ui/ide/index.html#/resources) may be used
to load the artifacts into the server:

* `project/types/TestType.json`: A data type for testing
* `project/rules/onTestPublish.vail`: A rule that persists an TestType record when an event is fired on `/test/topic`
* `project/procedures/echo.vail`: A procedure that simply echos the input arguments
* `project/sources/JSONPlaceholder.json`: A REMOTE source

The following CLI command will load these into the Vantiq server:

    %  vantiq -s <profile> import -d src/test/resources/intgTest

Note that `<profile>` specifies the proper server and credentials to use
in `~/.vantiq/profile`.

## Run Integration Tests

Once the project has been loaded, the integration tests can be run from inside IntelliJ or through the Gradle task 
`:intgTest`.

The integration tests also need Java system properties set before they can run. The server url must be provided in 
`server`, e.g. `https://dev.vantiq.com`. Authentication must be provided through either username and password or a
token. The username and password can be placed in `username` and `password`, or the token can be provided through
`token`. To set these properties for Gradle add `-D<variable>=<value>` for each property. For IntelliJ put the same in
the VM options section of the Ron/Debug configurations.

For example:  

    %  ./gradlew intgtest -Dserver=http://localhost:8080 -Dusername=aaaa -Dpassword=mypwd    
