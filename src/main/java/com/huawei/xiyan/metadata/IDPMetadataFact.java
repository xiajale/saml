package com.huawei.xiyan.metadata;

import com.huawei.xiyan.util.PropertityUtil;

/**
 * Created by zhouyibin on 2017/12/11.
 */
public class IDPMetadataFact {

    private static IDPMetadataHandler idpMetadataHandler;
    private static final String IDP_METADATA_NAME = PropertityUtil.getProperty("idp.metadata.file");

    private IDPMetadataFact(){

    }

    public static IDPMetadataHandler getInstance(){
        if (null == idpMetadataHandler){
            String filePath = IDPMetadataHandler.class.getClassLoader().getResource(IDP_METADATA_NAME).getPath();
            idpMetadataHandler = new IDPMetadataHandler(filePath);
        }
        return idpMetadataHandler;
    }
}
