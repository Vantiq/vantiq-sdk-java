package io.vantiq.prontoClient.servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Servlet implementation class ProntoClientServlet
 */
@WebServlet("/Catalog")
public class CatalogServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Parameter string for allCatalogs.jsp's form field
    private static final String PARAM_CATALOG_NAME  = "catalogName";
    
    // Global vars
    HashMap<String,Vantiq> vantiqMap = ProntoClientServlet.vantiqMap;
    Gson gson = new Gson();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public CatalogServlet() {
        super();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {   
        // Retrieving all relevant data from view
        String catalogName = request.getParameter(PARAM_CATALOG_NAME);
        
        // Get vantiq instance based on session
        Vantiq vantiq = vantiqMap.get(request.getSession().getId());
            
        // Fetching list of all known event types for a manager
        HashMap<String,String> catalogFilter = new HashMap<String,String>();
        catalogFilter.put("managerNode", catalogName);
        VantiqResponse catalogResponse = vantiq.execute("Broker.getAllEvents", catalogFilter);
        JsonArray catalogArray = (JsonArray) catalogResponse.getBody();
        
        // Getting the list of subscribers/publishers for the given namespace
        VantiqResponse subscriberResponse = vantiq.select("ArsEventSubscriber", null, null, null);
        VantiqResponse publisherResponse = vantiq.select("ArsEventPublisher", null, null, null);
        ArrayList<JsonObject> subscriberArray = (ArrayList<JsonObject>) subscriberResponse.getBody();
        ArrayList<JsonObject> publisherArray = (ArrayList<JsonObject>) publisherResponse.getBody();
        
        // Iterate over JsonArray and store in ArrayList (.jsp file can't iterate over JsonArray)
        ArrayList<JsonObject> catalogArrayList = new ArrayList<JsonObject>();
        for (int i = 0; i < catalogArray.size(); i++) {
            JsonObject elem = catalogArray.get(i).getAsJsonObject();
            
            // Merge subscriber information to event types
            for (int j = 0; j < subscriberArray.size(); j++) {
                if (subscriberArray.get(j).get("name").getAsString().equals(elem.get("name").getAsString())) {
                    elem.addProperty("subscriber", subscriberArray.get(j).get("localName").getAsString());
                }
            }
            
            // Merge publisher information to event types
            for (int j = 0; j < publisherArray.size(); j++) {
                if (publisherArray.get(j).get("name").getAsString().equals(elem.get("name").getAsString())) {
                    elem.addProperty("publisher", publisherArray.get(j).get("localEvent").getAsString());
                }
            }                
            catalogArrayList.add(elem);
        }

        // Display the appropriate view
        request.setAttribute("catalogName", catalogName);
        request.setAttribute("catalogData", catalogArrayList);
        RequestDispatcher view = request.getRequestDispatcher("catalog.jsp");
        view.forward(request, response);
            
    }
}
