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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

import com.adobe.granite.crypto.CryptoSupport;
import com.adobe.granite.crypto.internal.CryptoSupportImpl;
import com.adobe.granite.crypto.internal.ProxyClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the AEM {@link CryptoSupport} implementation.
 * Requires some class loader tweaks due to nested JAR files.
 */
public class CryptoSupportFactory implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSupportFactory.class);
    /** 128 bit key (unobfuscated) */
    private static final int UNOBFUSCATED_KEY_LENGTH = 16;
    /** obfuscated keys are 256 bit long */
    private static final int OBFUSCATED_KEY_LENGTH = 32;

    private final Path tmpDirectory;
    private final ClassLoader classLoader;

    public CryptoSupportFactory(ClassLoader classLoader) throws IOException {
        this(
                classLoader,
                "META-INF/lib/cryptojce-6.0.0.jar",
                "META-INF/lib/cryptojcommon-6.0.0.jar",
                "META-INF/lib/jSafeCryptoSupport.jar",
                "META-INF/lib/jcmFIPS-6.0.0.jar");
    }

    public CryptoSupportFactory(ClassLoader classLoader, String... nestedJarResourceNames) throws IOException {
        tmpDirectory = Files.createTempDirectory("crypto-support-exported-jars");
        this.classLoader = CryptoSupportFactory.createClassLoader(tmpDirectory, classLoader, nestedJarResourceNames);
    }

    public CryptoSupport create(String base64EncodedKey)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
                    IllegalArgumentException, InstantiationException, InvocationTargetException {
        Class<?> cryptoImplClass = classLoader.loadClass("com.adobe.granite.crypto.internal.jsafe.JSafeCryptoSupport");
        Constructor<?> cryptoImplConstructor = cryptoImplClass.getDeclaredConstructor((Class[]) null);
        CryptoSupportImpl cryptoSupportImpl;
        cryptoSupportImpl = (CryptoSupportImpl) cryptoImplConstructor.newInstance((Object[]) null);
        Method initMethod = CryptoSupportImpl.class.getDeclaredMethod("init", byte[].class);
        initMethod.setAccessible(true);
        byte[] key = Base64.getDecoder().decode(base64EncodedKey);
        // newer versions of Crypto Support support both unobfuscated and obfuscated keys, but the version available on
        // Maven Central only the latter, therefore obfuscated key before passing to init method
        if (key.length == UNOBFUSCATED_KEY_LENGTH) {
            // obfuscate key
            Method obfuscateMethod = cryptoImplClass.getDeclaredMethod("obfuscate", byte[].class);
            obfuscateMethod.setAccessible(true);
            key = (byte[]) obfuscateMethod.invoke(null, key);
            LOGGER.debug("Obfuscating key ended up with key of length {}", key.length);
        } else if (key.length != OBFUSCATED_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Given key length must be either 128 bit (unobfuscated) or 256 bit (obfuscated) but is "
                            + key.length * 8 + " bit");
        }
        initMethod.invoke(cryptoSupportImpl, key);
        return cryptoSupportImpl;
    }

    private static ClassLoader createClassLoader(
            Path tmpDirectory, ClassLoader classLoader, String... nestedJarResourceNames) throws IOException {
        // expand the nested JARs into the temporary directory
        ArrayList<URL> classPathUrls = new ArrayList<>();
        for (String nestedJarResourceName : nestedJarResourceNames) {
            Path extractedJarFile = tmpDirectory.resolve(nestedJarResourceName);
            Files.createDirectories(extractedJarFile.getParent());
            try (InputStream input = classLoader.getResourceAsStream(nestedJarResourceName)) {
                Files.copy(input, extractedJarFile);
                classPathUrls.add(extractedJarFile.toUri().toURL());
            }
        }

        URL[] urls = classPathUrls.toArray(new URL[classPathUrls.size()]);
        return new URLClassLoader(urls, new ProxyClassLoader(classLoader));
    }

    @Override
    public void close() throws IOException {
        deleteDirectory(tmpDirectory);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
