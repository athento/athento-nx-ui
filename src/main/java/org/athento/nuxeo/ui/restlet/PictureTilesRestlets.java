package org.athento.nuxeo.ui.restlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;
import org.nuxeo.ecm.platform.pictures.tiles.restlets.PictureTilesCachedEntry;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.JSONPictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.PictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.XMLPictureTilesSerializer;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.*;
import org.restlet.resource.OutputRepresentation;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Restlet to provide a REST API on top of the PictureTilingService.
 * 
 * @author tiry
 * @contributor athento
 */
//@Name("athentoTilesRestlet")
//@Scope(ScopeType.EVENT)
public class PictureTilesRestlets extends BaseStatelessNuxeoRestlet {

    private static final Log LOG = LogFactory.getLog(PictureTilesRestlets.class);

    // cache duration in seconds
    protected static int MAX_CACHE_LIFE = 60 * 10;

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    protected static Map<String, PictureTilesCachedEntry> cachedAdapters = new ConcurrentHashMap<String, PictureTilesCachedEntry>();

    @Override
    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repoId");
        String docid = (String) req.getAttributes().get("docId");
        Integer tileWidth = Integer.decode((String) req.getAttributes().get(
                "tileWidth"));
        Integer tileHeight = Integer.decode((String) req.getAttributes().get(
                "tileHeight"));
        Integer maxTiles = Integer.decode((String) req.getAttributes().get(
                "maxTiles"));

        Form form = req.getResourceRef().getQueryAsForm();
        String xpath = (String) form.getFirstValue("fieldPath");
        String x = form.getFirstValue("x");
        String y = form.getFirstValue("y");
        String format = form.getFirstValue("format");

        String test = form.getFirstValue("test");
        if (test != null) {
            try {
                handleSendTest(res, repo, docid, tileWidth, tileHeight,
                        maxTiles);
                return;
            } catch (IOException e) {
                handleError(res, e);
                return;
            }
        }

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
        } catch (ClientException e) {
            LOG.error("Unable to get document from session", e);
            handleError(res, e);
            return;
        } catch (LoginException e) {
            LOG.error("Login error in Athento templates", e);
            return;
        }

        Boolean init = initRepositoryAndTargetDocument(res, repo, docid);

        if (!init) {
            handleError(res, "unable to init repository connection");
            return;
        }

        PictureTilesAdapter adapter;
        try {
            adapter = getFromCache(targetDocument, xpath);
            if (adapter == null) {
                adapter = targetDocument.getAdapter(PictureTilesAdapter.class);
                if ((xpath != null) && (!"".equals(xpath))) {
                    adapter.setXPath(xpath);
                }
                updateCache(targetDocument, adapter, xpath);
            }
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        if (adapter == null) {
            handleNoTiles(res, null);
            return;
        }

        PictureTiles tiles = null;
        try {
            tiles = adapter.getTiles(tileWidth, tileHeight, maxTiles);
        } catch (ClientException e) {
            handleError(res, e);
        }

        if ((x == null) || (y == null)) {
            handleSendInfo(res, tiles, format);
        } else {
            handleSendImage(res, tiles, Integer.decode(x), Integer.decode(y));
        }
    }

    protected void handleSendTest(Response res, String repoId, String docId,
            Integer tileWidth, Integer tileHeight, Integer maxTiles)
            throws IOException {
        MediaType mt = null;
        mt = MediaType.TEXT_HTML;

        File file = FileUtils.getResourceFileFromContext("testTiling.html");
        String html = FileUtils.readFile(file);

        html = html.replace("$repoId$", repoId);
        html = html.replace("$docId$", docId);
        html = html.replace("$tileWidth$", tileWidth.toString());
        html = html.replace("$tileHeight$", tileHeight.toString());
        html = html.replace("$maxTiles$", maxTiles.toString());

        res.setEntity(html, mt);
    }

    protected void handleSendInfo(Response res, PictureTiles tiles,
            String format) {
        if (format == null) {
            format = "XML";
        }
        MediaType mt = null;
        PictureTilesSerializer serializer = null;

        if (format.equalsIgnoreCase("json")) {
            serializer = new JSONPictureTilesSerializer();
            mt = MediaType.APPLICATION_JSON;
        } else {
            serializer = new XMLPictureTilesSerializer();
            mt = MediaType.TEXT_XML;
        }

        res.setEntity(serializer.serialize(tiles), mt);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);

        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
    }

    protected void handleSendImage(Response res, PictureTiles tiles, Integer x,
            Integer y) {

        Blob image;
        try {
            image = tiles.getTile(x, y);
        } catch (Exception e) {
            handleError(res, e);
            return;
        }

        try {
            final File tempfile = File.createTempFile(
                    "nuxeo-tilingrestlet-tmp", "");
            image.transferTo(tempfile);
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
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    protected void handleNoTiles(Response res, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append("No Tiling is available for this document</h1>");
        } else {
            sb.append("Picture Tiling can not be generated for this document</h1>");
            sb.append("<br/><pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Content-Disposition", "inline");
    }

    protected void updateCache(DocumentModel doc, PictureTilesAdapter adapter,
                               String xpath) throws ClientException {

        Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
        PictureTilesCachedEntry entry = new PictureTilesCachedEntry(modified,
                adapter, xpath);
        synchronized (cachedAdapters) {
            cachedAdapters.put(doc.getId(), entry);
        }
        cacheGC();
    }

    protected void removeFromCache(String key) {
        PictureTilesCachedEntry entry = cachedAdapters.get(key);
        if (entry != null) {
            entry.getAdapter().cleanup();
        }
        synchronized (cachedAdapters) {
            cachedAdapters.remove(key);
        }
    }

    protected boolean isSameDate(Calendar d1, Calendar d2) {

        // because one of the date is stored in the repository
        // the date may be 'rounded'
        // so compare
        long t1 = d1.getTimeInMillis() / 1000;
        long t2 = d2.getTimeInMillis() / 1000;
        return Math.abs(t1 - t2) <= 1;
    }

    protected PictureTilesAdapter getFromCache(DocumentModel doc, String xpath)
            throws ClientException {
        if (cachedAdapters.containsKey(doc.getId())) {
            if (xpath == null) {
                xpath = "";
            }
            Calendar modified = (Calendar) doc.getProperty("dublincore",
                    "modified");
            PictureTilesCachedEntry entry = cachedAdapters.get(doc.getId());

            if ((!isSameDate(entry.getModified(), modified))
                    || (!xpath.equals(entry.getXpath()))) {
                removeFromCache(doc.getId());
                return null;
            } else {
                return entry.getAdapter();
            }
        } else {
            return null;
        }
    }

    protected void cacheGC() {
        for (String key : cachedAdapters.keySet()) {
            long now = System.currentTimeMillis();
            PictureTilesCachedEntry entry = cachedAdapters.get(key);
            if ((now - entry.getTimeStamp()) > MAX_CACHE_LIFE * 1000) {
            }
        }
    }

}
