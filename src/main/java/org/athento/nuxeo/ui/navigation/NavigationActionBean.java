package org.athento.nuxeo.ui.navigation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.configuration.UserUIConfigurationAction;
import org.athento.nuxeo.ui.configuration.service.UIConfigurationManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.jboss.seam.ScopeType.SESSION;

/**
 * Navigation action.
 */
@Name("navigationAction")
@Scope(ScopeType.CONVERSATION)
public class NavigationActionBean implements NavigationAction, Serializable {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(NavigationActionBean.class);

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    /** Value used to jump to page. */
    private long currentPage;

    /**
     * Init.
     */
    @Create
    public void init() {
        this.currentPage = this.contentViewActions.getCurrentContentView()
                .getPageProvider().getCurrentPageIndex() + 1;
    }

    /**
     * Goto page.
     */
    @Override
    public void gotoPage() {
        PageProvider<?> provider = this.contentViewActions.getCurrentContentView()
                .getPageProvider();
        if (this.currentPage < 0) {
            this.currentPage = 1;
        }
        if (this.currentPage >= provider.getNumberOfPages()) {
            this.currentPage = provider.getNumberOfPages();
        }
        provider.setCurrentPageIndex(this.currentPage - 1);
    }

    /**
     *
     * @return
     */
    @Override
    public long getCurrentPage() {
        return currentPage;
    }

    /**
     *
     * @param currentPage
     */
    @Override
    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
        gotoPage();
    }
}
