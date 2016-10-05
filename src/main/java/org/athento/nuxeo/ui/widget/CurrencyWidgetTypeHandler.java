package org.athento.nuxeo.ui.widget;

import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.core.ConvertNumberHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import javax.faces.component.html.HtmlInputSecret;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.convert.DoubleConverter;
import javax.faces.convert.NumberConverter;
import javax.faces.view.facelets.*;

/**
 * Currency widget.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class CurrencyWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = -887676775435738465L;

    private static final Log LOG = LogFactory.getLog(CurrencyWidgetTypeHandler.class);

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
                                            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        TagAttributes attributes;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            attributes = helper.getTagAttributes(widget);
        } else {
            attributes = helper.getTagAttributes(widgetId, widget);
        }
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                subHandlers, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                    tagConfig, widget.getTagConfigId(), new TagAttributesImpl(
                            new TagAttribute[0]), leaf,
                    DoubleConverter.CONVERTER_ID);
            ConverterHandler convert = new ConverterHandler(convertConfig);
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, convert,
                    HtmlInputText.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else if (BuiltinWidgetModes.VIEW.equals(mode)) {
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                    tagConfig, widget.getTagConfigId(), attributes, leaf,
                    NumberConverter.CONVERTER_ID);
            ConverterHandler convert = new ConvertNumberHandler(convertConfig);
            FaceletHandler nextHandler = new CompositeFaceletHandler(
                    new FaceletHandler[] { convert, leaf });
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, nextHandler,
                    HtmlOutputText.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { output, message };
            return new CompositeFaceletHandler(handlers);
        } else if (BuiltinWidgetModes.CSV.equals(mode)) {
            // default on text without any converter to ease format
            // configuration
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            return output;
        }
        return leaf;
    }
}
