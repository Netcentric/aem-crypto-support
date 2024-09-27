import java.util.zip.*
import groovy.xml.XmlSlurper

File file = new File( basedir, "target/package-plugin-test-pkg-1.0.0-SNAPSHOT.zip" );
try (ZipFile zipFile = new ZipFile(file)) {
    ZipEntry zipEntry = zipFile.getEntry("jcr_root/apps/foo/.content.xml")
    assert(zipEntry != null)
    try (InputStream input = zipFile.getInputStream(zipEntry)) {
        def root = new XmlSlurper().parse(input)
        assert(root.@escapedValue == "plainText")
        assert(root.@encryptedValue.text() ==~ $/\{[a-z0-9]*\}/$)
    }
}