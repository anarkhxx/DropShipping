package com.zjut.dropshipping.common;

/**
 * 常量类
 * @author zjxjwxk
 */
public class Const {

    public static final String CURRENT_USER = "currentUser";

    public static final String PHONE = "phone";
    public static final String IDENTITY_NUMBER = "identityNumber";
    public static final String EXTERNAL_SHOP = "externalShop";

    public interface State {

        String NORMAL = "正常";

        String UNAPPROVED = "未批准";

        String FROZEN = "冻结";
    }

    public interface UploadType {

        String IDCARD_1 = "IDCard-1";

        String IDCARD_2 = "IDCard-2";
    }

}
