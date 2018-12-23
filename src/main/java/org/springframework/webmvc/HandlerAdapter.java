package org.springframework.webmvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

public class HandlerAdapter {
    private Map<String,Integer> paramMapping;
    public HandlerAdapter(Map<String,Integer> paramMapping){
        this.paramMapping = paramMapping;
    }
    public ModelAndView handle(HttpServletRequest req, HttpServletResponse resp, HandlerMapping handler) throws Exception{
        //根据用户请求的参数信息，跟method中的参数信息进行动态匹配
        //resp 传进来的目的只有一个：只是为了将其赋值给方法参数，仅此而已
        //只有当用户传过来的ModelAndView为空的时候，才会new一个默认的
        //1、要准备好这个方法的形参列表
        //方法重载：形参的决定因素：参数的个数、参数的类型、参数顺序、方法的名字
        //1,拿到形参列表//方法重载：形参的决定因素：参数的个数、参数的类型、参数顺序、方法的名字
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
        //2,拿到自定义的参数所在位置,用户通过URL传过来的参数列表
        Map<String,String[]> reqParameterMap  = req.getParameterMap();
        //3，构造实参列表
        Object [] paramValues = new Object[parameterTypes.length];
        for (Map.Entry<String,String[]> param : reqParameterMap.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","").replaceAll("\\s","");

            if(!this.paramMapping.containsKey(param.getKey())){continue;}

            int index = this.paramMapping.get(param.getKey());

            //因为页面上传过来的值都是String类型的，而在方法中定义的类型是千变万化的
            //要针对我们传过来的参数进行类型转换
            paramValues[index] = caseStringValue(value,parameterTypes[index]);
        }
        //4,从hanler拿到controller，method，反射调用
        Object result = handler.getMethod().invoke(handler.getController(),paramValues);

        if(result == null){ return  null; }

        boolean isModelAndView = handler.getMethod().getReturnType() == ModelAndView.class;
        if(isModelAndView){
            return (ModelAndView)result;
        }else{
            return null;
        }

    }

    private Object caseStringValue(String value, Class<?> clazz) {
        if(clazz == String.class){
            return value;
        }else if(clazz == Integer.class){
            return  Integer.valueOf(value);
        }else if(clazz == int.class){
            return Integer.valueOf(value).intValue();
        }else {
            return null;
        }
    }
}
