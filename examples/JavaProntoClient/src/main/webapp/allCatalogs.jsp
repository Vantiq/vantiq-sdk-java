<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
	<link rel="stylesheet" type="text/css" href="style.css">
    <title>VANTIQ Catalogs</title>
    <body>
    	<h1>You're now connected to the VANTIQ Server</h1>
        <h2>Your VANTIQ Catalogs</h2>
        <div>
        	<table>
	        	<tr>
	       			<th>Manager Namespace</th>
	       			<th>Fetch Events</th>
	        	</tr>
	        	<c:forEach items="${managerData}" var="data">
	        		<tr>
		        		<td>${data}</td>
		        		<td>
			        		<form action="${pageContext.request.contextPath}/Catalog" method="post">
			        			<input type="submit" name="viewCatalog" value="View Catalog">
			        			<input type="hidden" name="catalogName" value="${data}">
			        		</form>
		        		</td>
		        	</tr>
	        	</c:forEach>
	        </table>
        </div>
    </body>
</html>