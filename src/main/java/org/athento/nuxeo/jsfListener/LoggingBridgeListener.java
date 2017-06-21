package org.athento.nuxeo.jsfListener;

import javax.servlet.ServletContextEvent;

/**
 * Loggin bridge to pass logger to Log4j.
 */
public class LoggingBridgeListener {

    @Override
    public void contextInitialized(ServletContextEvent arg) {
        System.out.println("contextInitialized....");

        //remove the jsf root logger, avoid duplicated logging
        //try comment out this and see the different on the console
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg) {
        System.out.println("contextDestroyed....");

    }


}
