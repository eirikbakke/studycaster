package no.ebakke.studycaster.backend;

import java.util.Date;

public class Request {
  private Long   id;
  private Date   time;
  private String type;

  Request() {
  }

  public Request(Date time, String type) {
    this.time = (Date) time.clone();
    this.type = type;
  }

  public Date getTime() {
    return (Date) time.clone();
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "Request(" + id + ", " + time + ", " + type + ")";
  }
}
