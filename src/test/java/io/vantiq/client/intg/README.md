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
* `project/services/testService.json`: A service with an outbound event defined
* `project/procedures/testService_publishToOutbound.vail`: A procedure within that service that publishes to the
service's outbound event

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

### HTTP(S) Proxy Integration Tests

To run the integration tests verifying that they work with a network proxy, you will need
to add a few more properties to the `gradle` command line.

Assume that we have a proxy running on the local machine (port 3128) that does NOT
require authentication. To run the integration tests using that configuration, use

```shell
%  ./gradlew intgtest -Dserver=http://localhost:8080 -Dusername=aaaa -Dpassword=mypwd \
    -DproxyHost=localhost -DproxyPort=3128
```

If you have the same configuration but it does require BASIC user/password authentication, use
the following command.

```shell
%  ./gradlew intgtest -Dserver=http://localhost:8080 -Dusername=aaaa -Dpassword=mypwd \
    -DproxyHost=localhost -DproxyPort=3128 -DproxyUser=proxyuser -DproxyPassword=proxypwd
```

where _proxyuser_ and _proxypwd_ are replaced by the appropriate
values for your proxy configuration.

(Note -- for running the tests, the `build.gradle` file will set these
properties using the server's scheme.  That is, the executing test
will see `http.proxyUser` if the `server` property is `http://...`.)

Note that you will need to adjust the properties used to reflect the server URL scheme
(`http.proxyUser` for a server URL of `http://localhost:8080`). That is, for IntelliJ,
put the adjusted values in the VM options section of the Ron/Debug configurations.

The `src/test/resources/squidProxy` directory has sample files for configuration of a Squid proxy (v5.9).
These are just samples -- there is no requirement to make use of them.
