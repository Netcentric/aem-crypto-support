# AEM Crypto Support Library

## Overview

This wrapper library allows to encrypt/decrypt with [AEM's CryptoSupport][aem-cryptosupport] outside AEM. The library provided by Adobe
only works inside AEM/OSGi Runtimes. This wrapper adds some class loader tweaks and provides a simple API for constructing `CryptoSupport` objects.

The encryption algorithm used internally is symmetrical **AES encryption (AES/CBC/PKCS5Padding)** with a **128 bit** key. Since it uses [Cypher Block Chaining](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#CBC) a random initialisation vector is used to make the encrypted text always look different (for the same plaintext). It uses [BSAFE Crypto-J from RSA (now Dell)][bsafe-wikipedia] as implementation basis.

## Retrieve key from AEM environment

The easiest way to retrieve the (usually auto-generated random) key from an AEM server is to leverage the [Groovy Console][groovyconsole]. The key is usually stored on the file system (either below the bundle data directory or in a directory given through OSGi property/environment variable with name `com.adobe.granite.crypto.keys.path`. It can be exposed with the following Groovy script.

```
org.osgi.framework.Bundle bundle = Arrays.asList(bundleContext.getBundles()).find { "com.adobe.granite.crypto.file".equals(it.getSymbolicName()) };

out.println("Bundle " + bundle);
out.println("Data File " + bundle.getBundleContext().getDataFile("master"));
String keyPath = bundle.getBundleContext().getProperty("com.adobe.granite.crypto.keys.path");
out.println("Keys Path " + keyPath );
File masterFile;
if (keyPath != null) {
    masterFile = new File(keyPath, "master");
} else {
    masterFile = bundle.getDataFile("master")
}
byte [] bytes = java.nio.file.Files.readAllBytes(masterFile.toPath());
out.println("master key in base64 encoding:\n" + Base64.getEncoder().encodeToString(bytes));
```

## Usage

This library can be used as extension for the filevault-package-maven plugin or programmatically via API.

### Extension for filevault-package-maven-plugin

This library can be used with [`filevault-package-maven-plugin`][filevault-package-maven-plugin] in version 1.4.0 or newer to allow [resource filtering][filevault-filtering] with encryption support. This is useful to create encrypted values in content packages.

The key is looked up from either a Maven property with name `AEM_KEY` or a same named environment variable (in that order).

#### Configuration of filevault-package-maven-plugin

```
<plugin>
  <groupId></groupId>
  <artifactId></artifactId>
  <version>1.4.0</version> <!-- this is the minimum version required -->
  <dependencies>
    <dependency>
      <groupId>biz.netcentric.aem</groupId>
      <artifactId>crypto-support</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
  <configuration>
    <enableJcrRootFiltering>true</enableJcrRootFiltering><!-- enable filtering -->
  </configuration>
</plugin>
```

#### Usage in FileVault DocView files

```
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
  jcr:primaryType="nt:unstructured"
  ...
  encryptedValue="${vltaemencrypt.env.MY_SECRET}" />
```

This will encrypt the value provided through the environment variable `MY_SECRET`.

### API 

```
try (CryptoSupportFactory cryptoSupportFactory = new CryptoSupportFactory(this.getClass().getClassLoader())) {
    CryptoSupport cryptoSupport = cryptoSupportFactory.create("your base64 encoded key);
    cryptoSupport.protect("my secret value");
    cryptoSupport.unprotect("my encrypted value");
}
```

Adobe, and AEM are either registered trademarks or trademarks of Adobe in
the United States and/or other countries.

[bsafe-wikipedia]: https://en.wikipedia.org/wiki/BSAFE
[aem-cryptosupport]: https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/granite/crypto/CryptoSupport.html
[groovyconsole]: https://github.com/orbinson/aem-groovy-console
[filevault-package-maven-plugin]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[filevault-filtering]: https://jackrabbit.apache.org/filevault-package-maven-plugin/filtering.html