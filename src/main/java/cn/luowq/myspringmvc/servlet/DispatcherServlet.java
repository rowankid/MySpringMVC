package cn.luowq.myspringmvc.servlet;

import cn.luowq.myspringmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: rowan
 * @Date: 2018/9/17 16:33
 * @Description:要知道，在Spring MVC中，要想基于注解，需要在配置中指明扫描的包路径，就像这个样子：
 *
 * <context:component-scan base-package="cn.luowq.myspringmvc">
 * </context:component-scan>
 *
 * 为了方便，我这里就通过初始化参数直接将需要扫描的基包路径传入。
 */
@WebServlet(name = "dispatcherServlet",urlPatterns = "/*",loadOnStartup = 1,
        initParams = {@WebInitParam(name = "base-package",value = "cn.luowq.myspringmvc")})
public class DispatcherServlet extends HttpServlet {

    //扫描的基包
    private String basePackage = "";
    //基包下面所有的带包路径权限定类名
    private List<String> packageNames = new ArrayList<String>();
    //注解实例化  注解上的名称：实例化对象
    private Map<String, Object> instanceMap = new HashMap<String, Object>();
    //带包路径的权限定名称：注解上的名称
    private Map<String, String> nameMap = new HashMap<String, String>();
    //URL地址和方法的映射关系  SpringMVC就是方法调用链
    private Map<String, Method> urlMethodMap = new HashMap<String, Method>();
    //Method和权限定类名映射关系  主要是为了通过Method找到该方法的对象利用反射执行。
    private Map<Method, String> methodPackageMap = new HashMap<Method, String>();

    @Override
    public void init(ServletConfig config){
        basePackage = config.getInitParameter("base-package");

        try{
            //1.扫描基包得到所有的带包路径权限定名
            scanBasePackage(basePackage);
            //2.把所有带有@Controller/@Service/@Repository的类实例化放入Map中，KEY为注解上的名称
            instance(packageNames);
            //3.Spring IOC注入
            springIOC();
            //完成URL地址于方法的映射关系
            handlerUrlMethodMap();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }catch(InstantiationException e){
            e.printStackTrace();
        }catch(IllegalAccessException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.replaceAll(contextPath,"");

        //通过path找到method
        Method method = urlMethodMap.get(path);
        if(method!=null){

            //通过Method拿到Controller对象，准备反射执行
            String packageName = methodPackageMap.get(method);
            String controllerName = nameMap.get(packageName);

            //拿到Controller对象
            UserController userController = (UserController)instanceMap.get(controllerName);
            try {
                method.setAccessible(true);
                method.invoke(userController);
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }

        private void scanBasePackage (String basePackage){
        //注意为了得到基包下面的URL路径需要对basePackage做转换:将.替换为/
        // /D:/MySoftware/apache-tomcat-7.0.78/webapes/mvagringmvc/WEB-INE/classes/cn/luowq/mvspringmvc/
        URL url = this.getClass().getClassLoader() .getResource (basePackage.replaceAll("\\.","/"));
        File basePackageFile = new File (url.getPath());
        System. out.println("scan:"+basePackageFile);
        File[] childFiles = basePackageFile.listFiles();
        for (File file: childFiles){
            if (file.isDirectory()) {//目录继续递归扫描
                scanBasePackage(basePackage + "." +file.getName());
            } else if (file.isFile()) {
            //类似这种:cn.luowq.myspringmvc.service.impl.UserServiceImpl.class去掉class
            packageNames.add(basePackage + "."+ file.getName().split("\\.")[0]);
            }
        }
    }
    private void instance(List<String> packageNames) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if(packageNames.size()<1){
            return;
        }

        for (String string: packageNames) {
            Class c =Class.forName(string);
            if(c.isAnnotationPresent(Controller.class)){
                Controller controller = (Controller) c.getAnnotation(Controller.class);
                String controllerName = controller.value();

                instanceMap.put(controllerName,c.newInstance());
                nameMap.put(string, controllerName);
                System.out.println("Controller : "+string+" ,value : "+controller.value());
            }else if(c.isAnnotationPresent(Service.class)){
                Service service = (Service) c.getAnnotation(Service.class);
                String serviceName = service.value();

                instanceMap.put(serviceName,c.newInstance());
                nameMap.put(string, serviceName);
                System.out.println("Service : "+string+" ,value : "+service.value());
            }else if(c.isAnnotationPresent(Repository.class)){
                Repository repository = (Repository) c.getAnnotation(Repository.class);
                String repositoryName = repository.value();

                instanceMap.put(repositoryName,c.newInstance());
                nameMap.put(string, repositoryName);
                System.out.println("Repository : "+string+" ,value : "+repository.value());
            }
        }
    }

    private void springIOC() throws IllegalAccessException {
        for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field: fields) {
                if(field.isAnnotationPresent(Qualifier.class)){
                    String name = field.getAnnotation(Qualifier.class).value();
                    field.setAccessible(true);
                    field.set(entry.getValue(),instanceMap.get(name));
                }
            }
        }
    }

    private void handlerUrlMethodMap() throws ClassNotFoundException {
        if(packageNames.size()<1){
            return;
        }

        for (String string: packageNames) {
            Class c = Class.forName(string);
            if (c.isAnnotationPresent(Controller.class)){
                Method[] methods = c.getMethods();
                StringBuffer baseUrl = new StringBuffer();
                if(c.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping requestMapping = (RequestMapping) c.getAnnotation(RequestMapping.class);
                    baseUrl.append(requestMapping.value());
                }

                for (Method method:methods) {
                    if(method.isAnnotationPresent(RequestMapping.class)){
                        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                        baseUrl.append(requestMapping.value());

                        urlMethodMap.put(baseUrl.toString(),method);
                        methodPackageMap.put(method,string);
                    }
                }
            }
        }
    }
}