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

import javax.inject.Named;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.jackrabbit.filevault.maven.packaging.InterpolatorCustomizerFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.Interpolator;

/** Registers {@link CryptoSupportInterpolatorCustomizer} for the given Maven session and project.
 * The key is retrieved from Maven property or environment variable named {@value #PROPERTY_NAME_KEY}. */
@Named
public class CryptoSupportInterpolatorCustomizerFactory implements InterpolatorCustomizerFactory {

    private static final String PROPERTY_NAME_KEY = "AEM_KEY";
    private final CryptoSupportFactory cryptoSupportFactory;

    protected CryptoSupportInterpolatorCustomizerFactory() {
        try {
            cryptoSupportFactory = new CryptoSupportFactory(this.getClass().getClassLoader());
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize CryptoSupportFactory", e);
        }
    }

    @Override
    public Consumer<Interpolator> create(MavenSession mavenSession, MavenProject mavenProject) {
        Supplier<String> keySupplier = () -> {
            String key = mavenProject.getProperties().getProperty(PROPERTY_NAME_KEY, System.getenv(PROPERTY_NAME_KEY));
            if (key == null) {
                throw new IllegalStateException(
                        "Could not find key in either Maven property or environment variable " + PROPERTY_NAME_KEY);
            }
            return key;
        };
        CryptoSupportInterpolatorCustomizer customizer =
                new CryptoSupportInterpolatorCustomizer(cryptoSupportFactory, keySupplier);
        return i -> {
            i.addPostProcessor(customizer);
            i.addValueSource(customizer);
        };
    }
}
