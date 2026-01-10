package com.cc.job.xo.model.result;

import java.util.Objects;

/**
 * Bean节点执行结果
 * 
 * <p>包含Bean方法执行的返回值和相关信息
 *
 * @author cc-job-team
 */
public class BeanResult {
    
    /** Bean执行返回的值 */
    private Object value;
    
    /** 方法名 */
    private String methodName;
    
    /** 类名 */
    private String className;
    
    /** 返回类型 */
    private String returnType;
    
    /** 方法参数（可选） */
    private Object[] methodArgs;

    public BeanResult() {
    }

    public BeanResult(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanResult that = (BeanResult) o;
        return Objects.equals(methodName, that.methodName) &&
                Objects.equals(className, that.className) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, className, returnType);
    }

    @Override
    public String toString() {
        return "BeanResult{" +
                "methodName='" + methodName + '\'' +
                ", className='" + className + '\'' +
                ", returnType='" + returnType + '\'' +
                '}';
    }
}
