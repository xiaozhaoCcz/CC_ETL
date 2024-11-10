package com.cc.job.task.enums;

import com.xxl.job.core.glue.GlueTypeEnum;

public enum CcJobGlueTypeEnum {
    API("API", false);
    private String desc;
    private boolean isScript;

    CcJobGlueTypeEnum(String desc, boolean isScript) {
        this.desc = desc;
        this.isScript = isScript;
    }


    public String getDesc() {
        return desc;
    }

    public boolean isScript() {
        return isScript;
    }

    public static CcJobGlueTypeEnum match(String name){
        for (CcJobGlueTypeEnum item: CcJobGlueTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }
}
