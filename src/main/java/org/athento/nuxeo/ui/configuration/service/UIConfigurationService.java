package org.athento.nuxeo.ui.configuration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.Serializable;
import java.util.*;

/**
 * UIConfiguration Service.
 */
public class UIConfigurationService extends DefaultComponent implements UIConfigurationManager {

    private static final Log LOG = LogFactory.getLog(UIConfigurationService.class);

    /** Query for get unique document config. */
    public static final String DOC_CONFIG_UNIQUE_QUERY = "SELECT * FROM ContentViewConfig";

    private static DocumentModel configUIDoc;

    /**
     * Save content view columsn of an user.
     *
     * @param session
     * @param contentView
     * @param columns
     * @param user
     */
    @Override
    public void saveContentViewColumns(CoreSession session, String contentView, String[] columns, String user) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Save content view columns for " + user);
        }
        // Get document with ui-configuration
        loadUIConfigurationDocument(session);

        // Columns configuration
        List<Map<String, Serializable>> allColumnsConfiguration =
                (List<Map<String, Serializable>>) configUIDoc.getProperty("cvconfig", "contentView");

        // Get columns config for the user
        Map<String, Serializable> columnsConfig = getUserConfiguration(configUIDoc, user, contentView);

        if (columnsConfig == null) {
            // Add new columns configuration for the user
            columnsConfig = new HashMap<>();
            columnsConfig.put("username", user);
            columnsConfig.put("contentViewName", contentView);
            columnsConfig.put("columnNames", columns);
            allColumnsConfiguration.add(columnsConfig);
        } else {
            // Remove old columns
            removeColumnsForUserAndContentView(allColumnsConfiguration, user, contentView);
            // Update columns for the configuration
            columnsConfig.put("columnNames", columns);
            allColumnsConfiguration.add(columnsConfig);
        }

        configUIDoc.setProperty("cvconfig", "contentView", allColumnsConfiguration);

        // Save document
        session.saveDocument(configUIDoc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

    }

    /**
     * Remove columns configuration for user and contentview.
     *
     * @param allColumnsConfiguration
     * @param user
     * @param contentViewName
     */
    private void removeColumnsForUserAndContentView(List<Map<String, Serializable>> allColumnsConfiguration, String user, String contentViewName) {
        for (Iterator<Map<String, Serializable>> it = allColumnsConfiguration.iterator(); it.hasNext();) {
            Map<String, Serializable> configItem = it.next();
            String username = (String) configItem.get("username");
            String contentView = (String) configItem.get("contentViewName");
            if (username.equals(user) && contentView.equals(contentViewName)) {
                it.remove();
            }
        }
    }

    /**
     * Get UI configuration document.
     *
     * @param session
     * @return
     */
    private void loadUIConfigurationDocument(CoreSession session) {
        if (UIConfigurationService.configUIDoc == null) {
            DocumentModel doc = null;
            // Get configuration-ui document
            DocumentModelList configUIDocList = session.query(DOC_CONFIG_UNIQUE_QUERY);
            if (configUIDocList.isEmpty()) {
                // Create document with config-ui
                doc = createConfigUIDoc(session);
            } else {
                doc = configUIDocList.get(0);
            }
            UIConfigurationService.configUIDoc = doc;
            LOG.info("Document UI configuration id " + UIConfigurationService.configUIDoc.getId());
        }
    }

    /**
     * Create config-ui doc.
     */
    private DocumentModel createConfigUIDoc(CoreSession session) {
        DocumentModel doc = session.createDocumentModel("/", "doc-ui-config", "ContentViewConfig");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        return doc;
    }

    /**
     * Get user configuration.
     *
     * @param configUIDoc
     * @param user
     * @param contentViewName
     * @return
     */
    private Map<String, Serializable> getUserConfiguration(DocumentModel configUIDoc, String user, String contentViewName) {
        // Get content view columns
        List<Map<String, Serializable>> configColumns =
                (List<Map<String, Serializable>>) configUIDoc.getProperty("cvconfig", "contentView");
        for (Map<String, Serializable> configItem : configColumns) {
            String username = (String) configItem.get("username");
            String contentView = (String) configItem.get("contentViewName");
            if (username.equals(user) && contentView.equals(contentViewName)) {
                return configItem;
            }
        }
        return null;
    }

    /**
     * Get columns of content view for an user.
     *
     * @param session
     * @param contentView
     * @param user
     * @return
     */
    @Override
    public String[] getContentViewColumns(CoreSession session, String contentView, String user) {
        // Get configuration-ui document
        loadUIConfigurationDocument(session);
        // Return columns
        Map<String, Serializable> columnsConfig = getUserConfiguration(configUIDoc, user, contentView);
        if (columnsConfig != null) {
            return (String[]) columnsConfig.get("columnNames");
        }
        return new String[0];
    }


}
