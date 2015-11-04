

This section shows how to create a SCEP client.

As a minimum, you need to specify the address of the server, the identity of the client, and a means of identifying the server certificate.  The following snippet how one would configure a client when the CA identity is already known.

```
final X509Certificate clientCertificate = ...;
final KeyPair clientKeyPair = ...;

final Client.Builder builder = new Client.Builder();
builder.url(new URL("http://www.example.org/scep/pkiclient.exe"));
builder.caFingerprint(new byte[] {-93, -44, 23, 25, -106, 116, 80, -113, 36, 23, 76, -89, -36, -18, 89, -59}, "MD5");
builder.identity(clientCertificate, clientKeyPair);
final Client client = builder.build();
```

The following sections will go into more details on the various requirements.

## Specifying the Address of the SCEP Server ##

Predictably, each client must know the URL used by the SCEP server application in order to correctly address requests.

```
builder.url(new URL("http://www.example.org/scep/pkiclient.exe"));
```

### Using a Proxy Server ###

If the machine on which the code is being executed does not have a direct route to your SCEP server, you can specify a proxy server to route requests through:

```
builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.org", 8080)));
```

### Technical Note on Validating URLs ###

Your URL must be a HTTP URL ending with `pkiclient.exe` with no query string, and no reference fragment.  That is, it must satisfy the following conditions:

```
url.getProtocol().matches("^https?$");
url.getPath().endsWith("pkiclient.exe");
url.getRef() == null
url.getQuery() == null;
```

## Specifying the Client Identity ##

Next, you have to provide a certificate and key pair to represent the SCEP client. In most cases, the certificate and key pair provided here will also be the certificate being enrolled with the SCEP server, although this is by no means necessary, and in the case of an RA implementation, will not be the case.

```
builder.identity(clientCertificate, clientKeyPair);
```

  1. If the requesting system already has a certificate issued by the SCEP server, and the server supports RENEWAL (see Appendix C), that certificate SHOULD be used.
  1. If the requesting system has no certificate issued by the new CA, but has credentials from an alternate CA the certificate issued by the alternate CA MAY be used.  Policy settings on the new CA will determine if the request can be accepted or not.  This is useful when enrolling with a new administrative domain; by using a certificate from the old domain as credentials.
  1. If the requester does not have an appropriate existing certificate, then a locally generated self-signed certificate MUST be used instead.  The self-signed certificate MUST use the same subject name as in the PKCS#10 request.

### Technical Note on Key Pairs ###

The key pair you provide **must** satisfy be an RSA key pair, satisfying the following conditions:

```
keyPair.getPrivate().getAlgorithm().equals("RSA");
keyPair.getPublic().getAlgorithm().equals("RSA");
```

## Identifying the Server Certificate ##

Next, you must provide a way for the SCEP client to validate the CA.  If you already know the identity of the CA certificate prior to runtime, you can provide the hash of the certificate and the algorithm name:

```
builder.caFingerprint(new byte[] {-93, -44, 23, 25, -106, 116, 80, -113, 36, 23, 76, -89, -36, -18, 89, -59}, "MD5");
```

Acceptable hash algorithms are limited to the following:

  * MD5
  * SHA-1
  * SHA-256
  * SHA-512

Alternatively, if you do NOT know the CA identity prior to runtime, you can configure the client to use a [CallbackHandler](http://java.sun.com/javase/6/docs/api/javax/security/auth/callback/CallbackHandler.html) to collect a confirmation response from the user (who can confirm the fingerprint out-of-band) using a GUI or some other means:

```
CallbackHandler handler = ...;

builder.callbackHandler(handler);
```

The CallbackHandler will be invoked with a instance of  [FingerprintVerificationCallback](http://jscep.googlecode.com/svn/trunk/documentation/com/google/code/jscep/FingerprintVerificationCallback.html).

### Technical Note on Calculating Digests ###

The hash of the CA certificate is calculated like so:

```
MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
byte[] digest = md.digest(ca.getEncoded());
```

## CA Identification ##

If your SCEP server provides multiple CA certificate profiles, it might be necessary to provide an identifying string.  This can be done like so:

```
builder.caIdentifier("myCA");
```

## Building ##

Once you've finished specifying arguments, invoking `build` on the builder creates a new instance of the client:

```
Client client = builder.build();
```

The constructor will throw an [IllegalStateException](http://java.sun.com/javase/6/docs/api/java/lang/IllegalStateException.html) if you fail to comply with the above rules.  If correctly configured, you can call the `build` method repeatedly to obtain new clients.