package org.athento.nuxeo.ui.navigation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.configuration.UserUIConfigurationAction;
import org.athento.nuxeo.ui.configuration.service.UIConfigurationManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Observer;
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
import java.util.*;

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

    private long currentPage;

    private Map<String, Long> indexes = new HashMap<String, Long>();

    /**
     * Goto page.
     */
    @Override
    public void gotoPage(long page, String contentView) {
        PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.setCurrentPageIndex(page - 1);
        this.currentPage = provider.getCurrentPageIndex();
        this.indexes.put(contentView, this.currentPage);
    }

    /**
     * Get current page.
     *
     * @return
     */
    public long getCurrentPage() {
        if (this.currentPage < 0) {
            this.currentPage = 0;
        }
        return this.currentPage + 1;
    }

    /**
     * @return
     */
    @Override
    public long getCurrentPage(String contentView) {
        Long value = indexes.get(contentView);
        if (value == null) {
            value = 0L;
        }
        return value;
    }

    /**
     * @param currentPage
     */
    @Override
    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public void setCurrentContentView(String contentView) {
        this.gotoPage(this.currentPage, contentView);
    }

    @Override
    public void previous(String contentView) {
        PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        Long index = indexes.get(contentView);
        if (index == null) {
            index = 0L;
        }
        provider.setCurrentPageIndex(index - 1);
        this.currentPage = provider.getCurrentPageIndex();
        this.indexes.put(contentView, this.currentPage);
    }

    @Override
    public void next(String contentView) {
        PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        Long index = indexes.get(contentView);
        if (index == null) {
            index = 0L;
        }
        provider.setCurrentPageIndex(index + 1);
        this.currentPage = provider.getCurrentPageIndex();
        this.indexes.put(contentView, this.currentPage);

    }

    @Override
    public void last(String contentView) {
        PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.lastPage();
        this.currentPage = provider.getCurrentPageIndex();
        this.indexes.put(contentView, this.currentPage);

    }

    @Override
    public void rewind(String contentView) {
        PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.firstPage();
        this.currentPage = provider.getCurrentPageIndex();
        this.indexes.put(contentView, this.currentPage);
    }

    /**
     * Refresh indexes.
     */
    public void refresh(String contentView) {
        this.indexes.remove(contentView);
    }

    /**
     * Refresh all indexes.
     */
    @Observer(ContentView.CONTENT_VIEW_REFRESH_EVENT)
    public void refreshAll() {
        // Calculate index for each content view
        for (String contentView : indexes.keySet()) {
            PageProvider<?> provider = this.contentViewActions.getContentViewWithProvider(contentView)
                    .getPageProvider();
            provider.setCurrentPageIndex(0);
            this.indexes.put(contentView, 0L);
        }
    }
}
