package org.springframework.support;

import org.springframework.beans.BeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BeanDefinitionReader {
    private Properties properties = new Properties();
    private List<String> registyBeanClasses = new ArrayList<String>();
    private final String SCAN_PACKAGE = "scanPackage";
    public BeanDefinitionReader(String... configLocations){
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(configLocations[0].replace("classpath:", ""));
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (null != resourceAsStream)
                    resourceAsStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        doScanner(properties.getProperty(SCAN_PACKAGE));
    }

    public List<String> loadBeanDefinitions(){

        return registyBeanClasses;
    }

    //
    public BeanDefinition registerBean(String className){
        if(this.registyBeanClasses.contains(className)){
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setBeanClassName(className);
            beanDefinition.setFactoryBeanName(lowerFirst(className.substring(className.lastIndexOf(".")+1)));
            return beanDefinition;
        }
        return  null;
    }
    Properties getProperties(){
        return properties;
    }
    //递归扫描所有的相关联的class，并且保存到一个List中
    private void doScanner(String packageName) {

        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));

        File classDir = new File(url.getFile());

        for (File file : classDir.listFiles()){
            if(file.isDirectory()){
                doScanner(packageName + "." +file.getName());
            }else {
                registyBeanClasses.add(packageName + "." + file.getName().replace(".class",""));
            }
        }

    }
    private String lowerFirst(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
    public Properties getConfig(){
        return properties;
    }
}
