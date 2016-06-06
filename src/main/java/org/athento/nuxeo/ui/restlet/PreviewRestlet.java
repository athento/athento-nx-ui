package org.athento.nuxeo.ui.restlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.NothingToPreviewException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Preview restlet with no auth. Based on PreviewRestlet.java of Nuxeo DM (c).
 */
@Name("athentoPreviewRestlet")
@Scope(ScopeType.EVENT)
public class PreviewRestlet extends BaseNuxeoRestlet {

    private static final Log LOG = LogFactory.getLog(PreviewRestlet.class);
    private static final String TOKEN_ENDCHARS_CONTROL = "#control";

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    protected DocumentModel targetDocument;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected static final List<String> previewInProcessing = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public void handle(Request req, Response res) {

        LOG.info("Previewing...");

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String xpath = (String) req.getAttributes().get("fieldPath");
        String token = getQueryParamValue(req, "token", "");

        xpath = xpath.replace("-", "/");
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        for (int i = 6; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error(e);
        }

        String blobPostProcessingParameter = getQueryParamValue(req,
                "blobPostProcessing", "false");
        boolean blobPostProcessing = Boolean.parseBoolean(blobPostProcessingParameter);

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }
        if (docid == null || repo.equals("*")) {
            handleError(res, "you must specify a documentId");
            return;
        }

        try {
            Framework.login();
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            targetDocument = documentManager.getDocument(new IdRef(docid));
        } catch (ClientException e) {
            LOG.error("Unable to get document from session", e);
            handleError(res, e);
            return;
        } catch (LoginException e) {
            LOG.error("Login error in Athento preview", e);
            return;
        }

        LOG.info("Subpath " + subPath + " token " + token);

        if (!ignoreSubpathAccess(subPath)) {
            if (!validToken(token, targetDocument)) {
                handleError(res, "Token is invalid.");
                return;
            }
        }

        List<Blob> previewBlobs;
        try {
            previewBlobs = initCachedBlob(res, xpath, blobPostProcessing);
        } catch (Exception e) {
            handleError(res, "unable to get preview");
            return;
        }
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            // response was already handled by initCachedBlob
            return;
        }
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            if (subPath == null || "".equals(subPath)) {
                handlePreview(res, previewBlobs.get(0), "text/html");
                return;
            } else {
                for (Blob blob : previewBlobs) {
                    if (subPath.equals(blob.getFilename())) {
                        handlePreview(res, blob, blob.getMimeType());
                        return;
                    }

                }
            }
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    /**
     * Check ignore subpath access.
     *
     * @param subPath
     * @return
     */
    private boolean ignoreSubpathAccess(String subPath) {
        return subPath != null && (subPath.endsWith(".png") || subPath.endsWith(".jpg"));
    }

    /**
     * Check a simple valid token.
     *
     * @param token
     * @param document
     * @return
     */
    private boolean validToken(String token, DocumentModel document) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String decodedToken = new String(Base64.decodeBase64(token));
        if (!decodedToken.endsWith(TOKEN_ENDCHARS_CONTROL)) {
            return false;
        }
        String changeToken = document.getChangeToken();
        if (changeToken == null) {
            return false;
        }
        String controlToken = String.format("%s%s", changeToken, TOKEN_ENDCHARS_CONTROL);
        LOG.info("Token " + decodedToken + ", " + controlToken);
        return controlToken.equals(decodedToken);

    }


    private List<Blob> initCachedBlob(Response res, String xpath,
            boolean blobPostProcessing) throws ClientException {

        HtmlPreviewAdapter preview = null; // getFromCache(targetDocument,
                                           // xpath);

        // if (preview == null) {
        preview = targetDocument.getAdapter(HtmlPreviewAdapter.class);
        // }

        if (preview == null) {
            handleNoPreview(res, xpath, null);
            return null;
        }

        List<Blob> previewBlobs = null;
        try {
            if (xpath.equals(PreviewHelper.PREVIEWURL_DEFAULTXPATH)) {
                previewBlobs = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                previewBlobs = preview.getFilePreviewBlobs(xpath,
                        blobPostProcessing);
            }
        } catch (PreviewException e) {
            previewInProcessing.remove(targetDocument.getId());
            handleNoPreview(res, xpath, e);
            return null;
        }

        if (previewBlobs == null || previewBlobs.size() == 0) {
            handleNoPreview(res, xpath, null);
            return null;
        }
        return previewBlobs;
    }

    protected void handleNoPreview(Response res, String xpath, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append(resourcesAccessor.getMessages().get(
                    "label.not.available.preview")
                    + "</h1>");
        } else {
            sb.append(resourcesAccessor.getMessages().get(
                    "label.cannot.generated.preview")
                    + "</h1>");
            sb.append("<pre>Technical issue:</pre>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        if (e instanceof NothingToPreviewException) {
            // Not an error, don't log
        } else {
            LOG.error("Could not build preview for missing blob at " + xpath, e);
        }

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

    protected void handlePreview(Response res, Blob previewBlob, String mimeType)
            throws IOException {
        final File tempfile = File.createTempFile("nuxeo-previewrestlet-tmp",
                "");
        Framework.trackFile(tempfile, res);
        previewBlob.transferTo(tempfile);
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                // the write call happens after the seam conversation is
                // finished which will garbage collect the CoreSession
                // instance, hence we store the blob content in a temporary
                // file
                FileInputStream instream = new FileInputStream(tempfile);
                FileUtils.copy(instream, outputStream);
                instream.close();
                tempfile.delete();
            }
        });
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
        response.setContentType(mimeType);
    }

}
