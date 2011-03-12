package hotwiredbridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeyStoreProvider {
	private File certificatesFile;
	private String passphrase;

	public KeyStoreProvider(File certificatesFile, String passphrase) {
		this.certificatesFile = certificatesFile;
		this.passphrase = passphrase;
	}
	
	public KeyStore getKeyStore()
		throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		InputStream in = new FileInputStream(certificatesFile);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(in, passphrase.toCharArray());
		in.close();
		return ks;
	}
}
