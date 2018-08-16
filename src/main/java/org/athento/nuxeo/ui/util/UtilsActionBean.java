package org.athento.nuxeo.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.automation.client.model.DateUtils;

import java.io.Serializable;
import java.util.GregorianCalendar;

/**
 * Util action.
 */
@Name("athentoUtilAction")
@Scope(ScopeType.CONVERSATION)
public class UtilsActionBean implements Serializable {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(UtilsActionBean.class);

    /**
     * Format date.
     *
     * @param value
     * @return
     */
    public String formatDate(GregorianCalendar value) {
        return DateUtils.formatDate(value.getTime());
    }

}
