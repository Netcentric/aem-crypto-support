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

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.adobe.granite.crypto.CryptoSupport;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

/** Enhances the regular FileVault resource filtering expressions with handling {@value #EXPRESSION_PREFIX}
 * prefixes which will automatically encrypt the interpolated value of
 * the suffix accordingly with the encryption from {@link CryptoSupport}.
 * It supports encryption with a default master key or a custom key with a given id.
 * Custom keys require an additional infix in the expression, e.g. {@value #EXPRESSION_PREFIX}customKeyId.
 */
public class CryptoSupportInterpolatorCustomizer extends AbstractValueSource implements InterpolationPostProcessor {

    public static final String EXPRESSION_PREFIX = "vltaemencrypt";

    static final String DEFAULT_KEY_ID = "default";
    private final CryptoSupportFactory cryptoSupportFactory;
    private final UnaryOperator<String> keyProvider;
    private Map<String, CryptoSupport> cryptoSupportMap; // lazily initialized

    public CryptoSupportInterpolatorCustomizer(
            CryptoSupportFactory cryptoSupportFactory, UnaryOperator<String> keyProvider) {
        super(false);
        this.cryptoSupportFactory = cryptoSupportFactory;
        this.keyProvider = keyProvider;
        this.cryptoSupportMap = new HashMap<>();
    }

    @Override
    public Object getValue(String expression) {
        // FIXME: currently the delimiter is hardcoded
        // (https://github.com/codehaus-plexus/plexus-interpolation/issues/76)
        return extractKeyIdAndSuffix(expression)
                .map(entry -> StringSearchInterpolator.DEFAULT_START_EXPR
                        + entry.getValue()
                        + StringSearchInterpolator.DEFAULT_END_EXPR)
                .orElse(null);
    }

    static Optional<Map.Entry<String, String>> extractKeyIdAndSuffix(String expression) {
        if (!expression.startsWith(EXPRESSION_PREFIX)) {
            return Optional.empty();
        }
        String suffix = expression.substring(EXPRESSION_PREFIX.length());
        int posDelimiter = suffix.indexOf('.');
        final Map.Entry<String, String> result;
        if (posDelimiter > 0 && posDelimiter < suffix.length() - 1) {
            result = new AbstractMap.SimpleEntry<>(
                    suffix.substring(0, posDelimiter), suffix.substring(posDelimiter + 1));
        } else if (posDelimiter == 0 && posDelimiter < suffix.length() - 1) {
            result = new AbstractMap.SimpleEntry<>(DEFAULT_KEY_ID, suffix.substring(1));
        } else {
            result = null;
        }
        return Optional.ofNullable(result);
    }

    @Override
    public Object execute(String expression, Object value) {
        return extractKeyIdAndSuffix(expression)
                .map(entry -> {
                    try {
                        return getCryptoSupport(entry.getKey()).protect(value.toString());
                    } catch (Exception e) {
                        throw new IllegalStateException("Can not encrypt value", e);
                    }
                })
                .orElse(null);
    }

    private CryptoSupport getCryptoSupport(String keyId) {
        return cryptoSupportMap.computeIfAbsent(keyId, k -> {
            try {
                return cryptoSupportFactory.create(keyProvider.apply(k));
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | SecurityException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InstantiationException
                    | InvocationTargetException e) {
                throw new IllegalStateException("Could not initialize CryptoSupport", e);
            }
        });
    }
}
