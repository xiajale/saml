package com.huawei.xiyan.util;

import com.huawei.xiyan.Constants.MetadataConstants;
import org.apache.commons.lang.StringUtils;
import org.opensaml.Configuration;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.*;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.util.Base64;

import javax.xml.namespace.QName;
import java.security.cert.*;

/**
 * Created by zhouyibin on 2017/12/11.
 */
public class OpenSamlUtil {

    public static <T> T buildSAMLObject(final Class<T> clazz) {
        T object = null;
        try {
            XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
            QName defaultElementName = (QName)clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            object = (T)builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Could not create SAML object");
        }

        return object;
    }

    public static void signature(Signature signature, SignableXMLObject signableXMLObject){

        signableXMLObject.setSignature(signature);

        try {
            Configuration.getMarshallerFactory().getMarshaller(signableXMLObject).marshall(signableXMLObject);
        } catch (MarshallingException e) {
            e.printStackTrace();
        }

        try {
            Signer.signObject(signature);
        } catch (SignatureException e) {
            e.printStackTrace();
        }

    }

}
