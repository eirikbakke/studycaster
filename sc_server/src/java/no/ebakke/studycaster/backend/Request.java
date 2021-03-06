package no.ebakke.studycaster.backend;

import java.util.Date;

public class Request {
  /* TODO: Don't store timestamps using Date via Hibernate/JDBC, since JDBC drivers are inconsistent
           in their handling of time zones. See http://stackoverflow.com/questions/4123534 . */
  private Long    id;
  private Date    time;
  private String  type;
  private Long    contentSize;
  private String  remoteAddrHash; // Formerly known as ticketCS
  private String  geoLocation;
  private String  launchTicket; // Formerly known as ticketCC
  private String  clientCookie; // Formerly known as ticketCF
  private String  logEntry;

  Request() {
  }

  public Request(Date time, String type, Long contentSize,
      String remoteAddrHash, String geoLocation, String launchTicket,
      String clientCookie, String logEntry)
  {
    this.time           = (Date) time.clone();
    this.type           = type;
    this.contentSize    = contentSize;
    this.remoteAddrHash = remoteAddrHash;
    this.geoLocation    = geoLocation;
    this.launchTicket   = launchTicket;
    this.clientCookie   = clientCookie;
    this.logEntry       = logEntry;
  }

  public Date getTime() {
    return (Date) time.clone();
  }

  public String getType() {
    return type;
  }

  public Long getContentSize() {
    return contentSize;
  }

  public String getRemoteAddrHash() {
    return remoteAddrHash;
  }

  public String getGeoLocation() {
    return geoLocation;
  }

  public String getLaunchTicket() {
    return launchTicket;
  }

  public String getClientCookie() {
    return clientCookie;
  }

  public String getLogEntry() {
    return logEntry;
  }

  @Override
  public String toString() {
    return "Request(" + id + ", " + time + ", " + type + ", " + remoteAddrHash +
        ", " + launchTicket + ", " + logEntry + ")";
  }
}
