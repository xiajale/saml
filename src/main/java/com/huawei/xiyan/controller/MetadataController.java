package com.huawei.xiyan.controller;

import com.huawei.xiyan.metadata.IDPMetadataFact;
import com.huawei.xiyan.metadata.IDPMetadataHandler;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by zhouyibin on 2017/12/11.
 */
@RestController
public class MetadataController {

    @RequestMapping(value = "/metadata", method = RequestMethod.GET, produces = "application/xml")
    public String getIDPMetadata(boolean unsigned){
        IDPMetadataHandler idpMetadataHandler = IDPMetadataFact.getInstance();
        return idpMetadataHandler.covertEntityDescriptorToXML(unsigned);
    }
}
