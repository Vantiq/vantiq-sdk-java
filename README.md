<div style="height: 50px"><img style="float:right" alt="Vantiq Logo" src="http://vantiq.com/wp-content/uploads/2015/12/vantiq.png"/></div>

[![Build Status](https://travis-ci.org/Vantiq/vantiq-sdk-java.svg?branch=master)](https://travis-ci.org/Vantiq/vantiq-sdk-java)
[ ![Download](https://api.bintray.com/packages/vantiq/maven/vantiq-sdk/images/download.svg) ](https://bintray.com/vantiq/maven/vantiq-sdk/_latestVersion)

# Vantiq SDK for Java

The [Vantiq](http://www.vantiq.com) Java SDK is a Java library that provides an API into a Vantiq system for Android and standalone Java applications.  The SDK connects to a Vantiq system using the [Vantiq REST API](https://dev.vantiq.com/docs/api/developer.html#api-reference-guide).

## Installation

The SDK is published through the Bintray Maven repository.  To include this into
your project, you can add the dependency.

In Gradle,

    repositories {
        maven {
            url "https://dl.bintray.com/vantiq/maven"
        }
    }
    
    dependencies {
        compile 'io.vantiq:vantiq-sdk:1.0.0'
    }

In Maven,

    <repositories>
        <repository>
            <id>Vantiq Maven Repo</id>
            <url>https://dl.bintray.com/vantiq/maven</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>io.vantiq</groupId>
            <artifactId>vantiq-sdk</artifactId>
            <version>1.0.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>    

The Vantiq SDK for Java requires Java 7 or later.

## Quick Start

You will need valid credentials on a Vantiq server in the form of a username and password.  If you have a private Vantiq server, contact your administrator for credentials.  If you wish to use the Vantiq public cloud, contact [support@vantiq.com](mailto:support@vantiq.com).

The first step is to create an instance of the Vantiq SDK providing the URL of the Vantiq server to connect:

```java
String server = "https://dev.vantiq.com";

Vantiq vantiq = new io.vantiq.Vantiq(server);
```

where `server` is the full URL for the Vantiq server to connect to, such as *https://dev.vantiq.com/*.  An optional second argument is the version of the API to connect to.  If not specified, this defaults to the latest version, currently *1*.  At this point, the *Vantiq* instance has not yet connected to the server.  To establish a connection to the server, use the `authenticate` method, e.g.,

```java
String username = "joe@user";
String password = "my-secr3t-passw0rd!#!"

vantiq.authenticate(username, password, handler);
```

The `username` and `password` are the same credentials used to log into the system.  Note the username and password are not stored either in-memory or persistently after this authentication call.  After successfully authenticating with the system, the *Vantiq* instance stores in-memory an access token that subsequent API calls will use.

The `handler` is an instance of `io.vantiq.ResponseHandler` that needs to be implemented by the caller and defines methods, such as `onSuccess` and `onError` that are called during the method processing.  `BaseResponseHandler` provides an implementation of `ResponseHandler` that should be overridden to provide desired behavior.

Now, you are able to perform any SDK calls to the Vantiq server.  For example, the following prints out the list of types that have been defined:

```java
vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              null, 
              null, 
              null, 
              new BaseResponseHandler() {
                    @Override
                    public void onSuccess(Object body, Response response) {
                        super.onSuccess(body, response);
                        for(JsonObject obj : (List<JsonObject>) body) {
                            System.out.println(obj);
                        }
                    }
              }
);
```

## Documentation

For the full documentation on the SDK, see the [SDK API Reference](./docs/api.md).

## Copyright and License

Copyright &copy; 2016 Vantiq, Inc.  Code released under the [MIT license](./LICENSE).
