/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Administrator
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin"})
public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /** 
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    System.out.println("Requested using: " + request.getRequestURL());
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("Pragma"       , "no-cache");
    // TODO: Remember that the button image and script will be remote.
    String scriptCode =
      "<script type=\"text/javascript\" src=\"deployJava.min.js\"></script>\n" +
      "<script type=\"text/javascript\">\n" +
      "  deployJava.launchButtonPNG = 'webstart_button.png';\n" +
      "  deployJava.createWebStartLaunchButton(\"launch_client\", \"1.5\");\n" +
      "</script>\n";
    request.setAttribute("scriptCode", scriptCode);
    request.setAttribute("listTest", Arrays.asList(
        new String[] {"item one", "item two", "ithem 3"}));
    RequestDispatcher rd = getServletContext().getRequestDispatcher("/WEB-INF/admin.jspx");
    rd.forward(request, response);
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /** 
   * Handles the HTTP <code>GET</code> method.
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
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>
}
