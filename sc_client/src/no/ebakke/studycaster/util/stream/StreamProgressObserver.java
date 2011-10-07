package no.ebakke.studycaster.util.stream;

public interface StreamProgressObserver {
  public void updateProgress(NonBlockingOutputStream nbos);
}
