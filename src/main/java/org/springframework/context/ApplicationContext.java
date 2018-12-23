package org.springframework.context;

import org.springframework.annotation.Autowired;
import org.springframework.annotation.Controller;
import org.springframework.annotation.Service;
import org.springframework.beans.BeanDefinition;
import org.springframework.beans.BeanPostProcessor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.support.BeanDefinitionReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext implements BeanFactory {
    String[] configLocations;
    private BeanDefinitionReader reader;
    private Map<String,BeanDefinition> benaDefinitionMap = new HashMap<String,BeanDefinition>();
    private Map<String,Object> beanCacheMap  = new HashMap<String, Object>();
    //用于存储所有被代理过的对象
    private Map<String,BeanWrapper> beanWrapperMap = new ConcurrentHashMap<String, BeanWrapper>();
    public ApplicationContext(String... configLocations) {
        this.configLocations = configLocations;
        this.reflush();
    }

    //通过读取BeanDefinition中的信息，通过反射创建一个实例返回
    //spring做法，不会把原始的bean返回去，而是会用一个BeanWrapper来进行一次包装
    //装饰器模式：1，保留原来的oop关系，2，扩展，增加
    //依赖注入从这里开始
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = this.benaDefinitionMap.get(beanName);
        BeanPostProcessor beanPostProcessor = new BeanPostProcessor();
        Object instantionBean = instantionBean(beanDefinition);
        if(instantionBean == null){return null;}
        beanPostProcessor.postProcessBeforeInitialization(instantionBean,beanName);
        BeanWrapper beanWrapper = new BeanWrapper(instantionBean);
        this.beanWrapperMap.put(beanName,beanWrapper);
        beanPostProcessor.postProcessAfterInitialization(instantionBean,beanName);
        //populateBean(beanName,instantionBean);
        //
        return this.beanWrapperMap.get(beanName).getWrapperInstance();
    }
    private Object instantionBean(BeanDefinition beanDefinition){
        String beanClassName = beanDefinition.getBeanClassName();
        Object instance = null;
        try {

            //是否已有
            if(this.beanCacheMap.containsKey(beanClassName)){
                instance = this.beanCacheMap.get(beanClassName);
            }else{
                Class<?> clazz = Class.forName(beanClassName);
                instance = clazz.newInstance();
                this.beanCacheMap.put(beanClassName,instance);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return instance;
    }
    private void reflush(){

        //定位
        this.reader = new BeanDefinitionReader(configLocations);

        //加载
        List<String> registyBeanClasses = reader.loadBeanDefinitions();
        //注册
        doRegisty(registyBeanClasses);
        //依赖注入（Lazy-init=false的时候执行）,自动调用getbean方法
        doAutowired();


    }

    //开始自动化的依赖注入
    private void doAutowired() {
        for(Map.Entry<String, BeanDefinition> beanDefinnition: this.benaDefinitionMap.entrySet()){
            String beanName = beanDefinnition.getKey();
            if (!beanDefinnition.getValue().isLazyInit()){
                getBean(beanName);
            }
        }
        for(Map.Entry<String,BeanWrapper> beanWrapperEntry : this.beanWrapperMap.entrySet()){

            populateBean(beanWrapperEntry.getKey(),beanWrapperEntry.getValue().getOriginalInstance());

        }
    }
    public void populateBean(String beanName,Object instance){
        Class<?> clazz = instance.getClass();
        if(!clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Service.class)){
            return;
        }
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if(!field.isAnnotationPresent(Autowired.class)){ continue;}
            Autowired autowired = field.getAnnotation(Autowired.class);
            String autowiredBeanName = autowired.value().trim();
            if("".endsWith(autowiredBeanName)){
                autowiredBeanName = field.getType().getName();
            }
            field.setAccessible(true);
            try {
                field.set(instance,this.beanWrapperMap.get(autowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }
    //真正将beanDefinition注入到IOC容器中
    private void doRegisty(List<String> registyBeanClasses) {
        //beanname 有三种情况
        //1，默认首字母小写
        //2,自定义名称
        //3，接口注入
        try{
            for (String className : registyBeanClasses){
                Class<?> clazzName = Class.forName(className);
                //如果是一个接口不能实例化，需要用他的实现类实例化
                if (clazzName.isInterface()) continue;
                BeanDefinition beanDefinition = reader.registerBean(className);
                if (beanDefinition != null){
                    this.benaDefinitionMap.put(beanDefinition.getFactoryBeanName(),beanDefinition);
                }

                Class<?>[] interfaces = clazzName.getInterfaces();
                for (Class<?> i :interfaces){
                    //多个实现类，要么报错，要么覆盖，通常可以自定义不同名称
                    this.benaDefinitionMap.put(i.getName(),beanDefinition);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public String[] getBeanDefinitionNames() {
        return this.benaDefinitionMap.keySet().toArray(new String[this.benaDefinitionMap.size()]);
    }
    public int getBeanDefinitionCount() {
        return  this.benaDefinitionMap.size();
    }

    public Properties getConfig(){
        return this.reader.getConfig();
    }
}
