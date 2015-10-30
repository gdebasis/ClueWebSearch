/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import searcher.WT10GRetriever;
import javax.servlet.http.HttpSession;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import searcher.ClueWebRetriever;
import searcher.MockRetriever;
import searcher.Retriever;

/**
 *
 * @author dganguly
 */
public class SearchServlet extends HttpServlet {

    String propFileName;
    Retriever retriever;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            propFileName = config.getInitParameter("configFile");
            //retriever = new WT10GRetriever(propFileName);

            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));
            String retrClassName = prop.getProperty("retriever.type");
            
            if (retrClassName.equals("WT10G")) {
                retriever = new WT10GRetriever(propFileName);
            } else {
                retriever = new ClueWebRetriever(propFileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //retriever = new MockRetriever();
        }
    }

    TopDocs getCachedTopDocs(HttpServletRequest request, String queryStr, Query query)
            throws Exception {

        HttpSession session = request.getSession();
        TopDocs topDocs = (TopDocs) session.getAttribute(queryStr);
        if (topDocs == null) {
            topDocs = retriever.retrieve(query);
            session.setAttribute(queryStr, topDocs);
        }
        return topDocs;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter printWriter = response.getWriter();
                JsonWriter jsonWriter = Json.createWriter(printWriter)) {

            String queryStr = request.getParameter("query");
            System.out.println("query = |" + queryStr + "|");
            Query query = retriever.buildQuery(queryStr);
            System.out.println(request.getParameterMap());

            /*
            request: Websearch?query=<q:String>&start=<s:int>&howMany=<h:int>
            response: {"totHits":<int>, "results":[<r_s:JsonObject>, ..., <r_(s+h):JsonObject>]} or {"error":<message:String>} 
            */
            String startPar = request.getParameter("start");
            String howManyPar = request.getParameter("howMany");
            if (startPar != null && howManyPar != null) {
                try {
                    int start = Integer.parseUnsignedInt(startPar);
                    int howMany = Integer.parseUnsignedInt(howManyPar);
                    TopDocs topDocs = getCachedTopDocs(request, queryStr, query);
                    jsonWriter.write(retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs, start, howMany));
                    return;
                } catch (NumberFormatException e) {
                    System.err.println("start/howMany: " + startPar + "/" + howManyPar);
                    System.err.println(e);
                }
            }

            /*
            request: Websearch?query=<q:String>&selection=[<i_1:int>, ..., <i_n:int>]
            response: {"totHits":<int>, "results":[<r_(i_1):JsonObject>, ..., <r_(i_n):JsonObject>]} or {"error":<message:String>} 
            */
            String selectionPar = request.getParameter("selection");
            if (selectionPar != null) {
                try (JsonReader reader = Json.createReader(new StringReader(selectionPar))) {
                    JsonArray array = reader.readArray(); //JsonParsingException? seems to throw JsonException
                    List<Integer> selection = new ArrayList<>(array.size());
                    for (JsonNumber number : array.getValuesAs(JsonNumber.class)) { //ClassCastException
                        selection.add(number.intValueExact()); //ArithmeticException
                    }
                    TopDocs topDocs = getCachedTopDocs(request, queryStr, query);
                    jsonWriter.write(retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs, selection));
                    return;
                } catch (JsonException | ClassCastException | ArithmeticException e) {
                    System.err.println("selection: " + selectionPar);
                    System.err.println(e);
                }
            }
            
            /*
            request: Websearch?query=<q:String>&page=<p:int>
            response: {"totHits":<int>, "results":[<r_p[0]:JsonObject>, ..., <r_(p[10]):JsonObject>]} or {"error":<message:String>} 
            */
            String pagePar = request.getParameter("page");
            if (pagePar != null) {
                try {
                    int page = Integer.parseUnsignedInt(pagePar);
                    TopDocs topDocs = getCachedTopDocs(request, queryStr, query);
                    jsonWriter.write(retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs, page));
                    return;
                } catch (NumberFormatException e) {
                    System.err.println("page: " + pagePar);
                    System.err.println(e);
                }
            }
            
            /*
            request: Websearch?query=<q:String>
            response: {"totHits":<int>, "results":[<r_0:JsonObject>, ..., <r_(tot-1):JsonObject>} or {"error":<message:String>} 
            */

            { // default: all documents (without cache)
                TopDocs topDocs = retriever.retrieve(query);
                jsonWriter.write(retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
