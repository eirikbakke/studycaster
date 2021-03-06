package no.ebakke.studycaster.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

/** Thread-safe. */
public class SingleInstanceHandler {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private final List<List<String>>    pending = new ArrayList<List<String>>();
  private final SingleInstanceService service;
  private SingleInstanceListener      clientListener;

  private final SingleInstanceListener proxyListener = new SingleInstanceListener() {
    public void newActivation(String[] strings) {
      LOG.info("User attempted to open an additional instance of the client");
      synchronized (SingleInstanceHandler.this) {
        pending.add(new ArrayList<String>(Arrays.asList(strings)));
        callPendingWhenReady();
      }
    }
  };

  private void callPendingWhenReady() {
    SingleInstanceListener listenerCopy = null;
    List<List<String>>     pendingCopy  = null;
    synchronized (this) {
      if (clientListener != null) {
        listenerCopy = clientListener;
        pendingCopy = new ArrayList<List<String>>(pending);
        pending.clear();
      }
    }
    if (listenerCopy != null) {
      for (final List<String> args : pendingCopy)
        listenerCopy.newActivation(args.toArray(new String[args.size()]));
    }
  }

  public synchronized void setListener(SingleInstanceListener listener) {
    if (clientListener != null)
      throw new IllegalStateException("Already set the SingleInstanceListener");
    clientListener = listener;
    callPendingWhenReady();
  }

  public SingleInstanceHandler() throws UnavailableServiceException {
    Object singleInstanceService = ServiceManager.lookup("javax.jnlp.SingleInstanceService");
    if (singleInstanceService == null || !(singleInstanceService instanceof SingleInstanceService))
      throw new UnavailableServiceException("Unexpected return value from ServiceManager.lookup()");
    this.service = (SingleInstanceService) singleInstanceService;
    this.service.addSingleInstanceListener(proxyListener);
  }

  public synchronized void close() {
    service.removeSingleInstanceListener(proxyListener);
    pending.clear();
  }
}
