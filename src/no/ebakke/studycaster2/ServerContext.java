package no.ebakke.studycaster2;

import java.net.URL;


/** Handles protocol details specific to our server-side PHP script. */
public class ServerContext {
  private static final int CLIENT_TICKET_BYTES = 6;
  private URL    serverScriptURL;
  private Ticket ticketFC; // First client ticket on this machine
  private Ticket ticketCC; // Current client ticket
  private Ticket ticketFS; // First server ticket on this machine
  private Ticket ticketCS; // Current server ticket

  public ServerContext(URL serverScriptURL) {
    this.serverScriptURL = serverScriptURL;
    ticketCC = new Ticket(CLIENT_TICKET_BYTES);

    
  }
}
