/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.http.Context;
import org.forgerock.http.context.AbstractContext;
import org.forgerock.json.JsonValue;

/**
 * A {@link Context} from an internal source.
 */
public class InternalContext extends AbstractContext implements ClientContext {

    /**
     * Construct the internal context.
     *
     * @param parent
     *         The parent context.
     */
    public InternalContext(Context parent) {
        super(parent, "internalServer");
    }

    /**
     * Construct the internal context from a persisted state.
     *
     * @param savedContext
     *         The JSON representation from which this context's attributes
     *         should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     *
     * @throws ResourceException
     *         If the JSON representation could not be parsed.
     */
    public InternalContext(JsonValue savedContext, ClassLoader classLoader) throws ResourceException {
        super(savedContext, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExternal() {
        return false;
    }
}