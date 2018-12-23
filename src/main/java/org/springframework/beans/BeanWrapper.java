package org.springframework.beans;

import org.springframework.beans.factory.FactoryBean;

public class BeanWrapper extends FactoryBean {

    public BeanPostProcessor getBeanPostProcessor() {
        return beanPostProcessor;
    }

    public void setBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessor = beanPostProcessor;
    }

    private BeanPostProcessor beanPostProcessor;
    private Object wrapperInstance;

    public Object getOriginalInstance() {
        return originalInstance;
    }

    public void setOriginaInstance(Object originalInstance) {
        this.originalInstance = originalInstance;
    }

    //原生的通过反射new的，要包装以后，存下来
    private Object originalInstance;

    public BeanWrapper(Object instance){
        this.wrapperInstance = instance;
        this.originalInstance = instance;
    }
    public Object getWrapperInstance(){
        return this.wrapperInstance;
    }
    //返回代理以后的Class，$Proxy0
    public Class<?> getWrapperClass(){
        return this.wrapperInstance.getClass();
    }
}
