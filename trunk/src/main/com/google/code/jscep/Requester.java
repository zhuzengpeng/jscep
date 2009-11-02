/*
 * Copyright (c) 2009 David Grant
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.google.code.jscep;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.x509.X509V3CertificateGenerator;

import com.google.code.jscep.content.ScepContentHandlerFactory;
import com.google.code.jscep.request.GetCACaps;
import com.google.code.jscep.request.GetCACert;
import com.google.code.jscep.request.GetCRL;
import com.google.code.jscep.request.GetCert;
import com.google.code.jscep.request.GetCertInitial;
import com.google.code.jscep.request.PkcsReq;
import com.google.code.jscep.request.PkiOperation;
import com.google.code.jscep.request.Request;
import com.google.code.jscep.response.Capabilities;
import com.google.code.jscep.transport.Transport;

public class Requester {
    static {
        URLConnection.setContentHandlerFactory(new ScepContentHandlerFactory());
    }

    private URL url;						// Required
    private Proxy proxy;					// Optional
    private String caIdentifier;			// Optional
    private KeyPair keyPair;				// Optional
    private X509Certificate identity;		// Optional
    
    private X509Certificate ca;

    private Requester(Builder builder) throws IllegalStateException {
    	// Must have only one way of obtaining an identity.
    	if (builder.identity == null && builder.subject == null) {
    		throw new IllegalStateException("Need Identity OR Subject");
    	}
    	if (builder.identity != null && builder.subject != null) {
    		throw new IllegalStateException("Need Identity OR Subject");
    	}
    	if (builder.identity != null && builder.keyPair == null) {
    		throw new IllegalStateException("Missing Key Pair for Identity");
    	}
    	
    	url = builder.url;
    	if (builder.proxy == null) {
    		proxy = Proxy.NO_PROXY;
    	} else {
    		proxy = builder.proxy;
    	}
    	if (builder.keyPair != null) {
    		keyPair = builder.keyPair;
    	} else {
    		keyPair = createKeyPair();		
    	}
    	
    	if (identity != null) {
    		identity = builder.identity;
    		// If we're replacing a certificate, we must have the same key pair.
    		if (identity.getPublicKey().equals(keyPair.getPublic()) == false) {
    			throw new IllegalStateException();
    		}
    	} else if (builder.subject != null) {
    		identity = createCertificate(builder.subject);
    	}
    	
    	caIdentifier = builder.identifier;
    }
    
    private void debug(String msg) {
    	System.out.println(msg);
    }
    
    private KeyPair createKeyPair() {
    	debug("Creating RSA Key Pair");
    	
    	try {
			return KeyPairGenerator.getInstance("RSA").genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
    }
    
    private X509Certificate createCertificate(X500Principal subject) {
    	debug("Creating Self-Signed Certificate for " + subject);
    	
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.DATE, -1);
    	Date notBefore = cal.getTime();
    	cal.add(Calendar.DATE, 2);
    	Date notAfter = cal.getTime();
    	X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
    	gen.setIssuerDN(subject);
    	gen.setNotBefore(notBefore);
    	gen.setNotAfter(notAfter);
    	gen.setPublicKey(keyPair.getPublic());
    	gen.setSerialNumber(BigInteger.ONE);
    	// TODO: Is this right?
    	gen.setSignatureAlgorithm("SHA1withRSA");
    	gen.setSubjectDN(subject);
    	try {
			return gen.generate(keyPair.getPrivate());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    private Transaction createTransaction() throws IOException {
    	return TransactionFactory.createTransaction(createTransport(), ca, identity, keyPair);
    }
    
    private Transport createTransport() throws IOException {
    	if (getCapabilities().supportsPost()) {
    		return Transport.createTransport("POST", url, proxy);
    	} else {
    		return Transport.createTransport("GET", url, proxy);
    	}
    }
    
    public KeyPair getKeyPair() {
    	return keyPair;
    }

    private Capabilities getCapabilities() throws IOException {
        Request req = new GetCACaps(caIdentifier);
        Transport trans = Transport.createTransport("GET", url, proxy);

        return (Capabilities) trans.sendMessage(req);
    }

    private X509Certificate[] getCaCertificate() throws IOException {
        Request req = new GetCACert(caIdentifier);
        Transport trans = Transport.createTransport("GET", url, proxy);
        
        return (X509Certificate[]) trans.sendMessage(req);
    }

    private void updateCertificates() throws IOException {
        X509Certificate[] certs = getCaCertificate();

        ca = certs[0];
    }

    public X509CRL getCrl() throws IOException, ScepException, GeneralSecurityException {
        updateCertificates();
        // PKI Operation
        PkiOperation req = new GetCRL(ca.getIssuerX500Principal(), ca.getSerialNumber());
        CertStore store = createTransaction().performOperation(req);
        
        List<X509CRL> crls = getCRLs(store.getCRLs(null));
        if (crls.size() > 0) {
        	return crls.get(0);
        } else {
        	return null;
        }
    }

    public X509Certificate enroll(char[] password) throws IOException, UnsupportedCallbackException, ScepException, GeneralSecurityException {
        updateCertificates();
        // PKI Operation
        PkiOperation req = new PkcsReq(keyPair, identity, password);
        CertStore store = createTransaction().performOperation(req);

        return getCertificates(store.getCertificates(null)).get(0);
    }

    public X509Certificate getCertInitial(X500Principal subject) throws IOException, ScepException, GeneralSecurityException {
        updateCertificates();
        // PKI Operation
        PkiOperation req = new GetCertInitial(ca.getIssuerX500Principal(), subject);
        CertStore store = createTransaction().performOperation(req);

        return getCertificates(store.getCertificates(null)).get(0);
    }

    public X509Certificate getCert(BigInteger serial) throws IOException, ScepException, GeneralSecurityException {
        updateCertificates();
        // PKI Operation
        PkiOperation req = new GetCert(ca.getIssuerX500Principal(), serial);
        CertStore store = createTransaction().performOperation(req);

        return getCertificates(store.getCertificates(null)).get(0);
    }
    
    private List<X509Certificate> getCertificates(Collection<? extends Certificate> certs) {
    	List<X509Certificate> x509 = new ArrayList<X509Certificate>();
    	
    	for (Certificate cert : certs) {
    		x509.add((X509Certificate) cert);
    	}
    	
    	return x509;
    }
    
    private List<X509CRL> getCRLs(Collection<? extends CRL> crls) {
    	List<X509CRL> x509 = new ArrayList<X509CRL>();
        
        for (CRL crl : crls) {
        	x509.add((X509CRL) crl);
        }
        
        return x509;
    }
    
    public static class Builder {
    	private URL url;
    	private Proxy proxy;
    	private String identifier;
    	private KeyPair keyPair;
    	private X509Certificate identity;
    	private X500Principal subject;
    	
    	public Builder(URL url) {
    		this.url = url;
    	}
    	
    	public Builder proxy(Proxy proxy) {
    		this.proxy = proxy;
    		
    		return this;
    	}
    	
    	public Builder identifier(String identifier) {
    		this.identifier = identifier;
    		
    		return this;
    	}
    	
    	public Builder keyPair(KeyPair keyPair) {
    		this.keyPair = keyPair;
    		
    		return this;
    	}
    	
    	public Builder subject(X500Principal subject) {
    		this.subject = subject;
    		
    		return this;
    	}
    	
    	public Builder identity(X509Certificate identity) {
    		this.identity = identity;
    		
    		return this;
    	}
    	
    	public Requester build() throws IllegalStateException {
    		return new Requester(this);
    	}
    }
}