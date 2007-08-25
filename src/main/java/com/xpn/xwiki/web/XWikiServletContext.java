/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.web;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class XWikiServletContext implements XWikiEngineContext {
    private ServletContext scontext;

    public XWikiServletContext(ServletContext scontext) {
        this.scontext = scontext;
    }

    public ServletContext getServletContext() {
        return scontext;
    }

    public Object getAttribute(String name) {
        return scontext.getAttribute(name);
    }

    public void setAttribute(String name, Object value) {
        scontext.setAttribute(name, value);
    }

    public String getRealPath(String path) {
        return scontext.getRealPath(path);
    }

    public URL getResource(String name) throws MalformedURLException {
        return scontext.getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        return scontext.getResourceAsStream(name);
    }

    public String getMimeType(String filename) {
        return scontext.getMimeType(filename);
    }


}
