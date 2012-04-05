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
package org.xwiki.platform.search;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.script.service.ScriptService;

/**
 * Search script service.
 * 
 * @version $Id$
 */
@Component("search")
public class SearchScriptService implements ScriptService, Initializable
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * Component manager.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Search service.
     */
    private SearchService searchService;

    /**
     * Congiruation source to fetch xwiki properties.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * Unique id which can be used to identify different component instances.
     * @return id as a String.
     */
    public String getID()
    {
        return searchService.getBackend();
    }

    /**
     * Initialize the script service by retrieving the configured search backend. This can be tuned in the xwiki.cfg
     * file by defining a specific backend to be used (i.e., the hint of the SearchService component that should be
     * looked up)
     */
    @Override
    public void initialize() throws InitializationException
    {
        String backend = configuration.getProperty("searchBackend");

        try {
            if (backend == null) {
                searchService = componentManager.lookup(SearchService.class);
            } else {
                searchService = componentManager.lookup(SearchService.class, backend);
            }

            logger.info("Search service initialized using the {} backend", searchService.getBackend());
        } catch (Exception e) {
            throw new InitializationException("Unable to initialize the search script service", e);
        }

    }
}
