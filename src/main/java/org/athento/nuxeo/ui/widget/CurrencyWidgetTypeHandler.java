package org.athento.nuxeo.ui.widget;

import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.core.ConvertNumberHandler;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import javax.faces.component.html.HtmlInputSecret;
import javax.faces.component.html.HtmlInputText;
import javax.faces.convert.NumberConverter;
import javax.faces.view.facelets.*;

/**
 * Currency widget.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class CurrencyWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = -887676775435738465L;

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
                                            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                subHandlers, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                    tagConfig, widget.getTagConfigId(), new TagAttributesImpl(
                            new TagAttribute[0]), leaf,
                    NumberConverter.CONVERTER_ID);
            ConverterHandler convert = new ConvertNumberHandler(convertConfig);
            FaceletHandler nextHandler = new CompositeFaceletHandler(
                    new FaceletHandler[] { convert, leaf });
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, nextHandler,
                    HtmlInputText.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        }
        return leaf;
    }
}
