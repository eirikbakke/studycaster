package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.reporting.Reports;
import no.ebakke.studycaster.reporting.Reports.Launch;
import no.ebakke.studycaster.reporting.Reports.Subject;

/**
 *
 * @author ebakke
 */
@WebServlet(name = "PlainSubjectReportServlet", urlPatterns = {"/subjectreport.tsv"})
public class PlainSubjectReportServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    List<Subject> subjectReport = Reports.getSubjectReport(LifeCycle.getSessionFactory(req));
    int subjectNumber = 1;
    for (Subject subject : subjectReport) {
      for (Launch launch : subject.getLaunches()) {
        resp.getWriter().println(subjectNumber + "\t" + launch.getLaunchTicket());
      }
      subjectNumber++;
    }
  }
}
