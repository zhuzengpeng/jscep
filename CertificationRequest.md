jscep 2.0 relies upon Bouncy Castle 1.47. The PKCS10CertificationRequestBuild interface supports the challenge password attribute.
```
KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
X500Principal entityName = new X500Principal("CN=localhost.localdomain");
X509Certificate entity = X509Util.createEphemeralCertificate(entityName, keyPair);

JcaContentSignerBuilder csb = new JcaContentSignerBuilder("SHA1withRSA");
ContentSigner cs = csb.build(keyPair.getPrivate());

PKCS10CertificationRequestBuilder crb = new JcaPKCS10CertificationRequestBuilder(entity, keyPair.getPublic());

DERPrintableString password = new DERPrintableString("secret");
crb.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_challengePassword, password);
PKCS10CertificationRequest csr = crb.build(cs);
```

Older releases of Bouncy Castle (and hence older relases of jscep) require direct use of Bouncy Castle ASN.1 objects.
```
String signatureAlgorithm = "SHA1withRSA";
X500Principal subject = new X500Principal("CN=alpha.jscep.org");
PublicKey key = ...;

DERObjectIdentifier attrType = PKCSObjectIdentifiers.pkcs_9_at_challengePassword;
ASN1Set attrValues = new DERSet(new DERPrintableString("challenge"));
DEREncodable password = new Attribute(attrType, attrValues);
ASN1Set attributes = new DERSet(password);

PrivateKey signingKey = ...;

PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureAlgorithm, subject, key, attributes, signingKey);
```