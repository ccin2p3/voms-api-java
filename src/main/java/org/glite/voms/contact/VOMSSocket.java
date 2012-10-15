/*********************************************************************
 *
 * Authors:
 *
 *      Andrea Ceccanti    - andrea.ceccanti@cnaf.infn.it
 *      Gidon Moont        - g.moont@imperial.ac.uk
 *      Vincenzo Ciaschini - vincenzo.ciaschini@cnaf.infn.it
 *
 * Copyright (c) Members of the EGEE Collaboration. 2004-2010.
 * See http://www.eu-egee.org/partners/ for details on the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Parts of this code may be based upon or even include verbatim pieces,
 * originally written by other people, in which case the original header
 * follows.
 *
 *********************************************************************/
package org.glite.voms.contact;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.italiangrid.voms.ac.impl.DefaultVOMSValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.emi.security.authn.x509.NamespaceCheckingMode;
import eu.emi.security.authn.x509.impl.KeyAndCertCredential;
import eu.emi.security.authn.x509.impl.OpensslCertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;

/**
 * The {@link VOMSSocket} class is used to manage the creation of the gsi socket
 * used for communication with the VOMS server.
 * 
 * @author Andrea Ceccanti
 * @author Vincenzo Ciaschini
 * 
 * 
 */
public class VOMSSocket {

	private static final Logger log = LoggerFactory.getLogger(VOMSSocket.class);

	UserCredentials cred;

	String hostDN;

	public static VOMSSocket instance(UserCredentials cred, String hostDN,
			int proxyType) {
		return new VOMSSocket(cred, hostDN, proxyType);

	}

	public static VOMSSocket instance(UserCredentials cred, String hostDN) {

		return new VOMSSocket(cred, hostDN, VOMSProxyBuilder.DEFAULT_PROXY_TYPE);

	}

	private VOMSSocket(UserCredentials cred, String hostDN, int proxyType) {

		this.cred = cred;
		this.hostDN = hostDN;
	}

	/**
	 * 
	 * Connects this socket to the voms server identified by the (host,port)
	 * passed as arguments.
	 * 
	 * @param defaultHost
	 * @param port
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * 
	 * @author Andrea Ceccanti
	 * @author Gidon Moont
	 * @author Vincenzo Ciaschini
	 */
	private SSLContext context = null;
	private SSLSocket socket = null;

	protected SSLSocketFactory getFactory() throws IOException,
			GeneralSecurityException {
		
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		
    context = SSLContext.getInstance("SSLv3");

    KeyAndCertCredential credential = new KeyAndCertCredential(cred.getUserKey(), cred.getUserChain());

    KeyManager[] keyManagers = new KeyManager[] {credential.getKeyManager()};

    OpensslCertChainValidator validator = new OpensslCertChainValidator(
        DefaultVOMSValidator.DEFAULT_TRUST_ANCHORS_DIR, NamespaceCheckingMode.EUGRIDPMA_AND_GLOBUS, 60000L);

    X509TrustManager trustManager = SocketFactoryCreator.getSSLTrustManager(validator);

    TrustManager[] trustManagers = new TrustManager[] {trustManager};

    SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");

    context.init(keyManagers, trustManagers, secureRandom);

    return context.getSocketFactory();
	}

	protected void connect(String host, int port) throws IOException,
			GeneralSecurityException {

		SSLSocketFactory socketFactory = null;

		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		log.debug("Initting CONNECCTION");
		try {
			socketFactory = getFactory();
			log.debug("Factory Created");
			log.debug(socketFactory.toString());
			log.debug("ABOUT to open CONNECTION");
			socket = (SSLSocket) socketFactory.createSocket(host, port);
			log.debug("CONNECTION OPEN");
			String[] protocols = { "SSLv3" };
			socket.setEnabledProtocols(protocols);
		} catch (SSLException e) {
			log.error("Error opening SSL socket: " + e.getMessage());

			if (log.isDebugEnabled())
				log.debug(e.getMessage(), e);
			throw e;
		} catch (IOException e) {

			log.error("Error opening SSL socket: " + e.getMessage());

			if (log.isDebugEnabled())
				log.debug(e.getMessage(), e);
			throw e;
		}

	}

	public void close() throws IOException {

		socket.close();
	}

	public SSLContext getContext() {

		return context;
	}

	public boolean isClosed() {

		return socket.isClosed();
	}

	public boolean isConnected() {

		return socket.isConnected();
	}

	public void shutdownInput() throws IOException {

		socket.shutdownInput();
	}

	public void shutdownOutput() throws IOException {

		socket.shutdownOutput();
	}

	public OutputStream getOutputStream() throws IOException {
		try {
			return socket.getOutputStream();
		} catch (IOException e) {

			log.error("Error getting output stream from underlying socket:"
					+ e.getMessage());
			if (log.isDebugEnabled())
				log.error(e.getMessage(), e);

			throw e;
		}
	}

	public InputStream getInputStream() throws IOException {

		try {

			return socket.getInputStream();

		} catch (IOException e) {
			log.error("Error getting input stream from underlying socket:"
					+ e.getMessage());
			if (log.isDebugEnabled())
				log.error(e.getMessage(), e);

			throw e;

		}
	}

}
