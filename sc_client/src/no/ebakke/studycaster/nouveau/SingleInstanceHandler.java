package no.ebakke.studycaster.nouveau;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

public class SingleInstanceHandler {
  private boolean                messagePending;
  private SingleInstanceListener clientListener;
  private SingleInstanceService  service;

  private final SingleInstanceListener proxyListener = new SingleInstanceListener() {
    public void newActivation(String[] strings) {
      callIfPendingAndReady(true);
    }
  };

  private synchronized void callIfPendingAndReady(boolean forcePending) {
    final SingleInstanceListener listener;
    synchronized (this) {
      messagePending |= forcePending;
      listener = messagePending ? clientListener : null;
      if (listener != null)
        messagePending = false;
    }
    if (listener != null)
      listener.newActivation(null);
  }

  public void setListener(SingleInstanceListener listener) {
    synchronized (this) {
      if (clientListener != null)
        throw new IllegalStateException("Already set the SingleInstanceListener");
      clientListener = listener;
    }
    callIfPendingAndReady(false);
  }

  public SingleInstanceHandler() throws UnavailableServiceException {
    Object singleInstanceService = ServiceManager.lookup("javax.jnlp.SingleInstanceService");
    if (singleInstanceService == null || !(singleInstanceService instanceof SingleInstanceService))
      throw new UnavailableServiceException("Unexpected return value from ServiceManager.lookup()");
    this.service = (SingleInstanceService) singleInstanceService;
    this.service.addSingleInstanceListener(proxyListener);
  }

  public void close() {
    if (service != null) {
      service.removeSingleInstanceListener(proxyListener);
      service = null;
    }
  }
}
