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
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import searcher.WT10GRetriever;
import javax.servlet.http.HttpSession;
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
            propFileName= config.getInitParameter("configFile");
            //retriever = new WT10GRetriever(propFileName);
            
            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));
            String retrClassName = prop.getProperty("retriever.type");
            if (retrClassName.equals("WT10G"))
                retriever = new WT10GRetriever(propFileName);
            else
                retriever = new ClueWebRetriever(propFileName);
        }
        catch (Exception ex) {
            ex.printStackTrace();            
        }
    }
    
    /* Return the snapshot of the current page */
    String getPageViewOfResultList(HttpServletRequest request,
            String query, String pageNumberStr)
            throws Exception {
        
        int pageNumber = Integer.parseInt(pageNumberStr);
        HttpSession session = request.getSession();
        TopDocs topDocs = (TopDocs)session.getAttribute(query);
        if (topDocs != null) {
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            return retriever.constructJSONForRetrievedSet(query, scoreDocs, pageNumber);
        }       
        topDocs = retriever.retrieve(query, pageNumber);
        session.setAttribute(query, topDocs);
        return retriever.constructJSONForRetrievedSet(query, topDocs.scoreDocs, pageNumber);
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
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String html = null;
        try {
            String queryStr = request.getParameter("query");
			System.out.println("query = |" + queryStr + "|");
            String pageNum = request.getParameter("page");
            
            if (pageNum == null) { // no pagination workflow
                html = retriever.retrieve(queryStr);
            }
            else { // pagination workflow
                html = getPageViewOfResultList(request, queryStr, pageNum);
            }
            
            out.println(html);                
            out.close();
        }
        catch (Exception ex) {
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
