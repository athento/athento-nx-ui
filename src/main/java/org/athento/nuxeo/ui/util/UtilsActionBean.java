package org.athento.nuxeo.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Util action.
 */
@Name("athentoUtilAction")
@Scope(ScopeType.SESSION)
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
    public String formatDate(Date value) {
        LOG.info("Format " + value);
        if (value != null) {
            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(value);
        }
        return null;
    }


public static void main(String [] args) {
        UtilsActionBean uab = new UtilsActionBean();
        System.out.println(uab.formatDate(Calendar.getInstance().getTime()));
    }
}
