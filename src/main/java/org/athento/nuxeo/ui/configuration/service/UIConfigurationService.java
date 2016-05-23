package org.athento.nuxeo.ui.configuration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UIConfiguration Service.
 *
 */
public class UIConfigurationService extends DefaultComponent implements UIConfigurationManager {

    private static final Log LOG = LogFactory.getLog(UIConfigurationService.class);

    public static final String DOC_CONFIG_UNIQUE_QUERY = "SELECT * FROM ContentViewConfig";

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
        LOG.info("Save content view columns for " + user);
        DocumentModel configUIDoc = getUIConfigurationDocument(session);

        // Set columns
        HashMap<String, Serializable> userInfo = null;
        Map.Entry<String, Serializable> columnsConfig = getUserConfiguration(configUIDoc, user);
        if (columnsConfig == null) {
            userInfo = new HashMap<>();

        }

        configUIDoc.setPropertyValue("cvconfig:userInfo", userInfo);

        LOG.info("Columns save " + columnsConfig);

        // Save document
        session.saveDocument(configUIDoc);
        session.save();

        LOG.info("=" + configUIDoc.getId());
    }

    /**
     * Get UI configuration document.
     *
     * @param session
     * @return
     */
    private DocumentModel getUIConfigurationDocument(CoreSession session) {
        DocumentModel doc = null;
        // Get configuration-ui document
        DocumentModelList configUIDocList = session.query(DOC_CONFIG_UNIQUE_QUERY);
        if (configUIDocList.isEmpty()) {
            // Create document with config-ui
            doc = createConfigUIDoc(session);
        } else {
            doc = configUIDocList.get(0);
        }
        return doc;
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
     * @return
     */
    private Map.Entry<String,Serializable> getUserConfiguration(DocumentModel configUIDoc, String user) {
        // Get content view columns
        List<Map<String, Serializable>> configColumns =
                (List<Map<String, Serializable>>) configUIDoc.getProperty("cvconfig", "contentview");
        LOG.info("Config " + configColumns);
        /*for (Map.Entry<String, Serializable> configItem : configs.entrySet()) {
            String username = configItem.getKey();
            if (username.equals(user)) {
                return configItem;
            }
        }*/
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
        DocumentModel configUIDoc = getUIConfigurationDocument(session);
        LOG.info("=" + configUIDoc.getId());
        // Return columns
        Map.Entry<String, Serializable> columnsConfig = getUserConfiguration(configUIDoc, user);
        if (columnsConfig != null) {
            return (String []) columnsConfig.getValue();
        }
        return new String[0];
    }


}
