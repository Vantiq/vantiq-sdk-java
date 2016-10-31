# Integration Tests

The tests in this folder require the Vantiq server to have a test namespace
loaded into Vantiq with a specific set of types, procedures, etc that
reside in the `project` folder.

## Vantiq Project Setup

The following project must be loaded into an existing Vantiq server.  The
[Vantiq CLI](https://dev.vantiq.com/ui/ide/index.html#/resources) may be used
to load the artifacts into the server:

* `project/type/TestType.type`: A data type for testing
* `project/rule/onTestPublish.rule`: A rule that persists an TestType record when an event is fired on `/test/topic`
* `project/procedure/echo.proc`: A procedure that simply echos the input arguments

The following CLI commands will load these into the Vantiq server:

    % vantiq -s <profile> load type      project/type/TestType.type
    % vantiq -s <profile> load ruleset   project/rule/onTestPublish.rule
    % vantiq -s <profile> load procedure project/procedure/echo.proc

Note that `<profile>` specifies the proper server and credentials to use
in `~/.vantiq/profile`.

## Run Integration Tests

Once the project has been loaded, the integration tests can be run from inside IntelliJ.
