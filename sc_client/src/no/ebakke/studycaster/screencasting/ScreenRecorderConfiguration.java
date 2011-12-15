package no.ebakke.studycaster.screencasting;

// TODO: Get the ScreenCensor in here, too.
public class ScreenRecorderConfiguration {
  // TODO: Move this into the XML configuration and move this class to the configuration package.
  public static final ScreenRecorderConfiguration DEFAULT =
      new ScreenRecorderConfiguration(15.0, 0.05, 5.0, 0.1, 5.0, 0.7);

  private final double maxPointerSamplingFrequency;
  private final double maxPointerDutyCycle;
  private final double maxDesktopMetaSamplingFrequency;
  private final double maxDesktopMetaDutyCycle;
  private final double maxFrameSamplingFrequency;
  private final double maxFrameDutyCycle;

  public ScreenRecorderConfiguration(double maxPointerSamplingFrequency,
      double maxPointerDutyCycle, double maxDesktopMetaSamplingFrequency,
      double maxDesktopMetaDutyCycle, double maxFrameSamplingFrequency, double maxFrameDutyCycle)
  {
    this.maxPointerSamplingFrequency = maxPointerSamplingFrequency;
    this.maxPointerDutyCycle = maxPointerDutyCycle;
    this.maxDesktopMetaSamplingFrequency = maxDesktopMetaSamplingFrequency;
    this.maxDesktopMetaDutyCycle = maxDesktopMetaDutyCycle;
    this.maxFrameSamplingFrequency = maxFrameSamplingFrequency;
    this.maxFrameDutyCycle = maxFrameDutyCycle;
  }

  public double getMaxFrameDutyCycle() {
    return maxFrameDutyCycle;
  }

  public double getMaxFrameSamplingFrequency() {
    return maxFrameSamplingFrequency;
  }

  public double getMaxPointerDutyCycle() {
    return maxPointerDutyCycle;
  }

  public double getMaxPointerSamplingFrequency() {
    return maxPointerSamplingFrequency;
  }

  public double getMaxDesktopMetaDutyCycle() {
    return maxDesktopMetaDutyCycle;
  }

  public double getMaxDesktopMetaSamplingFrequency() {
    return maxDesktopMetaSamplingFrequency;
  }
}
