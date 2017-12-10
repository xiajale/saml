package com.huawei.xiyan.metadata;

import com.huawei.xiyan.Constants.MetadataConstants;
import com.huawei.xiyan.util.OpenSamlUtil;
import com.huawei.xiyan.util.PropertityUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.security.utils.Base64;
import org.opensaml.Configuration;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.io.*;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.cert.CertificateEncodingException;

/**
 * Created by zhouyibin on 2017/12/11.
 */
public abstract class AbstractMetadataHandler implements MetadataHandler {

    protected MetadataBean metadataBean;

    public AbstractMetadataHandler(String metadataPath){
        EntityDescriptor entityDescriptor = loadEntityDescriptorFromFile(metadataPath);
        this.metadataBean = new MetadataBean(entityDescriptor);
    }

    @Override
    public EntityDescriptor loadEntityDescriptorFromFile(String filePath) {

        File file = new File(filePath);

        Element element = null;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            element = document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        EntityDescriptor entityDescriptor = null;
        try {
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            entityDescriptor = (EntityDescriptor) unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            e.printStackTrace();
        }

        return entityDescriptor;
    }

    public String covertEntityDescriptorToXML(boolean unsigned){

        EntityDescriptor entityDescriptor = getEntityDescriptor();
        entityDescriptor.setID("sssss");

        if(false == unsigned){
            X509Credential x509Credential = this.metadataBean.getSigningCredList().get(0);

            Signature signature = OpenSamlUtil.buildSAMLObject(Signature.class);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setSigningCredential(x509Credential);
            signature.setSignatureAlgorithm(getSignatureAlg());


            KeyInfo keyInfo = OpenSamlUtil.buildSAMLObject(KeyInfo.class);
            X509Data x509Data = OpenSamlUtil.buildSAMLObject(X509Data.class);
            X509Certificate x509Certificate = OpenSamlUtil.buildSAMLObject(X509Certificate.class);
            try {
                x509Certificate.setValue(Base64.encode(x509Credential.getEntityCertificate().getEncoded()));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            x509Data.getX509Certificates().add(x509Certificate);
            keyInfo.getX509Datas().add(x509Data);
            signature.setKeyInfo(keyInfo);

            entityDescriptor.setSignature(signature);

            try {
                Configuration.getMarshallerFactory().getMarshaller(entityDescriptor).marshall(entityDescriptor);
            } catch (MarshallingException e) {
                e.printStackTrace();
            }

            try {
                Signer.signObject(signature);
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }

        Source source = null;
        Result result = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            MarshallerFactory marshallerFactory = Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(entityDescriptor);
            Element element = marshaller.marshall(entityDescriptor);
            Document document = element.getOwnerDocument();
            result = new StreamResult(byteArrayOutputStream);
            source = new DOMSource(document);
        } catch (MarshallingException e) {
            e.printStackTrace();
        }

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
        return this.metadataBean.getEntityDescriptor();
    }

    @Override
    public String getEntityId() {
        return this.metadataBean.getEntityId();
    }

    private static String getSignatureAlg(){
        String alg = PropertityUtil.getProperty("idp.sig.alg");
        if(StringUtils.equals(MetadataConstants.RSA_SHA1, alg)){
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        }
        else if(StringUtils.equals(MetadataConstants.RSA_SHA256, alg)){
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
        }
        else {
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        }
    }

}
