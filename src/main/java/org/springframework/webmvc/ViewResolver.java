package org.springframework.webmvc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//设计这个类的主要目的是：
//1、讲一个静态文件变为一个动态文件
//2、根据用户传送参数不同，产生不同的结果
//最终输出字符串，交给Response输出
public class ViewResolver {
    private String viewName;
    private File templateFile;
    public ViewResolver(String viewName,File templateFile){
        this.templateFile = templateFile;
        this.viewName = viewName;

    }
    public String viewResolver(ModelAndView mv){
        StringBuffer sb = new StringBuffer();
        try {
            RandomAccessFile ra = new RandomAccessFile(this.templateFile,"r");

            String line = null;
            while (null != (line = ra.readLine())){
                Matcher matcher = matcher(line);
                while (matcher.find()){
                    for (int i = 1; i < matcher.groupCount(); i++) {
                        //要把￥{}中间的这个字符串给取出来
                        String paramName = matcher.group(i);
                        Object paramValue = mv.getModel().get(paramName);
                        if (null == paramValue) {
                            continue;
                        }
                        line = line.replaceAll("￥\\{" + paramName + "\\}", paramValue.toString());
                        line = new String(line.getBytes("utf-8"), "ISO-8859-1");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    public Matcher matcher(String str){
        Pattern pattern = Pattern.compile("￥\\{(.+?)\\}",Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);
        return  matcher;

    }
    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public File getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(File templateFile) {
        this.templateFile = templateFile;
    }
}
