# API Reference

The Vantiq API provides an API for interacting with the Vantiq server.

This document defines the Vantiq Client SDK.  Please refer to the [Vantiq Reference Guides](https://dev.vantiq.com/docs/system/index.html) for details on the how to use the Vantiq system.

## Vantiq API

The Vantiq object provides the main API for the Vantiq Java SDK.  This SDK follows the Vantiq REST API resources model.

### Resources

Each of the SDK API methods corresponds to a specific REST API operation on a specific resource.  For example,
`select` performs a query against a given resource.  `select("types", ...)` queries against defined data types.
`select("procedures", ...)` queries against defined procedures.

The available system resources are defined in the `Vantiq.SystemResources` enum and include the following:

Enum Name  | Resource Name  | Type Name    | Description
---------- | -------------- | ------------ | -----------
ANALYTICS_MODELS | analyticsmodels | ArsAnalyticsModel | Analytics Models defined in the Vantiq system
CONFIGURATIONS | configurations | ArsRuleConfiguration | Configurations of Vantiq artifacts
DOCUMENTS  | documents      | ArsDocument  | Unstructured documents stored in the Vantiq system
NAMESPACES | namespaces     | ArsNamespace | Namespaces defined in the Vantiq system
NODES      | nodes          | ArsPeerNode  | Node defined in the Vantiq system to support federation
PROFILES   | profiles       | ArsProfile   | Vantiq user permission profiles
PROCEDURES | procedures     | ArsComponent | Procedures defined in the Vantiq system
RULES      | rules          | ArsRuleSet   | Rules defined in the Vantiq system
SCALARS    | scalars        | ArsScalar    | User-defined property type definitions
SOURCES    | sources        | ArsSource    | Data sources defined in the Vantiq system
TOPICS     | topics         | ArsTopic     | User-defined topics in the Vantiq system
TYPES      | types          | ArsType      | Data types defined in the Vantiq system
USERS      | users          | ArsUser      | Vantiq user accounts
IMAGES     | images         | ArsImages    | Images stored in the Vantiq system
VIDEOS     | videos         | ArsVideos    | Videos stored in the Vantiq system

Data types defined in the Vantiq system can also be used as resources.  For example, if you define data
type `MyNewType`, then `MyNewType` is now a legal resource name that can be used in the API methods.

### API

* [io.vantiq.client.Vantiq](#user-content-vantiq)

#### Methods

* [authenticate](#user-content-vantiq-authenticate)
* [select](#user-content-vantiq-select)
* [selectOne](#user-content-vantiq-selectOne)
* [count](#user-content-vantiq-count)
* [insert](#user-content-vantiq-insert)
* [update](#user-content-vantiq-update)
* [upsert](#user-content-vantiq-upsert)
* [delete](#user-content-vantiq-delete)
* [deleteOne](#user-content-vantiq-deleteOne)
* [publish](#user-content-vantiq-publish)
* [execute](#user-content-vantiq-execute)
* [evaluate](#user-content-vantiq-evaluate)
* [query](#user-content-vantiq-query)
* [subscribe](#user-content-vantiq-subscribe)
* [unsubscribeAll](#user-content-vantiq-unsubscribeAll)
* [upload](#user-content-vantiq-upload)
* [download](#user-content-vantiq-download)
* [acknowledge](#user-content-vantiq-acknowledge)

All Vantiq SDK methods have both an asynchronous form and a
synchronous form.  The asynchronous form requires a response
handler that is executed when the response from the Vantiq
server arrives, 

```java
vantiq.method(arg1, arg2, ..., responseHandler);
```

The synchronous form blocks until the response from the 
Vantiq server arrives and returns the response as the return
value,

```java
VantiqResponse response = vantiq.method(arg1, arg2, ...);
```

#### Properties

* [accessToken](#user-content-vantiq-accessToken)
* [server](#user-content-vantiq-server)
* [username](#user-content-vantiq-username)
* [enablePings](#user-content-vantiq-enablePings)
* [readTimeout](#user-content-vantiq-readTimeout)
* [writeTimeout](#user-content-vantiq-writeTimeout)
* [connectTimeout](#user-content-vantiq-connectTimeout)

### Responses

* [VantiqResponse](#user-content-VantiqResponse)
* [ResponseHandler](#user-content-ResponseHandler)

### Error

* [VantiqError](#VantiqError)

## <a id="vantiq"></a> Vantiq

The `Vantiq` constructor creates a new instance of the `Vantiq` SDK object.
The SDK expects that the first operation is to authenticate onto the
specified Vantiq server.  After successfully authenticated, the client
is free to issue any requests to the Vantiq server.

This class exposes the [Vantiq REST API](https://dev.vantiq.com/docs/system/api/index.html)

### Signature

```java
Vantiq vantiq = new Vantiq(String server[, int apiVersion])
```

### Option Parameters

**`String server`**:  (*Required*) Specifies the VANTIQ server URL to connect to, e.g. `https://dev.vantiq.com` 

**`int apiVersion`** Integer to specify the version of the API to use.  Defaults to the latest. 

### Returns

An instance of the `Vantiq` object

### Example

```java
Vantiq vantiq = new Vantiq("https://dev.vantiq.com");
```

## <a id="vantiq-authenticate"></a> Vantiq.authenticate

The `authenticate` method connects to the Vantiq server with the given 
authentication credentials used to authorize the user.  Upon success,
an access token is provided to the client for use in subsequent API 
calls to the Vantiq server.  The username and password credentials
are not stored.

### Signature

```java
void vantiq.authenticate(String username, String password, ResponseHandler handler)

VantiqResponse vantiq.authenticate(String username, String password)
```

### Parameters

**`String username`**: (*Required*) The username to provide access to the Vantiq server 

**`String password`**:  (*Required*) The password to provide access to the Vantiq server 

****`ResponseHandler handler`****:  (*Required*) A ResponseHandler that is called upon success or failure 

### Returns

The `authenticate` method returns a Boolean body indicating if the authentication was successful.

### Example

```java
vantiq.authenticate("joe@user", "my-secr3t-passw0rd!#!", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Authenticated!");
    }
    
    @Override public void onError(List<VantiqError> errors, Response response) {
        super.onError(errors, response);
        System.out.println("Errors: " + errors);
    }

});
```

## <a id="vantiq-select"></a> Vantiq.select

The `select` method issues a query to select all matching records for a given 
resource.

### Signature

```java
void vantiq.select(String resource, 
                   List<String> props, 
                   Object where, 
                   SortSpec sort, 
                   ResponseHandler handler)
                   
VantiqResponse vantiq.select(String resource, 
                             List<String> props, 
                             Object where, 
                             SortSpec sort)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.

**`List<String> props`**: (*Required*) Specifies the desired properties to be returned in each record.  An empty list or null value means all properties will be returned.

**`Object where`**: Specifies constraints to filter the data.  Null means all records will be returned.

**`SortSpec sort`**: Specifies the desired sort for the result set

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

### Returns

The `select` method returns a body of type `List<JsonObject>` providing the list of matching records.

### Example

Select the `name` property for all available types.

```java
vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              Collections.singletonList("name"), 
              null, 
              null, 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj.get("name").getAsString());
        }
    }
    
});
```

Selects all properties filtering to only return types with the `TestType` name.

```java
JsonObject where = new JsonObject();
where.addProperty("name", "TestType");

vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              null, 
              where, 
              null, 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj);
        }
    }
    
});
```

Selects all records for the `TestType` type returning only the `key` and `value` properties and sorting the results by the `key` in descending order.

```java
vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              Arrays.asList("key", "value"), 
              null, 
              new SortSpec("key", true), 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj);
        }
    }
    
});
```

## <a id="vantiq-selectOne"></a> Vantiq.selectOne

The `selectOne` method issues a query to return the single record identified 
by the given identifier.

### Signature

```java
void vantiq.selectOne(String resouce, String id, ResponseHandler handler)

VantiqResponse vantiq.selectOne(String resouce, String id)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.

**`String id`**: (*Required*) The id for the given record 

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

### Returns

The `selectOne` method returns a body of type `JsonObject` providing the matching record.

### Example

Select the `TestType` definition from the `types` resource.

```java
vantiq.selectOne(Vantiq.SystemResources.TYPES.value(), "TestType", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }
    
});
```

Selects a `TestType` record with `_id` equal to `23425231ad31f`.

```java
vantiq.selectOne("TestType", "23425231ad31f", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }
    
});
```

## <a id="vantiq-count"></a> Vantiq.count

The `count` method is similar to the `select` method except it returns only the
number of records rather than returning the records themselves.

### Signature

```java
void vantiq.count(String resource, Object where, ResponseHandler handler)

VantiqResponse vantiq.count(String resource, Object where)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.
 
**`Object where`**:  Specifies constraints to filter the data.  Null means all records will be returned.
 
**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

### Returns

The **`count`** method returns a body of type `Integer` providing the count of the matching records.

### Example

Counts the number of `TestType` records.

```java
vantiq.count("TestType", null, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("TestType has " + (Integer) body + " records");
    }
    
});
```

Counts the number of `TestType` with a `value` greater than 10.

```java
JsonObject where = new JsonObject();
  JsonObject gt = new JsonObject();
  gt.addProperty("$gt", 10);
where.add("value", gt);

vantiq.count("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("TestType has " + (Integer) body + " records with value > 10");
    }
    
});
```

## <a id="vantiq-insert"></a> Vantiq.insert

The `insert` method creates a new record of a given resource.

### Signature

```java
void vantiq.insert(String resource, Object object, ResponseHandler handler)

VantiqResponse vantiq.insert(String resource, Object object)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.
 
 
**`Object object`**: (*Required*) The object to insert.

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

### Returns

The **`insert`** method returns a `JsonObject` object containing the object just inserted.  

### Example

Inserts an object of type `TestType`.

```
JsonObject objToInsert = new JsonObject();
objToInsert.addProperty("key", "SomeKey");
objToInsert.addProperty("value", 42);
    
vantiq.insert("TestType", objToInsert, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-update"></a> Vantiq.update

The `update` method updates an existing record of a given resource.  This method
supports partial updates meaning that only the properties provided are updated.
Any properties not specified are not changed in the underlying record.

### Signature

```java
void vantiq.update(String resource, String id, Object object, ResponseHandler handler)

VantiqResponse vantiq.update(String resource, String id, Object object)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.
 
**`String id`**: (*Required*)  The unique identifier for the record ("_id" for user defined types)
 
**`Object props`**:  The properties to update in the record.  

**`ResponseHandler handler`**:  (*Required*) Listener that is called upon success or failure

### Returns

The `update` method returns a `JsonObject` object containing the object just updated.  

### Example

Updates a given `TestType` record

```java
String _id = "56f4c52120eb8b5dee4898fd";

JsonObject propsToUpdate = new JsonObject();
propsToUpdate.addProperty("value", 13);
    
vantiq.update("TestType", _id, propsToUpdate, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-upsert"></a> Vantiq.upsert

The `upsert` method either creates or updates a record in the database depending
if the record already exists.  The method tests for existence by looking at the
natural keys defined on the resource.

### Signature

```java
void vantiq.upsert(String resource, Object object, ResponseHandler response)

VantiqResponse vantiq.upsert(String resource, Object object)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`. 
 
**`Object object`**: (*Required*) The object to upsert. 

**`ResponseHandler handler`**:  (*Required*) Listener that is called upon success or failure

### Returns

The `upsert` method return a `JsonObject` object containing the object just inserted or created.

### Example

Inserts an object of type `TestType`.

```java
JsonObject objToUpsert = new JsonObject();
objToUpsert.addProperty("key", "SomeKey");
objToUpsert.addProperty("value", 42);
    
vantiq.upsert("TestType", objToUpsert, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-delete"></a> Vantiq.delete

The `delete` method removes records from the system for a given resource.  Deletes always
require a constraint indicating which records to remove.

### Signature

```java
void vantiq.delete(String resource, Object where, ResponseHandler handler)

VantiqResponse vantiq.delete(String resource, Object where)
```

### Parameters


**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.
  
**`Object where`**: (*Required*) Specifies which records to remove  

**`ResponseHandler handler`**:  (*Required*) Listener that is called upon success or failure

The `where` is an object with supported operations defined in [API operations](https://dev.vantiq.com/docs/system/api/index.html#where-parameter) that will be converted to JSON using [Gson](https://github.com/google/gson).

### Returns

The `delete` method returns a boolean indicating the removal succeeded.

### Example

Removes the record with the given key.

```java
JsonObject where = new JsonObject();
where.addProperty("key", "SomeKey");

vantiq.delete("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

Removes all records of `TestType` with a `value` greater than 10.

```java
JsonObject where = new JsonObject();
  JsonObject gt = new JsonObject();
  gt.addProperty("$gt", 10);
where.add("value", gt);

vantiq.delete("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Deleted all records with value > 10.");
    }

});
```

## <a id="vantiq-selectOne"></a> Vantiq.deleteOne

The `deleteOne` method removes a single record specified by the given identifier.

### Signature

```java
void vantiq.deleteOne(String resource, String id, ResponseHandler response)

VantiqResponse vantiq.deleteOne(String resource, String id)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()`, `SystemResources.SOURCES.value()` or `SystemResources.TYPES.value()`.
 
**`String id`**: (*Required*)  The unique identifier for the record ("_id" for user defined types)
 
**`ResponseHandler handler`**:  (*Required*) Listener that is called upon success or failure

### Returns

The `deleteOne` method returns a boolean indicating the removal succeeded.

### Example

Removes the `TestType` definition from the `types` resource.

```java
vantiq.deleteOne(Vantiq.SystemResources.TYPES.value(), "TestType", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

Removes the `TestType` record with `_id` equal to `23425231ad31f`.

```java
vantiq.deleteOne("TestType", "23425231ad31f", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

## <a id="vantiq-publish"></a> Vantiq.publish

The `publish` method supports publishing to a topic or a source.

Publishing a message to a given topic allows for rules to be defined
that trigger based on the publish event.  Topics are slash-delimited strings, 
such as '/test/topic'.  Vantiq reserves  `/type`, `/property`, `/system`, and 
`/source` as system topic namespaces and should not be published to.  The 
`payload` is the message to be sent.

Calling `publish` on a source performs a publish (asynchronous call) on the
specified source.  The `payload` is the parameters required to issue the
publish operation on the source.

### Signature

```java
void vantiq.publish(String resource, String id, Object payload, ResponseHandler response)

VantiqResponse vantiq.publish(String resource, String id, Object payload)
```

### Parameters

**`String resource`**:  (*Required*) The name of the resource type to query.
 Must be either `SystemResources.TOPICS.value()` or `SystemResources.SOURCES.value()`.
 
**`String id`**: (*Required*)  The unique name for the resource (eg:/test/topic or 'mqttSrc')
 
**`Object payload`**:  For topics, the payload is the message to send.  For sources, this is the parameters for the source.

**`ResponseHandler handler`**:  (*Required*) Listener that is called upon success or failure

For sources, the parameters required are source specific and are the same as those required
when performing a `PUBLISH ... TO SOURCE ... USING params`.  Please refer to the specific source definition
documentation in the [Vantiq API Documentation](https://dev.vantiq.com/docs/system/api/index.html).

### Returns

Since the `publish` operation is a fire-and-forget action, a successful publish will result in a boolean value indicating the publish occurred.

### Example

Send a message onto the `/test/topic` topic.

```java
JsonObject message = new JsonObject();
message.addProperty("key", "AnotherKey");
message.addProperty("value", 13);

vantiq.publish(Vantiq.SystemResources.TOPICS.value(), 
               "/test/topic", 
               message,
               new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Published message successfully");
    }

});
```

Send a message to a SMS source `mySMSSource`.

```java
JsonObject params = new JsonObject();
params.addProperty("body", "Nice message to the user");
params.addProperty("to", "+16505551212");

vantiq.publish(Vantiq.SystemResources.SOURCES.value(), 
               "mySMSSource", 
               params,
               new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Published to source successfully");
    }

});
```

## <a id="vantiq-execute"></a> Vantiq.execute

The `execute` method executes a procedure on the Vantiq server.  Procedures can
take parameters (i.e. arguments) and produce a result.

### Signature

```java
void vantiq.execute(String procedure, Object params, ResponseHandler response)

VantiqResponse vantiq.execute(String procedure, Object params)
```

### Parameters

**`String procedure`**: (*Required*) The name of the procedure to execute

**`Object params`**: (*Required*) An object that holds the parameters where each key is the parameter name

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

The parameters may be provided as an array where the arguments are given in order.
Alternatively, the parameters may be provided as an object where the arguments
are named.

### Returns

The `execute` method returns with a `JsonObject` object containing the result of the procedure call.

### Example

Executes an `sum` procedure that takes `arg1` and `arg2` arguments and returns the total using positional arguments.

```java
JsonArray params = new JsonArray();
params.add(new JsonPrimitive(1));
params.add(new JsonPrimitive(2));

vantiq.execute("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }

});
```

Using named arguments.

```java
JsonObject params = new JsonObject();
params.addProperty("arg1", 1);
params.addProperty("arg1", 2);

vantiq.execute("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }

});
```

## <a id="vantiq-evaluate"></a> Vantiq.evaluate

The `evaluate` method evaluates an analytics model on the Vantiq server.  Analytics models
expect input data as parameters and produce a result.

### Signature

```java
void vantiq.evaluate(String modelName, Object params, ResponseHandler response)

VantiqResponse vantiq.evaluate(String modelName, Object params)
```

### Parameters

**`String modelName`**: (*Required*) The name of the analytics model to execute

**`Object params`**:  An object that holds the parameters.

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

The input data in the `params` field should be structured in the form defined by the 
analytics model.  In general, it should be a JSON object with a structured defined as the
input record type in the model.

### Returns

The `evaluate` method return with a `JsonObject` or `JsonPrimitive` object containing the result of the model.

### Example

Evaluates an `Score` analytics model that takes `arg1` and `arg2` arguments and returns the score in a
`JsonObject`.

```java
JsonObject params = new JsonObject();
params.addProperty("arg1", "xxx");
params.addProperty("arg1", "yyy");

vantiq.evaluate("Score", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The model score is = " + this.getBodyAsJsonObject().get("score"));
    }

});
```

## <a id="vantiq-query"></a> Vantiq.query

The `query` method performs a query (synchronous call) on the specified source.  The query can
take parameters (i.e. arguments) and produce a result.

### Signature

```java
void vantiq.query(String source, Object params, ResponseHandler handler)

VantiqResponse vantiq.query(String source, Object params)
```

### Parameters

**`String source`**: (*Required*) The name of the source to perform the query

**`Object params`**: (*Required*)  An object that holds the parameters.

**`ResponseHandler handler`**: (*Required*) Listener that is called upon success or failure

The parameters required are source specific and are the same as those required
when performing a `SELECT ... FROM SOURCE ... WITH params`.  Please refer to the specific source definition
documentation in the [Vantiq API Documentation](https://dev.vantiq.com/docs/system/api/index.html).

### Returns

The `query` method returns with a `JsonObject` object containing the returned value from the source.

### Example

Query a REST source `adder` that returns the total of the given parameters.

```java
JsonObject params = new JsonObject();
params.addProperty("path", "/api/adder");
params.addProperty("method", "POST");
params.addProperty("contentType", "application/json");

  JsonObject body = new JsonObject();
  body.addProperty("arg1", 1);
  body.addProperty("arg2", 2);
  
params.add("body", body);

vantiq.query("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }
});
```


## <a id="vantiq-subscribe"></a> Vantiq.subscribe

The `subscribe` method creates a WebSocket to the Vantiq server and listens for specified events.  The provided
callback is executed whenever a matching event occurs.

### Signature

```java
void vantiq.subscribe(String resource, String name, TypeOperation operation, SubscriptionCallback callback, Map parameters)
```

### Parameters

**`String resource`**: (*Required*) Describes the type of event being subscribed to. 
Must be either SystemResources.TOPICS.value(), SystemResources.SOURCES.value() or SystemResources.TYPES.value().

**`String name`**: (*Required*) A required String that identifies the specific resource event. For topics, this is the topic name (e.g. '/my/topic/'). 
 For sources, this is the source name.  For types, this is the data type name (e.g. TypeOperation.INSERT, TypeOperation.UPDATE, TypeOperation.DELETE)

**`TypeOperation operation`**: (*Required for Type events* )Only required for type events. Specifies the operation to listen to 
(e.g. TypeOperation.INSERT, TypeOperation.UPDATE, TypeOperation.DELETE)

**`SubscriptionCallback callback`**: (*Required*) SubscriptionCallback that is executed when the specified event occurs.
 
**`Map parameters`**: Not required map specifying extra details about the subscription to the server.
  (eg: {persistent:true} to create a persistent subscription,
   {persistent:true: subscriptionName: 'mySub', requestId: requestId} to reconnect to a broken persistent subscription)
  

   
<br/>   

The **`SubscriptionCallback`** interface contains 4 methods:

**`onConnect()`**: Method called once the connection has been established and the subscription has been acknowledged by the Vantiq server.

**`SubscriptionMessage onMessage(m)`**: Method called for every matching event that occurs

**`String onError(e)`**:   Method called whenever an error occurs that does not arise from an exception, such as if a non-success response is provided.  The argument provides the error message.

**`Throwable onFailure(e)`**: Method Called whenever an exception occurs during the subscription processing.

 <br/>

The **`SubscriptionMessage`**: contains the fields:

**`int status`**: The status for the given message.  The possible values are the HTTP status codes. Typically, this would be 100. 

**`String contentType`**:  The content MIME type for the message body.  Typically, this is `application/json`.

**`Map<String,String> headers`**: A map of headers associated with the response.

**`Object body`**: The Object payload for the event.  For JSON encoded responses, this is typically a Map with keys *path* (full event path) and *value* (payload of the event).


### Returns

In the case of a persistent subscription returns a SubscriptionMessage with the body containing a requestId and subscriptionName
used to acknowledge events and reconnect to broken subscriptions. 

### Example

In this example, we implement a callback that simply prints out the events to standard out:

    public class StandardOutputCallback implements SubscriptionCallback {
    
        @Override
        public void onConnect() {
            System.out.println("Connected Successfully");
        }
    
        @Override
        public void onMessage(SubscriptionMessage message) {
            System.out.println("Received Message: " + message);
        }
    
        @Override
        public void onError(String error) {
            System.out.println("Error: " + error);
        }
    
        @Override
        public void onFailure(Throwable t) {
            t.printStackTrace();
        }
        
    }

Create a subscription to the `/test/topic` topic that prints out when events are published to the topic.

    vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), 
                     "/test/topic", 
                     null, 
                     new StandardOutputCallback());

Create a subscription to the `MySource` source that prints out when messages arrive at the source.

    vantiq.subscribe(Vantiq.SystemResources.SOURCES.value(), 
                     "MySource", 
                     null, 
                     new StandardOutputCallback());

Create a subscription to the `MyDataType` type for that prints out when that type has been updated.

    vantiq.subscribe(Vantiq.SystemResources.TYPES.value(), 
                     "MyDataType", 
                     Vantiq.TypeOperation.UPDATE, 
                     new StandardOutputCallback());

## <a id="vantiq-unsubscribeAll"></a> Vantiq.unsubscribeAll

The `unsubscribeAll` method removes all active subscriptions to the Vantiq server by
closing the WebSocket.

### Signature

```java
void vantiq.unsubscribeAll()
```

### Parameters

N/A

### Returns

N/A

### Example

    vantiq.unsubscribeAll();

## <a id="vantiq-upload"></a> Vantiq.upload

The `upload` method performs an upload of a file into a Vantiq resource. The default upload method will upload the file to 
the ArsDocument resource, but the user may optionally specify which Vantiq resource they wish to upload to. The upload may be 
asynchronous or synchronous.

### Signature

```java
void vantiq.upload(File file, String contentType, String documentPath, ResponseHandler responseHandler)

VantiqResopnse vantiq.upload(File file, String contentType, String documentPath)
```

**OR**

```java
void vantiq.upload(File file, String contentType, String documentPath, String resourcePath, ResponseHandler responseHandler)

VantiqResopnse vantiq.upload(File file, String contentType, String documentPath, String resourcePath)
```

### Parameters


**`File file`**: (*Required*) The file to be uploaded

**`String contentType`**: (*Required*) The MIME type of the uploaded file (e.g. "image/jpeg")

**`String documentPath`**: (*Required*) The "path" of the ArsDocument in Vantiq (e.g. "assets/myDocument.txt")

**`String  resourcePath`**: The "path" of the Vantiq resource to which you wish to upload the file (e.g. `"/resources/documents"`, `"/resources/images"`, or `"/resources/videos"`) 

### Returns

The `upload` method returns a `JsonObject` object containing the information about the stored document.  In particular, the response will contain:


**`String name`**:  The document path (e.g. "assets/myDocument.txt")

**`StringfileType`**: The MIME type of the uploaded file (e.g. "image/jpeg")

**`String content`**: This provides the URI path to download the content.  This is used in the [download](#user-content-vantiq-download) method.

### Example

The following uploads a text file asynchronously and prints out the location of the content:

```java
File myFile = ...
    
vantiq.upload(myFile, 
              "text/plain", 
              "myAssets/myFile.txt",
              new BaseResponseHandler() {
              
    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        
        System.out.println("Content Location = " + this.getBodyAsJsonObject().get("content"));
    }
    
});
```

The following uploads an image file asynchronously, specifying that the image be stored in Vantiq as an image (instead of a 
document), and prints out the location of the content:

```java
File myFile = ...
    
vantiq.upload(myFile, 
              "image/jpeg", 
              "myAssets/myImage.jpg",
              "/resources/images",
              new BaseResponseHandler() {
              
    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        
        System.out.println("Content Location = " + this.getBodyAsJsonObject().get("content"));
    }
    
});
```

## <a id="vantiq-download"></a> Vantiq.download

The `download` method pulls down the content of a file that was
previously uploaded.  The download method may be synchronous or asynchronous, but the data is always streamed to the client.

### Signature

```java
void vantiq.download(String path, ResponseHandler responseHandler)

VantiqResopnse vantiq.download(String path)
```

### Parameters

**`String path`**: (*Required*) This is the path to the file.  This can be gotten from the `content` field in the ArsDocument or the returned value from the `upload` method.

### Returns

The **`download`** response body is a `Okio.BufferedSource` that can be used
to stream the data to the client.

### Example

The following calls the download asynchronously and prints out the file
contents:

```java
String path = ...from ArsDocument...
    
vantiq.download(path, new BaseResponseHandler() {
              
    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);

        byte[] data = ((BufferedSource) body).readByteArray();
        System.out.println("File: " + new String(data, "UTF-8"));
    }
    
});
```

The following calls the download synchronoulsy and writes the file
to disk:

```java
String path = ...from ArsDocument...
OutputStream os = ...stream to write...

VantiqResponse resopnse = vantiq.download(path);
BufferedSource source = (BufferedSource) response.getBody();

byte[] buf = new byte[1024];
while((int len = source.read(buf)) != -1) {
    os.write(buf, 0, len);
}

os.close();
```

## <a id="vantiq-acknowledge"></a> Vantiq.acknowledge

The `acknowledge` method is used to acknowledge the receipt of messages from reliable resources after creating a persistent subscription.
 The provided callback is executed whenever a matching event occurs.

### Signature

```java
void vantiq.ack(String subscriptionName, String requestId, Map msg)
```

### Parameters

**`String subscriptionName`**: (*Required*) The name of the subscription that uniquely identifies the persistent subscription.
This was returned by the server on creation of the persistent subscription. 

**`String requestId`**: (*Required*) The id of the requestId that that uniquely identifies the websocket requests made by this subscription.
This was returned by the server on creation of the persistent subscription. 

**`Map msg`**: (*Required*) The message in the event being acknowledged. This is the body of the SubscriptionMessage
  

### Returns

N/A

### Example

Create a callback that acknowledges events as they arrive.
```
public class AcknowledgingOutputCallback implements SubscriptionCallback {
   private String subscriptionName;
   private String requestId;

    @Override
    public void onConnect() {
        System.out.println("Connected Successfully");
    }

    @Override
    public void onMessage(SubscriptionMessage message) {
        //Server response with subscription information (not a topic event)
        if (message.body.subscriptionName) {
            this.subscriptionName = message.body.subscriptionName;
            this.requestId = message.body.requestId;
            System.out.println("SubscriptionName " + this.subscriptionName);
            System.out.println("RequestId " + this.requestId);
        } else {
            //message.body is an event on the subscribed topic. Acknowledge that we received the event
            vantiq.ack(this.subscriptionName, this.requestId, message.body);
        }
    }

    @Override
    public void onError(String error) {
        System.out.println("Error: " + error);
    }

    @Override
    public void onFailure(Throwable t) {
        t.printStackTrace();
    }
}

```
Create a persistent subscription  to the `/test/topic` reliable topic that acknowledges when events are published to the topic.

    vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), 
                     "/test/topic", 
                     null, 
                     new AcknowledgingOutputCallback(),
                     {persistent: true});
                     
  

Create a subscription to the `MySource` reliable source that prints out when messages arrive at the source.

    vantiq.subscribe(Vantiq.SystemResources.SOURCES.value(), 
                     "MySource", 
                     null, 
                     new AcknowledgingOutputCallback(),
                      {persistent: true});

To reconnect to a severed persistent subscription.
    vantiq.subscribe(Vantiq.SystemResources.TOPICS.value(), 
                     "/test/topic", 
                     null, 
                     new AcknowledgingOutputCallback(),
                     {persistent: true, subscriptionName: subscriptionName, requestId: requestsId});




## <a id="vantiq-accessToken"></a> Vantiq.accessToken [String]

The `accessToken` property provides access to the 
access token and a means for explicitly
setting an access token without the `authenticate` method.
The token may be a token previously retrieved through the
`authenticate` method or a long-lived token issued by the
Vantiq server.

After setting the access token, the SDK assumes that the
session has been authenticated and operations can be issued.

## <a id="vantiq-server"></a> Vantiq.server [String]

The server URL used to connect to the Vantiq system.

After setting the server, the SDK uses the URL for all future requests.

## <a id="vantiq-username"></a> Vantiq.username [String]

The username associated with the authenticated user in Vantiq.
The `authenticate` method sets this upon success.

## <a id="vantiq-enablePings"></a> Vantiq.enablePings [Boolean]

The `enablePings` property determines if periodic pings should
be sent on the WebSocket subscriptions.  This may be required
as some proxy servers will close connections if they are idle.

The default value is _true_.

## <a id="vantiq-readTimeout"></a> Vantiq.readTimeout [Long]

The `readTimeout` property determines the timeout for waiting
for data.  A value of zero means there is no timeout.

The default value is _0_.

## <a id="vantiq-writeTimeout"></a> Vantiq.writeTimeout [Long]

The `writeTimeout` property determines the timeout while writing
data.  A value of zero means there is no timeout.

The default value is _0_.

## <a id="vantiq-connectTimeout"></a> Vantiq.connectTimeout [Long]

The `connectTimeout` property determines the timeout to establish
a connection.  A value of zero means there is no timeout.

The default value is _0_.




# <a id="VantiqResponse"></a> VantiqResponse

The `VantiqResponse` object holds the completion state of the
Vantiq SDK method if the synchronous form is used.

### Methods

Name | Description
---- | -----------
getBody| Returns the parsed content of the response.  This is usually a JsonElement.
getContentType| Returns the content type header value 
getCount| If a count was requested, this returns the count value
getErrors| Errors returned from the Vantiq server, if any
getException| The exception thrown during processing, if any
getResponse| The underlying `OkHttp3` response object
getStatusCode| The HTTP response status code
hasErrors| Indicates if there were errors from the Vantiq server
hasException|Indicates if there was an exception during processing
isSuccess| Indicates if the operation was successful

# <a id="ResponseHandler"></a> ResponseHandler

The `ResponseHandler` interface provides the API that are called when the Vantiq SDK methods complete either
successfully or in error.

The SDK provides the concrete class `BaseResponseHandler` that implements the `ResponseHandler` interface and provides
helper methods.

### Methods

Name | Description
---- | -----------
onSuccess | Called when the method call returns successfully (i.e. HTTP status codes 2xx).  The response body is parsed and provided in the `body` argument.
onError   | Called when the server returns one or more errors (i.e. HTTP status codes 4xx/5xx).
onFailure | Called when the client has an exception during processing.

### Android Usage

On Android, the `BaseResponseHandler` can be used within an `AsyncTask` to support Vantiq server requests while still
allowing the UI to be responsive.  Below is an example of how to create a base async task class (from the 
Android example).

```java
package io.vantiq.examplesdkclient;

import android.os.AsyncTask;

import io.vantiq.client.BaseResponseHandler;
import io.vantiq.client.Vantiq;

/**
 * Abstract AsyncTask class that provides an easy mechanism to issue requests
 * to the Vantiq server.  Like other async tasks, this should only be used once.
 */
public abstract class VantiqAsyncTask<Params,Progress,Result> extends AsyncTask<Params,Progress,Result> {

    private Vantiq vantiq;
    private BaseResponseHandler handler;
    private boolean done = false;

    public VantiqAsyncTask(Vantiq vantiq) {
        this.vantiq = vantiq;

        // Provide the response handler that simply indicates when the response has finished
        this.handler = new BaseResponseHandler() {
            @Override public void completionHook(boolean success) {
                VantiqAsyncTask.this.done = true;
            }
        };
    }

    /**
     * Abstract method that must be overridden by subclasses to issue a Vantiq request.
     * @param handler The response handler
     */
    abstract protected void doRequest(Vantiq vantiq, BaseResponseHandler handler);

    /**
     * Abstract method that must be overridden by subclasses to return the result.
     * @param handler The response handler
     * @return The response of this async task
     */
    abstract protected Result prepareResult(BaseResponseHandler handler);

    @Override
    protected final Result doInBackground(Params... params) {

        // Issue request to the server
        doRequest(this.vantiq, this.handler);

        // Wait for the request to be completed
        try {
            while (!this.done) {
                Thread.sleep(100);
            }
        } catch(InterruptedException ex) {
            /* If interrupted, then just drop out */
        }

        return prepareResult(this.handler);
    }

}
```

This async task makes it easy to use the Vantiq SDK API.  Here is an example of running 
a select operation.

```java
// Trigger select query
this.selectTask = new SelectTypesTask(vantiq);
this.selectTask.execute((Void) null);
```

Using the following `SelectTypesTask`.

```java
/**
 * Async task to perform a select to return all available types from the Vantiq server.
 */
public class SelectTypesTask extends VantiqAsyncTask<Void, Void, List<Map<String,String>>> {

    SelectTypesTask(Vantiq vantiq) {
        super(vantiq);
    }

    @Override
    protected void doRequest(Vantiq vantiq, BaseResponseHandler handler) {
        vantiq.select(Vantiq.SystemResources.TYPES.value(),
                      Arrays.asList("name", "namespace"),
                      null,
                      new SortSpec("name", /*descending=*/ false),
                      handler);
    }

    @Override
    protected List<Map<String,String>> prepareResult(BaseResponseHandler handler) {
        List<Map<String,String>> result = Lists.newArrayList();

        // At this point, we should have a result, so we pull out the response
        if(handler.getBody() != null) {
            for(JsonObject obj : handler.getBodyAsList()) {
                Map<String,String> entry = Maps.newHashMap();
                entry.put("name", obj.get("name").getAsString());
                entry.put("namespace", obj.get("namespace").getAsString());
                result.add(entry);
            }
        } else if(handler.hasErrors()) {
            MainActivity.this.onServerError(handler.getErrors().toString());
        } else if(handler.hasException()) {
            MainActivity.this.onServerError(handler.getException().getMessage());
        }

        return result;
    }

    @Override
    protected void onPostExecute(final List<Map<String,String>> result) {
        MainActivity.this.selectTask = null;
        onServerCompletion(result);
    }
}
```

# <a id="VantiqError"></a> VantiqError

The REST API provides an error in the following form:

    {
        code: <ErrorIdentifier>,
        message: <ErrorMessage>,
        params: [ <ErrorParameter>, ... ]
    }
    
The SDK returns errors using the `VantiqError` object.

### Parameters


code | String | The Vantiq error id (e.g. _"io.vantiq.authentication.failed"_)
message | String | The human readable message associated with the error
params | List\<Object\> | A list of arguments associated with the error
