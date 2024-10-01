# AEM Crypto Support Library

[![Build Status](https://img.shields.io/github/actions/workflow/status/Netcentric/aem-crypto-support/maven.yml?branch=main)](https://github.com/Netcentric/aem-crypto-support/actions)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.aem/aem-crypto-support)](https://search.maven.org/artifact/biz.netcentric.aem/aem-crypto-support)
[![SonarCloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-crypto-support&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-crypto-support)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-crypto-support&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-crypto-support)

## Overview

This wrapper library allows to encrypt/decrypt with [AEM's CryptoSupport][aem-cryptosupport] outside AEM. The library provided by Adobe
only works inside AEM/OSGi Runtimes. This wrapper adds some class loader tweaks and provides a simple API for constructing `CryptoSupport` objects.

The encryption algorithm used internally is symmetrical **AES encryption (AES/CBC/PKCS5Padding)** with a **128 bit** key. Since it uses [Cypher Block Chaining](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#CBC) a random initialisation vector is used to make the encrypted text always look different (for the same plaintext). It uses [BSAFE Crypto-J from RSA (now Dell)][bsafe-wikipedia] as implementation basis.

## Retrieve key from AEM environment

The easiest way to retrieve the (usually auto-generated random) key from an AEM server is to leverage the [Groovy Console][groovyconsole]. The key is usually stored on the file system (either below the bundle data directory or in a directory given through OSGi property/environment variable with name `com.adobe.granite.crypto.keys.path`). It can be exposed with the following Groovy script.

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

The master key (base-64 encoded) is looked up from either a Maven property with name `AEM_KEY` (or a same named environment variable) or `AEM_KEY_<SUFFIX>` in case a specific master key is referenced.

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
  encryptedValue="${vltattributeescape.vltaemencrypt.env.MY_SECRET}" />
```

This will encrypt the value provided through the environment variable `MY_SECRET` and afterwards [escape the encrypted value according to FileVault DocView rules][filevault-escape].

In order to use specific keys (e.g. when targeting multiple environments with different master keys in the same build) use a suffix after `vltaemencrypt` like `vltaemencryptprod.env.MY_SECRET`.
This will encrypt `MY_SECRET` with the master key provided in Maven property with name `AEM_KEY_PROD` or a same named environment variable (in that order). Note that the *suffix* (`prod` in this case) is automatically converted to uppercase letters before being used in the environment variable/property name.

### API 

```
try (CryptoSupportFactory cryptoSupportFactory = new CryptoSupportFactory(this.getClass().getClassLoader())) {
    CryptoSupport cryptoSupport = cryptoSupportFactory.create("your base64 encoded key);
    cryptoSupport.protect("my secret value");
    cryptoSupport.unprotect("my encrypted value");
}
```

## Use Cases

Several [AEM Cloud Service configurations][aem-cloudservice-configs] still access (encrypted) credentials from the repository (like [Dynamic Media configuration][dynamic-media-aem-config]) instead of leveraging interpolated OSGi configurations.
Those can be automatically configured via content packages with the help of this FileVault extension.

## Best Practices

* Never store store either master keys or to be encrypted values (in clear text) in any source code management system like Git. They should always be injected via some secure means as environment variables.
    * For CloudManager this is [secret pipeline variables][cloudmanager-pipelinevars], 
    * for GitHub Actions this is [secrets][gha-secrets],
    * Jenkins has a dedicated [Credentials API Plugin][jenkins-credentials-plugin] which is leveraged from several plugins.

* Preferably use secret values in OSGi configuration which have [native support for interpolation with secrets in AEMaaCS](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/deploying/configuring-osgi#when-to-use-secret-environment-specific-configuration-values) instead of encrypting and storing sensitive values within the repository.

* Don't overwrite/modify the default IMS configurations provided for integrations with other Adobe tools (like Adobe Analytics, Asset Compute, or Adobe Tags fka Adobe DTM).

Adobe, and AEM are either registered trademarks or trademarks of Adobe in
the United States and/or other countries.

[bsafe-wikipedia]: https://en.wikipedia.org/wiki/BSAFE
[aem-cryptosupport]: https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/granite/crypto/CryptoSupport.html
[groovyconsole]: https://github.com/orbinson/aem-groovy-console
[filevault-package-maven-plugin]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[filevault-filtering]: https://jackrabbit.apache.org/filevault-package-maven-plugin/filtering.html#Filtering_Extensions
[filevault-escape]: https://jackrabbit.apache.org/filevault/docview.html#Escaping]
[cloudmanager-pipelinevars]: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/using-cloud-manager/cicd-pipelines/pipeline-variables
[gha-secrets]: https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions
[jenkins-credentials-plugin]: https://github.com/jenkinsci/credentials-plugin/tree/master/docs
[dynamic-media-aem-config]: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/assets/dynamicmedia/config-dm#configuring-dynamic-media-cloud-services
[aem-cloudservice-configs]: https://experienceleague.adobe.com/en/docs/experience-manager-65/content/implementing/developing/extending-aem/extending-cloud-services/extending-cloud-config
