package tutorial;

import javax.faces.bean.*;

@ManagedBean   
public class Navigator {
  public String choosePage() {
    String[] results = 
      { "page1", "page2", "page3" };
    return(RandomUtils.randomElement(results));
  }
}
