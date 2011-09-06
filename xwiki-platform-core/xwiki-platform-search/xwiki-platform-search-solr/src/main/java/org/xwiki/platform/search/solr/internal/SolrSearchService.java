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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.search.SearchService;

@Component
public class SolrSearchService implements SearchService, EventListener
{
    @Inject
    private Logger logger;

    private EmbeddedSolrServer solrServer;

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList((Event) new ApplicationStartedEvent());
    }

    @Override
    public String getName()
    {
        return "SOLR search service";
    }

    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        try {
            /* Initialize the SOLR backend using an embedded server */
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            CoreContainer container = initializer.initialize();
            solrServer = new EmbeddedSolrServer(container, "");

            logger.info("SOLR initialized");
        } catch (Exception e) {
            logger.error("Unable to initialize SOLR");
        }
    }

    @Override
    public String getBackend()
    {
        return "SOLR";
    }
}
