package org.athento.nuxeo.converter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.DoubleConverter;
import javax.faces.convert.FacesConverter;

import static org.athento.nuxeo.converter.AthentoNumberConverter.CONVERTER_ID;

/**
 * Converter ID.
 */
@FacesConverter(CONVERTER_ID)
public class AthentoNumberConverter extends DoubleConverter {

    public static final String CONVERTER_ID = "AthentoNumberConverter";
    public static final Log LOG = LogFactory.getLog(AthentoNumberConverter.class);


    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String s) {
        s = s.replace(",",".");
        return super.getAsObject(facesContext, uiComponent, s);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        String s = super.getAsString(context, component, value);
        return s.replace(".",",");
    }
}
