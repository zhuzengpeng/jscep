This section provides information on using jSCEP to perform **transactional** operations.

### Certificate Enrollment ###

#### Manual Enrollment ####

Manual enrollment occurs when the client submits a request and does not receive an issued certificate immediately.  It becomes necessary for the client to enter into polling mode to keep checking back with the server to see if the certificate has been issued yet.

The following code shows how to submit an enrollment request, and then poll the server hourly (blocking the thread) to check the status.

```
// Locate the certificate
X509Certificate identity = ...;
// ...and the key pair
KeyPair keyPair = ...;
// Locate the SCEP server
URL url = new URL("http://www.example.org/scep/pkiclient.exe");
// Specify the CA digest
byte[] digest = getDigest();

// Create our new client
Client.Builder builder = new Client.Builder();
builder.url(url);
builder.caFingerprint(digest, "MD5");
builder.identity(identity, keyPair);
Client client = builder.build();

Transaction trans = client.createTransaction();
// Send the enrollment request using our secret password
State state = trans.enrollCertificate(identity, keyPair, "password".toCharArray()); 
// Check for completion
while (state == State.CERT_REQ_PENDING) {
    // If we're manually enrolling, wait for an hour and try again.
    Callable<State> task = trans.getTask();
    state = getScheduledExecutorService().schedule(task, 1L, TimeUnit.HOURS).get();
}
if (state == State.CERT_ISSUED) {
    CertStore store = trans.getCertStore();
} else if (state == State.CERT_NON_EXISTANT) {
    FailInfo fail = trans.getFailureReason();
} else {
    // Unexpected
}
```

#### Automatic Enrollment ####

If your SCEP server automatically enrolls certificate requests into the PKI, it is likely that the issued certificate will be returned immediately.  If your application depends on this, you might choose to report an error if the result comes back as pending.

```
// Locate the existing certificate
X509Certificate identity = ...;
// ...and the key pair
KeyPair keyPair = ...;
// Locate the SCEP server
URL url = new URL("http://www.example.org/scep/pkiclient.exe");
// Specify the CA digest
byte[] digest = getDigest();

// Create our new client
Client.Builder builder = new Client.Builder();
builder.url(url);
builder.caFingerprint(digest, "MD5");
builder.identity(identity, keyPair);
Client client = builder.build();

Transaction trans = client.createTransaction();
// Send the enrollment request using our secret password
State state = trans.enrollCertificate(identity, keyPair, "password".toCharArray()); 
// Check for completion
if (state == State.CERT_ISSUED) {
    CertStore store = trans.getCertStore();
} else if (state == State.CERT_NON_EXISTANT) {
   FailInfo fail = trans.getFailureReason();
} else {
   // We don't want to wait around...
}
```

#### Renewing an Existing Certificate ####

If your CA [supports renewal](InformationRequests#Determining_Server_Capabilities.md), you can request the renewal an existing certificate.  This example assumes automatic enrollment.

```
// Locate the existing certificate
X509Certificate existing = ...;
KeyPair existingKeyPair = ...;
// Locate the self-signed identity certificate
X509Certificate identity = ...;
// ...and the key pair
KeyPair keyPair = ...;
// Locate the SCEP server
URL url = new URL("http://www.example.org/scep/pkiclient.exe");
// Specify the CA digest
byte[] digest = getDigest();

// Create our new client
Client.Builder builder = new Client.Builder();
builder.url(url);
builder.caFingerprint(digest, "MD5");
builder.identity(identity, keyPair);
Client client = builder.build();

Transaction trans = client.createTransaction();
// Send the enrollment request using our secret password
State state;
if (client.getCaCapabilities().isRenewalSupported()) {
    // Renewal is supported
    state = trans.enrollCertificate(existing, existingKeyPair, "password".toCharArray()); 
} else {
    // Renewal isn't supported, so use a self-signed certificate
    state = trans.enrollCertificate(identity, keyPair, "password".toCharArray()); 
}
// Check for completion
if (state == State.CERT_ISSUED) {
    CertStore store = trans.getCertStore();
} else if (state == State.CERT_NON_EXISTANT) {
   FailInfo fail = trans.getFailureReason();
} else {
   // We don't want to wait around...
}
```

#### Rollover ####

**Using a roll-over certificate should be handled automatically.**

http://tools.ietf.org/html/draft-nourse-scep-20#appendix-E

### Certificate Access ###

### CRL Access ###

**What about distribution points?**