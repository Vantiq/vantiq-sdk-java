<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
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

<style type="text/css">
	h1,h2{
        text-align: center;
        font-family: arial, sans-serif;
    }
    div{
        padding: 10px 25px 25px 25px;
    }
    table{
    	margin: 0 auto;
        font-family: arial, sans-serif;
        border-collapse: collapse;
        width: 40%;
    }
    td, th{
        border-bottom: 1px solid #000000;
        text-align: center;
        padding: 15px;
    }
    tr:nth-child(even){
        background-color: #dddddd;
    }
</style>