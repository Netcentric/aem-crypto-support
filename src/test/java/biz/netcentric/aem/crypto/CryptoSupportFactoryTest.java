package biz.netcentric.aem.crypto;

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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoSupportFactoryTest {

    @Test
    void testCreateCryptoSupport()
            throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException,
                    IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException,
                    CryptoException, NoSuchAlgorithmException {
        try (CryptoSupportFactory cryptoSupportFactory =
                new CryptoSupportFactory(this.getClass().getClassLoader())) {
            byte[] key = new byte[16]; // 128 bit random key
            SecureRandom.getInstanceStrong().nextBytes(key);
            CryptoSupport cryptoSupport =
                    cryptoSupportFactory.create(Base64.getEncoder().encodeToString(key));
            String encryptedValue = cryptoSupport.protect("secret value");
            assertEquals("secret value", cryptoSupport.unprotect(encryptedValue));
        }
    }
}
