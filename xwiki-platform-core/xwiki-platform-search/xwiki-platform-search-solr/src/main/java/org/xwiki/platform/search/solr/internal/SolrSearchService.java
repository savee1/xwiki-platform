/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.platform.search.solr.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.search.SearchService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * SOLR Search service component.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class SolrSearchService implements SearchService, EventListener
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * Execution.
     */
    @Inject
    private Execution execution;

    /**
     * Solr Server.
     */
    private static EmbeddedSolrServer solrServer;

    /**
     * Congiruation source to fetch xwiki properties.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList((Event) new ApplicationStartedEvent());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    @Override
    public String getName()
    {
        return "SOLR search service";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        try {
            logger.info("Starting embedded solr server by the event " + event.toString());
            /* Initialize the SOLR backend using an embedded server */
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            CoreContainer container = initializer.initialize();
            solrServer = new EmbeddedSolrServer(container, "");

            // Delete the existing index.
            solrServer.deleteByQuery("*:*");

            logger.info("SOLR initialized");
        } catch (Exception e) {
            logger.error("Unable to initialize SOLR :: " + e.getLocalizedMessage());

        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.platform.search.SearchService#getBackend()
     */
    @Override
    public String getBackend()
    {
        return "SOLR";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.platform.search.SearchService#indexDocuments()
     */
    @Override
    public void indexDocuments()
    {
        try {
            ExecutionContext context = execution.getContext();
            String hql = "select distinct doc.space, doc.name, doc.version, doc.language from XWikiDocument as doc ";

            XWikiContext xcontext = (XWikiContext) context.getProperty("xwikicontext");

            List<Object[]> documents = xcontext.getWiki().search(hql, xcontext);
            String wikiName = xcontext.getMainXWiki();
            String spaces = xcontext.getWiki().Param("xwiki.solr.allowed.spaces", "Sandbox");
            logger.info("Spaces configured for indexing :: " + spaces);
            for (Object[] document : documents) {

                String spaceName = (String) document[0];
                if (!spaces.contains(spaceName)) {
                    continue;
                }

                DocumentReference documentReference = new DocumentReference(wikiName, spaceName, (String) document[1]);
                String version = (String) document[2];
                String language = (String) document[3];

                XWikiDocument xdoc = xcontext.getWiki().getDocument(documentReference, xcontext);
                String languages = xcontext.getWiki().Param("xwiki.solr.allowed.languages", "en");
                logger.info("Languages configured for indexing :: " + languages);
                if (StringUtils.isEmpty(language)) {
                    language = xdoc.getDefaultLanguage();
                }

                if (!languages.contains(language)) {
                    continue;
                }

                this.addDocument(xcontext, xdoc, language);

            }

            UpdateResponse response = solrServer.commit();
            logger.info("All the files are indexed and committed - Response " + response.toString());

        } catch (Exception e) {
            logger.error("Error while indexing the wiki documents " + e.getMessage());
        }

    }

    /**
     * @param xcontext - XWiki Context.
     * @param xdoc - Reference to xwiki document.
     * @param language - the language of the document.
     */
    private void addDocument(XWikiContext xcontext, XWikiDocument xdoc, String language)
    {

        try {
            XWikiDocument tdoc = xdoc.getTranslatedDocument(language, xcontext);
            DocumentReference docRef = xdoc.getDocumentReference();

            SolrInputDocument sdoc = new SolrInputDocument();
            String lang = "_" + language;
            sdoc.addField(IndexFields.DOCUMENT_NAME + lang, docRef.getName());
            sdoc.addField(IndexFields.DOCUMENT_TITLE + lang, tdoc.getTitle());
            sdoc.addField(IndexFields.DOCUMENT_SPACE + lang, tdoc.getSpaceName());
            sdoc.addField(IndexFields.FULLTEXT + lang, tdoc.getContent());
            sdoc.addField("language", language);
            sdoc.addField("id", tdoc.getId());

            UpdateResponse response = solrServer.add(sdoc);
            logger.info("Document being added [" + tdoc.getTitle() + "] Language [" + language
                + "]  with Update response " + response.toString());
        } catch (Exception e) {
            logger.error("Error adding the document " + e.getMessage());
            // e.printStackTrace();
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.platform.search.SearchService#reIndexDocuments()
     */
    @Override
    public void reIndexDocuments()
    {
        // no op
        logger.warn("Dummy - no operation");

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.platform.search.SearchService#queryDocument(java.lang.String)
     */
    @Override
    public List<String> queryDocument(String query, String qfvalues)
    {
        SolrQuery squery = new SolrQuery();
        squery.setQuery(query).setFacet(true)
            .addFacetField("title_en", "title_fr", "title_es", "ft_en", "ft_fr", "ft_es");

        // Edismax
        squery.setParam("defType", "edismax");
        squery.setParam("qt", "edismax");

        // qf values passed on from front end.
        squery.setParam("qf", qfvalues);
        squery.setParam("debugQuery", "on");

        // Highlighting
        squery.setHighlight(true);
        squery.setParam("hl.simple.pre", "**");
        squery.setParam("hl.simple.post", "**");

        logger.info("Querying document - " + query);

        QueryResponse response;
        Map<String, Map<String, List<String>>> highlightingMap = null;
        try {

            response = solrServer.query(squery);
            logger.info("Facet fields " + response.getFacetFields());

            List<String> docNames = new ArrayList<String>();
            highlightingMap = response.getHighlighting();
            logger.info("Highlighting :: " + highlightingMap);

            for (SolrDocument d : response.getResults()) {
                String language = "_" + (String) d.getFieldValue("language");
                logger.info("The language for the document is - " + language);

                String space = (String) d.getFieldValue(IndexFields.DOCUMENT_SPACE + language);
                String pageName = (String) d.getFieldValue(IndexFields.DOCUMENT_NAME + language);
                String title = (String) d.getFieldValue(IndexFields.DOCUMENT_TITLE + language);
                Float score = (Float) d.getFieldValue("score");
                String id = (String) d.getFieldValue("id");

                if (highlightingMap != null) {
                    Map<String, List<String>> docMap = highlightingMap.get(id);
                    if (docMap != null && docMap.containsKey(IndexFields.DOCUMENT_TITLE + language)) {
                        title = docMap.get(IndexFields.DOCUMENT_TITLE + language).toString();
                    }
                }

                // return a string with link to the page, score and page title ( varies from language to langauge for
                // same page).
                String retStr = "[[" + space + "." + pageName + "]] with a score (" + score + ") \\\\" + title;

                logger.info("Fields :: " + retStr);
                docNames.add(retStr);
            }
            return docNames;
        } catch (SolrServerException e) {
            e.printStackTrace();
        }

        return Collections.EMPTY_LIST;

    }

}
