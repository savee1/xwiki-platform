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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Execute the macro's content in the context of another document's reference.
 * 
 * @version $Id$
 * @since 3.0M1
 */
@Component
@Named("context")
@Singleton
public class ContextMacro extends AbstractMacro<ContextMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Executes content in the context of the passed document";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "The content to execute";

    /**
     * Used to set the current document in the context (old way) and check rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The parser used to parse macro content.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Used to transform document links into absolute references.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ContextMacro()
    {
        super("Context", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), ContextMacroParameters.class);

        // The Context macro must execute early since it can contain include macros which can bring stuff like headings
        // for other macros (TOC macro, etc). Make it the same priority as the Include macro.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(ContextMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException("You must specify a 'document' parameter pointing to the document to "
                + "set in the context as the current document.");
        }

        DocumentReference docReference = this.currentDocumentReferenceResolver.resolve(parameters.getDocument());

        boolean currentContextHasProgrammingRights = this.documentAccessBridge.hasProgrammingRights();

        List<Block> result;
        try {
            Map<String, Object> backupObjects = new HashMap<String, Object>();
            try {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, docReference);

                // The current document is now the passed document. Check for programming rights for it. If it has
                // programming rights then the initial current document also needs programming right, else throw an
                // error since it would be a security breach otherwise.
                if (this.documentAccessBridge.hasProgrammingRights() && !currentContextHasProgrammingRights) {
                    throw new MacroExecutionException("Current document must have programming rights since the "
                        + "context document provided [" + parameters.getDocument() + "] has programming rights.");
                }

                result = this.contentParser.parse(content, context, true, false).getChildren();

            } finally {
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
            }
        } catch (Exception e) {
            if (e instanceof MacroExecutionException) {
                throw (MacroExecutionException) e;
            } else {
                throw new MacroExecutionException("Failed to render page in the context of [" + docReference + "]", e);
            }
        }

        // Step 4: Wrap Blocks in a MetaDataBlock with the "source" metadata specified so that potential relative
        // links/images are resolved correctly at render time.
        result = Arrays.asList((Block) new MetaDataBlock(result, MetaData.SOURCE, parameters.getDocument()));

        return result;
    }
}
