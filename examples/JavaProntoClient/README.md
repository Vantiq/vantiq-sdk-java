# JavaProntoClient
A client that uses the VANTIQ Java SDK to interact with the VANTIQ Event Broker.

## Dependencies <a name="dependencies" id="dependencies"></a>
All dependencies are installed using gradle. The project is designed to run on a Tomcat 9 server.

To properly build the project:
* Clone this repository.
* Navigate inside the directory where the repository was cloned. 
* Run ``./gradlew build``.

## Running the project
First, follow the steps explained under [Dependencies](#dependencies). Once those steps are complete, simply run the following 
command: ``../.././gradlew tomcatRun`` to start the project.

**IMPORTANT NOTES:**
* You will know the server is running when the following line appears in your command line: "The Server is running at
http://localhost:8000/JavaProntoClient".
* The gradle task will stay at 75% EXECUTING even when the server is up and running.
* To quit the program, simply use Ctrl C. The server will take a few seconds to shut down, so be sure to wait before 
restarting it.
