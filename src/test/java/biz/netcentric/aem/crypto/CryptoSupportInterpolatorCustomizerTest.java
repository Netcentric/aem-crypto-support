package biz.netcentric.aem.crypto;

import java.util.AbstractMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*-
 * #%L
 * aem-crypto-support
 * %%
 * Copyright (C) 2024 Cognizant Netcentric
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */

class CryptoSupportInterpolatorCustomizerTest {

    @Test
    void testExtractKeyIdAndSuffix() {
        assertEquals(Optional.empty(), CryptoSupportInterpolatorCustomizer.extractKeyIdAndSuffix("other.key"));
        assertEquals(
                Optional.of(new AbstractMap.SimpleEntry<String, String>("default", "test")),
                CryptoSupportInterpolatorCustomizer.extractKeyIdAndSuffix("vltaemencrypt.test"));
        assertEquals(
                Optional.of(new AbstractMap.SimpleEntry<String, String>("id1", "test")),
                CryptoSupportInterpolatorCustomizer.extractKeyIdAndSuffix("vltaemencryptid1.test"));
        assertEquals(Optional.empty(), CryptoSupportInterpolatorCustomizer.extractKeyIdAndSuffix("vltaemencryptid1."));
        assertEquals(Optional.empty(), CryptoSupportInterpolatorCustomizer.extractKeyIdAndSuffix("vltaemencrypt."));
    }
}
