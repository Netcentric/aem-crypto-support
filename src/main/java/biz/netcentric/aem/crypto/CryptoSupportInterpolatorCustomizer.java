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
import java.util.function.Supplier;

import com.adobe.granite.crypto.CryptoSupport;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Enhances the regular FileVault resource filtering expressions with handling {@value #EXPRESSION_PREFIX}
 * prefixes which will automatically encrypt the interpolated value of
 * the suffix accordingly with the encryption from {@link CryptoSupport}. */
public class CryptoSupportInterpolatorCustomizer extends AbstractValueSource implements InterpolationPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSupportInterpolatorCustomizer.class);
    public static final String EXPRESSION_PREFIX = "vltaemencrypt.";

    private final CryptoSupportFactory cryptoSupportFactory;
    private final Supplier<String> keySupplier;
    private CryptoSupport cryptoSupport; // lazily initialized

    public CryptoSupportInterpolatorCustomizer(
            CryptoSupportFactory cryptoSupportFactory, Supplier<String> keySupplier) {
        super(false);
        this.cryptoSupportFactory = cryptoSupportFactory;
        this.keySupplier = keySupplier;
    }

    @Override
    public Object getValue(String expression) {
        if (expression.startsWith(EXPRESSION_PREFIX)) {
            // FIXME: currently the delimiter is hardcoded
            // (https://github.com/codehaus-plexus/plexus-interpolation/issues/76)
            return StringSearchInterpolator.DEFAULT_START_EXPR
                    + expression.substring(EXPRESSION_PREFIX.length())
                    + StringSearchInterpolator.DEFAULT_END_EXPR;
        } else {
            return null;
        }
    }

    @Override
    public Object execute(String expression, Object value) {
        if (expression.startsWith(EXPRESSION_PREFIX)) {
            try {
                return getCryptoSupport().protect(value.toString());
            } catch (Exception e) {
                throw new IllegalStateException("Can not encrypt value", e);
            }
        }
        return null;
    }

    private CryptoSupport getCryptoSupport()
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, InstantiationException {
        if (cryptoSupport == null) {
            cryptoSupport = cryptoSupportFactory.create(keySupplier.get());
            Thread factoryClose = new Thread(() -> {
                try {
                    cryptoSupportFactory.close();
                } catch (IOException e) {
                    LOGGER.error("Cannot close CryptoSupportFactory", e);
                }
            });
            Runtime.getRuntime().addShutdownHook(factoryClose);
        }
        return cryptoSupport;
    }
}
