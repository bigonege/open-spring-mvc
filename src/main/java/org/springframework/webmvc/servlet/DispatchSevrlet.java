package org.springframework.webmvc.servlet;

import org.springframework.annotation.Controller;
import org.springframework.annotation.RequestMapping;
import org.springframework.annotation.RequestParam;
import org.springframework.context.ApplicationContext;
import org.springframework.controller.action.MyAction;
import org.springframework.webmvc.HandlerAdapter;
import org.springframework.webmvc.HandlerMapping;
import org.springframework.webmvc.ModelAndView;
import org.springframework.webmvc.ViewResolver;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatchSevrlet extends HttpServlet {

    List<HandlerMapping> handlerMappings = new ArrayList<HandlerMapping>();

    Map<HandlerMapping,HandlerAdapter> handlerAdapters = new HashMap<HandlerMapping, HandlerAdapter>();

    private List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("invoke local doPost Methed");
        try {
            doDispatch(req, resp);
        }catch (Exception e){
            resp.getWriter().write("<font size='25' color='blue'>500 Exception</font><br/>Details:<br/>" + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s","\r\n") +  "<font color='green'><i>Copyright@GupaoEDU</i></font>");
            e.printStackTrace();
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //根据用户请求的URL来获得一个Handler
        HandlerMapping handler = getHandler(req);
        if (null == handler){
            resp.getWriter().write("<font size='25' color='red'>404 Not Found</font><br/><font color='green'><i>Copyright@GupaoEDU</i></font>");
            return;
        }
        HandlerAdapter ha = getHandlerAdapter(handler);
        //这一步只是调用方法，得到返回值
        ModelAndView mv = null;
        try {
            mv = ha.handle(req, resp, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //这一才真的输出
        processDispatchResult(resp, mv);
    }

    private void processDispatchResult(HttpServletResponse resp, ModelAndView mv) throws IOException {
        //调用viewResolver的resolveView方法
        if(null == mv){ return;}

        if(this.viewResolvers.isEmpty()){ return;}
        for (ViewResolver viewResolver: this.viewResolvers) {

            if(!mv.getViewName().equals(viewResolver.getViewName())){ continue; }
            String out = viewResolver.viewResolver(mv);
            if(out != null){
                resp.getWriter().write(out);
                break;
            }
        }
    }

    private HandlerAdapter getHandlerAdapter(HandlerMapping handler) {
        if(null == this.handlerAdapters) return null;

        return this.handlerAdapters.get(handler);
    }

    private HandlerMapping getHandler(HttpServletRequest req) {

        if (this.handlerMappings == null){return  null;}

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        requestURI = requestURI.replace(contextPath,"").replaceAll("/+","/");

        for (HandlerMapping handlerMapping: this.handlerMappings){
            Matcher matcher = handlerMapping.getPattern().matcher(requestURI);
            if (!matcher.matches()){continue;}

            return handlerMapping;

        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        ApplicationContext context = new ApplicationContext(config.getInitParameter("contextLocation"));
        initStrategies(context);
        /*MyAction myAction = (MyAction) contextLocation.getBean("myAction");
        myAction.query(null,null,"aaaaaaa");*/
    }

    private void initStrategies(ApplicationContext context) {
        //springmvc 九大组件
        initMultipartResolver(context);//文件上传解析
        initLocaleResolver(context);//本地化解析
        initThemeResolver(context);//主题解析
        //保存controller中requestmapping和method的对应关系
        initHandlerMappings(context);//通过handlerMapping 将请求映射到处理器
        //动态匹配Method参数，保护类转换，动态赋值
        initHandlerAdapters(context);//通过HandlerAdapter 进行多类型的参数动态匹配
        initHandlerExceptionResolvers(context);//如果执行中遇到异常，交给他来解析
        initRequestToViewNameTranslator(context);
        initViewResolvers(context);//通过viewResolver解析逻辑师徒到具体视图实现
        initFlashMapManager(context);
    }

    private void initHandlerMappings(ApplicationContext context) {
        //从容器中取到所有实例
        String[] beanNames = context.getBeanDefinitionNames();
        for(String beanName : beanNames){
            Object controller = context.getBean(beanName);
            Class<?> clazz = controller.getClass();
            if(!clazz.isAnnotationPresent(Controller.class)){continue;}

            String baseUrl = "";
            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //扫描public方法
            Method[] methods = clazz.getMethods();
            for (Method method:methods){
                if(!method.isAnnotationPresent(RequestMapping.class)){continue;}

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String regex = "/"+baseUrl+requestMapping.value().replaceAll("\\*", ".*").replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                this.handlerMappings.add(new HandlerMapping(controller,method,pattern));
                System.out.println("mapping"+ regex + " , " + method);
            }

        }
    }

    private void initHandlerAdapters(ApplicationContext context) {
        for (HandlerMapping handlerMapping : this.handlerMappings){

            Map<String,Integer> paramMapping = new HashMap<String,Integer>();
            Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();
            //这里只处理命名参数
            for (int i = 0; i < pa.length; i++) {
                for (Annotation annotation : pa[i]){
                    if (annotation instanceof RequestParam){
                        String paramName = ((RequestParam) annotation).value();
                        if (!"".equals(paramName.trim())){
                            paramMapping.put(paramName,i);
                        }
                    }
                }

            }
            //处理非命名参数，只处理Request和Response
            Class<?>[] parameterTypes = handlerMapping.getMethod().getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramMapping.put(type.getName(),i);
                }

            }
            this.handlerAdapters.put(handlerMapping,new HandlerAdapter(paramMapping));

        }
    }

    private void initViewResolvers(ApplicationContext context) {
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        File templateRootDir = new File(templateRootPath);
        for (File template : templateRootDir.listFiles()){
            this.viewResolvers.add(new ViewResolver(template.getName(),template));
        }
    }

    private void initHandlerExceptionResolvers(ApplicationContext context) { }
    private void initFlashMapManager(ApplicationContext context) { }
    private void initRequestToViewNameTranslator(ApplicationContext context) { }
    private void initThemeResolver(ApplicationContext context) { }
    private void initLocaleResolver(ApplicationContext context) { }
    private void initMultipartResolver(ApplicationContext context) { }
}
