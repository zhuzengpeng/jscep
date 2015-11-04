# Introduction #

This page describes how to add jSCEP to your own application.

# Details #

## Dependencies ##

jSCEP uses the [Bouncy Castle](http://www.bouncycastle.org/) JCE Provider library ("bcprov") for:

  * Generating X.509 Certificates
  * Generating [PKCS #10](http://tools.ietf.org/html/rfc2314) Certificate Signing Requests
  * Generating and Parsing ASN.1 Data


This library can be downloaded from the [latest releases](http://www.bouncycastle.org/latest_releases.html) page on the Bouncy Castle web site.

## Using jSCEP ##

Using jSCEP is **easy**:

  * download the [the latest release](http://code.google.com/p/jscep/downloads/list?q=label:Featured%20type=Library) of jSCEP
  * add the jSCEP and bcprov.jar to your classpath

See SecurityManagerRequirements

## Logging ##

jSCEP uses the Java Logging API for logging and has quite extensive log messages.