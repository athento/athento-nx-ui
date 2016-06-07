package org.athento.nuxeo.ui.navigation;

/**
 * Created by victorsanchez on 19/05/16.
 */
public interface NavigationAction {

    /**
     * Goto page.
     */
    void gotoPage();

    /**
     * Get current page.
     *
     * @return
     */
    long getCurrentPage();

    /**
     * Set current page.
     *
     * @param currentPage
     */
    void setCurrentPage(long currentPage);
    

}
