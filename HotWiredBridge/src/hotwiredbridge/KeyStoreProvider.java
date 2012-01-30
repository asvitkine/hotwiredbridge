package hotwiredbridge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class KeyStoreProvider {
	private File certificatesFile;
	private byte[] cachedFileBytes;
	private String passphrase;

	public KeyStoreProvider(File certificatesFile, String passphrase) {
		this.certificatesFile = certificatesFile;
		this.passphrase = passphrase;
	}
	
	private void createKeyStoreIfNecessary()
		throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		if (!certificatesFile.exists()) {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, null);
			ks.store(new FileOutputStream(certificatesFile), passphrase.toCharArray());
			cachedFileBytes = null;
		}
	}
	
	public KeyStore getKeyStore()
		throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		createKeyStoreIfNecessary();
		if (cachedFileBytes == null) {
			cachedFileBytes = Utils.fileToByteArray(certificatesFile);
		}
		InputStream in = new ByteArrayInputStream(cachedFileBytes);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(in, passphrase.toCharArray());
		in.close();
		return ks;
	}

	public boolean addCertificatesForServer(String host, int port)
		throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException
	{
		boolean added = false;
		KeyStore ks = getKeyStore();
		SSLContext context = SSLContext.getInstance("TLS");
		String algorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(ks);

		X509TrustManager defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
		TrustManagerProxy tmProxy = new TrustManagerProxy(defaultTrustManager);
		context.init(null, new TrustManager[] { tmProxy }, null);

		boolean alreadyTrusted = attemptConnection(context, host, port);

		if (!alreadyTrusted) {
			X509Certificate[] chain = tmProxy.getCertificateChain();
			if (chain != null) {
				OutputStream out = new FileOutputStream(certificatesFile);
				for (int i = 0; i < chain.length; i++) {
					X509Certificate cert = chain[i];
					String alias = host + "-" + (i + 1);
					ks.setCertificateEntry(alias, cert);
					ks.store(out, passphrase.toCharArray());
					added = true;
					cachedFileBytes = null;
				}
				out.close();
			}
		}

		return added;
	}

	private boolean attemptConnection(SSLContext context, String host, int port)
		throws UnknownHostException, IOException
	{
		boolean success = true;
		SSLSocketFactory factory = context.getSocketFactory();

		SSLSocket socket = (SSLSocket)factory.createSocket(host, port);
		socket.setEnabledProtocols(new String[] { "TLSv1" });
		socket.setSoTimeout(10000);
		try {
			socket.startHandshake();
			socket.close();
		} catch (SSLException e) {
			success = false;
		}
		return success;
	}

	private static class TrustManagerProxy implements X509TrustManager {
		private final X509TrustManager tm;
		private X509Certificate[] chain;

		public TrustManagerProxy(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getCertificateChain() {
			return chain;
		}
		
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException
		{
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

}
