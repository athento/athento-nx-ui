package org.athento.nuxeo.ui.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.fluent.Content;
import org.athento.nuxeo.ui.configuration.service.UIConfigurationManager;
import org.jboss.seam.annotations.*;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.ScopeType.STATELESS;

/**
 * Created by victorsanchez on 19/05/16.
 */
@Name("userUIConfiguration")
@Scope(SESSION)
public class UserUIConfigurationBean implements UserUIConfigurationAction, Serializable {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(UserUIConfigurationBean.class);

    /** Document manager. */
    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    @In(required = true)
    protected Principal currentUser;

    private boolean LOADED = false;

    /**
     * Destroy.
     */
    @Observer("contentViewRefresh")
    public void updateContentViewsConfiguration() {
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        Set<String> contentViewNames = contentViewService.getContentViewNames();
        for (String contentViewName : contentViewNames) {
            ContentView contentView = contentViewActions.getContentView(contentViewName);
            if (contentView.getCurrentResultLayoutColumns() != null) {
                saveContentViewConfiguration(contentViewName,
                        contentView.getCurrentResultLayoutColumns().toArray(new String[0]),
                        this.currentUser.getName());
            }
        }
    }

    /**
     * Save content view configuration.
     *
     * @param contentView
     * @param columns
     * @param user
     */
    public void saveContentViewConfiguration(String contentView, String [] columns, String user) {
        LOG.info("Saving content view configuration for " + user);
        boolean needClose = false;
        if (this.documentManager == null) {
            this.documentManager = CoreInstance.openCoreSession("default");
            needClose = true;
        }
        // Save content view columns
        UIConfigurationManager uiConfigurationManager = Framework.getService(UIConfigurationManager.class);
        uiConfigurationManager.saveContentViewColumns(documentManager, contentView, columns, user);
        if (this.documentManager != null && needClose) {
            CoreInstance.closeCoreSession(this.documentManager);
        }
    }

    /**
     * Load configurations.
     */
    @Observer(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)
    public void loadConfiguration() {
        // Get all content-views
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        Set<String> cvws = contentViewService.getContentViewNames();
        LOG.info("Current user " + currentUser);
        for (String cvw : cvws) {
            loadContentViewConfiguration(cvw, currentUser.getName());
        }
    }

    /**
     * Load content view configuration.
     *
     * @param contentView
     * @param user
     */
    public void loadContentViewConfiguration(String contentView, String user) {
        if (!LOADED) {
            LOG.info("Loading content view configuration for " + user);
            // Load content view columns
            UIConfigurationManager uiConfigurationManager = Framework.getService(UIConfigurationManager.class);
            String[] columns = uiConfigurationManager.getContentViewColumns(documentManager, contentView, user);
            for (String column : columns) {
                LOG.info("Column " + column);
            }
            // TODO: Add to user content view in session
            LOADED = true;
        }
    }


}
