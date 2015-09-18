/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import searcher.WT10GRetriever;
import javax.servlet.http.HttpSession;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import searcher.ClueWebRetriever;

/**
 *
 * @author dganguly
 */
public class SearchServlet extends HttpServlet {

    String propFileName;
    WT10GRetriever retriever;

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
            String pageNum = request.getParameter("page");
           
            JsonObject json;
            Query query = retriever.buildQuery(queryStr);
            if (pageNum == null) { // no pagination workflow
                TopDocs topDocs = retriever.retrieve(query);
                json = retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs);
            } else { // pagination workflow
                int page = Integer.parseUnsignedInt(pageNum);
                TopDocs topDocs = getCachedTopDocs(request, queryStr, query);
                json = retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs, page);
            }
            jsonWriter.write(json);
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
