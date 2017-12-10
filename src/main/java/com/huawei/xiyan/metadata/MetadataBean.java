package com.huawei.xiyan.metadata;

import com.huawei.xiyan.Constants.MetadataConstants;
import com.huawei.xiyan.util.OpenSamlUtil;
import com.huawei.xiyan.util.PropertityUtil;
import org.opensaml.saml2.metadata.*;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.opensaml.xml.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhouyibin on 2017/12/11.
 */
public class MetadataBean {

    private EntityDescriptor entityDescriptor;
    private String entityId;
    private SSODescriptor ssoDescriptor;
    private List<BasicX509Credential> signingCredList;
    private List<BasicX509Credential> encryptionCredList;
    private List<NameIDFormat> nameIDFormatList;
    private List<Endpoint> endpointList;

    public MetadataBean(EntityDescriptor entityDescriptor){
        this.entityDescriptor = entityDescriptor;
        init();
    }

    private void init() {

        this.entityId = entityDescriptor.getEntityID();
        this.ssoDescriptor = entityDescriptor.getSPSSODescriptor(MetadataConstants.SAML2_SUPPORTED_PROTOCOL);
        if (this.ssoDescriptor instanceof SPSSODescriptor){
            fillSPSsoDescriptorByFile();
        }
        else {
            fillIDPSsoDescriptorByDefine();
        }
    }

    private void fillSPSsoDescriptorByFile() {
        this.signingCredList = getSPCredList(UsageType.SIGNING);
        this.encryptionCredList = getSPCredList(UsageType.ENCRYPTION);
        this.nameIDFormatList = ssoDescriptor.getNameIDFormats();
        this.endpointList = ssoDescriptor.getEndpoints();
    }

    private void fillIDPSsoDescriptorByDefine() {

        this.entityId = "https://" + PropertityUtil.getProperty("idp.domain.name") + "/";
        this.entityDescriptor.setEntityID(entityId);

        this.ssoDescriptor = entityDescriptor.getIDPSSODescriptor(MetadataConstants.SAML2_SUPPORTED_PROTOCOL);

        this.signingCredList = getIDPCredList();
        if(signingCredList != null && signingCredList.size() > 0){
            java.security.cert.X509Certificate x509Certificate = signingCredList.get(0).getEntityCertificate();
            KeyDescriptor keyDescriptor = fillKeyDescriptor(x509Certificate, UsageType.SIGNING);
            this.ssoDescriptor.getKeyDescriptors().add(keyDescriptor);
        }

        this.encryptionCredList = getIDPCredList();
        if(encryptionCredList != null && encryptionCredList.size() > 0){
            java.security.cert.X509Certificate x509Certificate = signingCredList.get(0).getEntityCertificate();
            KeyDescriptor keyDescriptor = fillKeyDescriptor(x509Certificate, UsageType.ENCRYPTION);
            this.ssoDescriptor.getKeyDescriptors().add(keyDescriptor);
        }

        List<NameIDFormat> nameIDFormatList = new ArrayList<NameIDFormat>();
        NameIDFormat nameIDFormat = OpenSamlUtil.buildSAMLObject(NameIDFormat.class);
        nameIDFormat.setFormat(MetadataConstants.TRANSIENT_NAME_ID_FORMAT);
        nameIDFormatList.add(nameIDFormat);
        this.nameIDFormatList = nameIDFormatList;
        this.ssoDescriptor.getNameIDFormats().addAll(nameIDFormatList);

        IDPSSODescriptor idpssoDescriptor = (IDPSSODescriptor)this.ssoDescriptor;
        idpssoDescriptor.getSingleSignOnServices().addAll(EndpointGenerator.getSingleSignOnServices());
        idpssoDescriptor.getArtifactResolutionServices().addAll(EndpointGenerator.getArtifactResolutionServices());

    }

    private KeyDescriptor fillKeyDescriptor(java.security.cert.X509Certificate certificate, UsageType usageType){

        X509Certificate x509Certificate = OpenSamlUtil.buildSAMLObject(X509Certificate.class);
        String value = null;
        try {
            value = Base64.encodeBytes(certificate.getEncoded()).toString();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        x509Certificate.setValue(value);

        X509Data x509Data = OpenSamlUtil.buildSAMLObject(X509Data.class);
        x509Data.getX509Certificates().add(x509Certificate);

        KeyInfo keyInfo = OpenSamlUtil.buildSAMLObject(KeyInfo.class);
        keyInfo.getX509Datas().add(x509Data);

        KeyDescriptor keyDescriptor = OpenSamlUtil.buildSAMLObject(KeyDescriptor.class);
        keyDescriptor.setUse(usageType);
        keyDescriptor.setKeyInfo(keyInfo);

        return keyDescriptor;
    }

    public String getEntityId(){
        return this.entityId;
    }

    public EntityDescriptor getEntityDescriptor(){
        return this.entityDescriptor;
    }

    public List<BasicX509Credential> getSigningCredList(){
        return this.signingCredList;
    }

    public List<BasicX509Credential> getEncryptionCredList(){
        return this.encryptionCredList;
    }

    public List<NameIDFormat> getNameIDFormatList(){
        return this.nameIDFormatList;
    }

    public List<Endpoint> getEndpointList(){
        return this.endpointList;
    }

    private KeyStore getKeyStore() {

        String fileName = PropertityUtil.getProperty("idp.keystore.file");
        String password = PropertityUtil.getProperty("idp.keystore.password");

        String filePath = MetadataBean.class.getClassLoader().getResource(fileName).getPath();
        char[] pwChars = password.toCharArray();

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream inputStream = new FileInputStream(new File(filePath));
            keystore.load(inputStream, pwChars);
            inputStream.close();
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong reading keystore", e);
        }
    }

    private List<BasicX509Credential> getIDPCredList(){

        KeyStore keyStore = getKeyStore();

        String aliasName = PropertityUtil.getProperty("idp.alias.name");
        String password = PropertityUtil.getProperty("idp.alias.password");

        java.security.cert.X509Certificate certificate = null;
        try {
            certificate = (java.security.cert.X509Certificate) keyStore.getCertificate(aliasName);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        PrivateKey privateKey = null;
        try {
            privateKey = (PrivateKey) keyStore.getKey(aliasName, password.toCharArray());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        BasicX509Credential basicX509Credential = new BasicX509Credential();
        basicX509Credential.setEntityCertificate(certificate);
        basicX509Credential.setPrivateKey(privateKey);

        List<BasicX509Credential> basicX509Credentials = new ArrayList<BasicX509Credential>();
        basicX509Credentials.add(basicX509Credential);

        return basicX509Credentials;
    }

    private List<BasicX509Credential> getSPCredList(UsageType usageType){
        List<KeyDescriptor> keyDescriptors = ssoDescriptor.getKeyDescriptors();
        if(null == keyDescriptors){
            throw new RuntimeException("sp keyDescriptors is null");
        }

        BasicX509Credential basicX509Credential = new BasicX509Credential();

        for(KeyDescriptor keyDescriptor : keyDescriptors){
            if(keyDescriptor.getUse() == usageType){
                X509Certificate x509Certificate = keyDescriptor.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0);
                java.security.cert.X509Certificate certificate = covertSamlCertToJavaCert(x509Certificate);
                basicX509Credential.setEntityCertificate(certificate);
                break;
            }
        }

        List<BasicX509Credential> basicX509CredentialList = new ArrayList<BasicX509Credential>();
        basicX509CredentialList.add(basicX509Credential);
        return basicX509CredentialList;
    }

    private java.security.cert.X509Certificate covertSamlCertToJavaCert(X509Certificate x509Certificate){
        CertificateFactory certificateFactory = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        byte[] bytes = Base64.decode(x509Certificate.getValue());
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        java.security.cert.X509Certificate certificate = null;
        try {
            certificate = (java.security.cert.X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return certificate;
    }
}
