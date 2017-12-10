package com.huawei.xiyan.metadata;

import org.opensaml.saml2.metadata.EntityDescriptor;

/**
 * Created by zhouyibin on 2017/12/11.
 */
public interface MetadataHandler {

    EntityDescriptor loadEntityDescriptorFromFile(String filePath);
    EntityDescriptor getEntityDescriptor();
    String getEntityId();

}
