package org.athento.nuxeo.ui.navigation;

/**
 * Created by victorsanchez on 19/05/16.
 */
public interface NavigationAction {

    /**
     * Goto page.
     */
    void gotoPage(long page, String contentView);

    /**
     * Get current page.
     *
     * @return
     */
    long getCurrentPage();

    /**
     * Get current page.
     *
     * @return
     */
    long getCurrentPage(String contentView);

    /**
     * Set current page.
     *
     * @param currentPage
     */
    void setCurrentPage(long currentPage);

    void previous(String contentView);

    void next(String contentView);

    void last(String contentView);

    void rewind(String contentView);

    void setCurrentContentView(String contentView);


}
