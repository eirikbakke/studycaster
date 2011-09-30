package no.ebakke.studycaster.screencasting;

// TODO: Get the ScreenCensor in here, too.
public class ScreenRecorderConfiguration {
  public static final ScreenRecorderConfiguration DEFAULT =
      new ScreenRecorderConfiguration(15.0, 0.1, 5.0, 0.7);

  private double maxFrameSamplingFrequency;
  private double maxFrameDutyCycle;
  private double maxPointerSamplingFrequency;
  private double maxPointerDutyCycle;

  public ScreenRecorderConfiguration(double maxPointerSamplingFrequency,
      double maxPointerDutyCycle, double maxFrameSamplingFrequency, double maxFrameDutyCycle)
  {
    this.maxPointerSamplingFrequency = maxPointerSamplingFrequency;
    this.maxPointerDutyCycle = maxPointerDutyCycle;
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
}
