Writing a [CallbackHandler](http://download.oracle.com/javase/6/docs/api/javax/security/auth/callback/CallbackHandler.html) for **jscep** will depend on the environment under which you are running.  The basic code will look like so:

```
public class MyCallbackHandler implements CallbackHandler {
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        // Loop through all the callbacks.  There will only ever be one.
        for (int i = 0; i < callbacks.length; i++) {
            // Check that the callback is a CertificateVerificationCallback
            if (callbacks[i] instanceof CertificateVerificationCallback) {
                // Cast the callback
                CertificateVerificationCallback callback = (CertificateVerificationCallback) callbacks[i];
                // Check the certificate
                callback.setVerified(verify(callback.getCertificate());
            } else {
                // If we don't know the type of callback, throw an exception
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }

    public boolean verify(X509Certificate cert) {
        return true;
    }
}
```

In the above snippet, the `verify()` method always returns true, which means that we don't really care what the certificate is.  This is almost never the correct thing to do!

Your application must either be provisioned with the identity of the certificate, or it must prompt the user to make a decision.  This decision-making process might be made through a Swing dialog, or through a [Console](http://download.oracle.com/javase/6/docs/api/java/io/Console.html) prompt.