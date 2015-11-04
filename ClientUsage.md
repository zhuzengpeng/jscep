## Creating a new Client ##

Firstly, you will need the URL of your SCEP server:

```
URL server = new URL("http://pki.example.org:8080/scep/pkiclient.exe");
```

Next, you will need an RSA private key and X509 certificate to represent your SCEP client.  The private key is used to sign SCEP messages.  The certificate is used by the server for verifying the signature on your SCEP messages, and for encrypting its responses.

```
PrivateKey priKey = ...;
X509Certificate cert = ...;
```

Next, you'll need a callback handler.  The handler is used for verifying the identity of the CA represented by the SCEP server.

```
CallbackHandler handler = ...;
```

We now have all we need to create a SCEP client:

```
Client client = new Client(server, cert, priKey, handler);
```

If your SCEP server supports multiple identities (or you're using Microsoft NDES), you can provide the name of the identity as a String:

```
String profile = "PublicCA";
```

You can then used the overloaded constructor to provide this profile name:

```
Client client = new Client(server, cert, priKey, handler, profile);
```

## Using the Client ##

The SCEP client supports three main operations:

  1. [Enrolment](#Enrolment.md)
  1. [Certificate Retrieval](#Certificate_Retrieval.md)
  1. [Certificate Revocation List Retrieval](#Certificate_Revocation_List_Retrieval.md)

Plus a further three [supporting operations](#Supporting_Operation.md):

  1. [CA Certificate Retrieval](#CA_Certificate_Retrieval.md)
  1. [Rollover CA Certificate Retrieval](#Rollover_CA_Certificate_Retrieval.md)
  1. [Listing Server Capabilities](#Listing_Server_Capabilities.md)

### Enrolment ###

Enrolment is the primary operation of the SCEP client, and is used for enrolling an entity in a PKI.  In order to invoke the operation, you will need to provide a [certificate signing request](CertificationRequest.md) (CSR):

```
CertificationRequest csr = ...;
EnrolmentTransaction txn = client.enrol(csr);
```

After calling `enrol()`, you will receive an `EnrolmentTransaction` object.  You should then call `send()` on this object, to send the actual request to the server:

```
State state = txn.send();
```

The State object is an enum which represents one of the following states:

  1. Success
  1. Failure
  1. Result Pending

**Insert graphic of state machine**

If the transaction was successful (`State.CERT_ISSUED`), the CSR was accepted, and a certificate has been returned.  To retrieve the certificate, call `getCertStore()` on the transaction, and retrieve the certificate:

```
CertStore store = txn.getCertStore();
Collection<? extends Certificate> certs = store.getCertificates(null);
```

If the transaction failed (`State.CERT_NON_EXISTANT`), the CSR was **not** accepted.  You can retrieve the failure reason by calling `getFailInfo()`:

```
FailInfo fail = txn.getFailInfo();
```

The last state -- pending (`State.CERT_REQ_PENDING`) -- means that the CA has not yet made a decision about the enrolment request, and that you should call `poll()` on the transaction object periodically to get an update:

```
state = txn.poll();
```

The state returned from `poll()` is the same as that returned from `send()`, so the steps outlined above should be followed until either a success or failure state is reached.

### Certificate Retrieval ###

The SCEP client can be used to retrieve a particular certificate issued by the SCEP CA by providing the serial number of the certificate:

```
try {
    Collection<X509Certificate> certs = client.getCertificate(BigInteger.TEN);
} catch (OperationFailureException e) {
    FailInfo fail = e.getFailInfo();
}
```

If the operation succeeds, the method returns a collection of certificates (which may include the CA certificate).  If the operation fails, e.g. if the serial number does not correspond to a certificate issued by the CA, the method will throw an `OperationFailureException`, which can be used to retrieve the reason for failure.

### Certificate Revocation List Retrieval ###

The SCEP client can be used to retrieve the latest CRL from the CA, if supported:

```
try {
    X509CRL crl = client.getRevocationList();
} catch (OperationFailureException e) {
    FailInfo fail = e.getFailInfo();
}
```

As with the certificate retrieval above, `getRecovationList()` will throw a `OperationFailureException` if the operation could not be completed successfully.  If the operation is successful, a collection of CRLs (which may be empty) will be returned.

### Supporting Operations ###

The following operations exist to support the main operations discussed above, and are can only be used for informational purposes.

#### CA Certificate Retrieval ####

The SCEP client can be used to retrieve the current CA certificate:

```
List<X509Certificate> caChain = client.getCaCertificate();
```

If the SCEP server is acting as an RA, the returned list will contain two certificates representing the RA and CA.  If the SCEP server is acting as a CA, the list will contain only one certificate.

#### Rollover CA Certificate Retrieval ####

The SCEP client can be used to retrieve the next certificate to be used by the CA.  This functionality is used by the client to determine which public key to use to encrypt SCEP messages for the CA.  If a CA certificate is coming to its end of life, the PKI administrator may choose to "queue" a new certificate to be used on some future date around the time when the current CA expires.  The SCEP client will use this method in conjunction with an inspection of the server capabilities to identify the CA certificate to use.

```
List<X509Certificate> caChain = getRolloverCertificate();
```

As with the [CA Certificate Retrieval](#CA_Certificate_Retrieval.md), the list may contain one or more certificates.



#### Listing Server Capabilities ####

The SCEP client can be used to retrieve a list of operations supported by the SCEP client:

```
Capabilities caps = client.getCaCapabilities();
```

The capabilities object is used by the SCEP client in the main operations to determine server support for security algorithms, enrolment scenarios and allowed transports:

```
// is HTTP POST supported for SCEP messages?
boolean usePost = caps.isPostSupported();

// does the server support rollover certificates?
boolean rollover = caps.isRolloverSupported();

// is certificate renewal supported?
boolean renewal = caps.isRenewalSupported();

// retrieve the strongest cipher (DES or Triple DES)
String cipher = caps.getStrongestCipher();

// retrieve the strongest digest (MD5, SHA-1, SHA-256 or SHA-512)
String digest = caps.getStrongestMessageDigest();
```