package no.ebakke.studycaster.domain;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {
  private HibernateUtil() { }

  private static final SessionFactory sessionFactory;
  
  static {
    try {
      /* Create the SessionFactory from standard (hibernate.cfg.xml) config
      file. */
      sessionFactory = new Configuration().configure().buildSessionFactory();
    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  public static SessionFactory getSessionFactory() {
    return sessionFactory;
  }
}
