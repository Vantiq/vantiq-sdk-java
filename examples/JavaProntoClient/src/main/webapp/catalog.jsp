<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
    <link rel="stylesheet" type="text/css" href="style.css">
    <title>VANTIQ Catalog</title>
    <body>
        <h1>Now Viewing Catalog Hosted By ${catalogName}</h1>
        <div>
            <table>
                <tr>
                    <th>Event Type</th>
                    <th>Description</th>
                    <th>Tags</th>
                    <th>Subscribed Topic</th>
                    <th>Published Address</th>
                    <th>Actions</th>
                </tr>
                <c:forEach items="${catalogData}" var="data">
                    <tr>
                        <td>${data.get("name").getAsString()}</td>
                        <td>${data.get("description").getAsString()}</td>
                        <td>${data.get("ars_properties").get("tags").getAsJsonArray()}</td>
                        <td>${data.get("subscriber").getAsString()}</td>
                        <td>${data.get("publisher").getAsString()}</td>
                        <td>
                            <c:if test="${data.get(\"publisher\").getAsString().startsWith(\"/topics/\")}">
                                <form action="${pageContext.request.contextPath}/Publish" method="post">
                                    <input type="submit" name="publish" value="Publish">
                                    <input type="hidden" name="publishID" value="${data.get("publisher").getAsString()}">
                                    <input type="hidden" name="catalogName" value="${catalogName}">
                                </form>
                            </c:if>
                            <c:if test="${data.get(\"subscriber\").getAsString() != null}">
                                <form action="${pageContext.request.contextPath}/LiveView" method="post">
                                    <input type="submit" name="viewLive" value="View Live Events">
                                    <input type="hidden" name="eventPath" value="${data.get("subscriber").getAsString()}">
                                    <input type="hidden" name="catalogName" value="${catalogName}">
                                    <input type="hidden" name="eventName" value="${data.get("name").getAsString()}">
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </body>
</html>