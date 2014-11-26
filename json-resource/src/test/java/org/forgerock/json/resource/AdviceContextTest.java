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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit test for {@link org.forgerock.json.resource.AdviceContext}.
 *
 * @since 2.4.0
 */
public class AdviceContextTest {

    @Test
    public void deserializes() throws ResourceException {
        // Given
        RootContext root = new RootContext();
        AdviceContext advice = new AdviceContext(root, Collections.<String>emptyList());
        advice.putAdvice("Warning", "version_is_not_supported");
        ServerContext context = new ServerContext(advice);
        PersistenceConfig config = PersistenceConfig
                .builder()
                .connectionProvider(mock(ConnectionProvider.class))
                .build();

        JsonValue savedContext = context.toJsonValue();
        ServerContext restoredContext = new ServerContext(savedContext, config);
        AdviceContext restored = restoredContext.asContext(AdviceContext.class);

        // Then
        assertNotNull(restored.getAdvices());
        assertEquals(advice.getAdvices(), restored.getAdvices());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldNotAllowAdviceForRestrictedAdviceName() {

        //Given
        AdviceContext context = new AdviceContext(new RootContext(), Arrays.asList("Content-Type"));

        //When
        try {
            context.putAdvice("Content-Type", "VALUE");
        } catch (IllegalArgumentException e) {
            //Then
            assertThat(e.getMessage()).contains("restricted advice name");
            throw e;
        }
    }

    @DataProvider(name = "legalCharacters")
    private Object[][] legalCharacters() {
        Object[][] data = new Object[95][];
        for (int i = 0; i < 95; i++) {
            data[i] = new Object[]{(char) (i + 32)};
        }
        return data;
    }

    @Test(dataProvider = "legalCharacters")
    public void shouldAllowAdviceWithLegalCharacters(char character) {

        //Given
        AdviceContext context = new AdviceContext(new RootContext(), Collections.<String>emptyList());

        //When
        context.putAdvice("ADVICE_NAME", "" + character);

        //Then
        // All good
    }

    @DataProvider(name = "illegalCharacters")
    private Object[][] illegalCharacters() {
        Object[][] data = new Object[33][];
        for (int i = 0; i < 32; i++) {
            data[i] = new Object[]{(char) i};
        }
        data[32] = new Object[]{(char) 127};
        return data;
    }

    @Test(dataProvider = "illegalCharacters", expectedExceptions = IllegalArgumentException.class)
    public void shouldNotAllowAdviceWithIllegalCharacters(char character) {

        //Given
        AdviceContext context = new AdviceContext(new RootContext(), Collections.<String>emptyList());

        //When
        try {
            context.putAdvice("ADVICE_NAME", "" + character);
        } catch (IllegalArgumentException e) {
            //Then
            assertThat(e.getMessage()).contains("illegal characters");
            throw e;
        }
    }
}
