package pers.mtx.handWriteIOC.servlet;

import pers.mtx.handWriteIOC.annonation.Autowired;
import pers.mtx.handWriteIOC.annonation.Controller;
import pers.mtx.handWriteIOC.annonation.RequestMapping;
import pers.mtx.handWriteIOC.annonation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class DisPatchServlet extends HttpServlet {
    Properties properties = new Properties();
    Set<String> classPaths = new HashSet<>();
    HashMap<String, Object> ioc = new HashMap<>();
    HashMap<String,String> urlMethodMap = new HashMap<>();
    HashMap<String,String> urlControllerMap = new HashMap<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDisPatcher(req, resp);
    }
    @Override
    public void init(ServletConfig config) throws ServletException {
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        doScanner(properties.getProperty("scanPackage"));
        doInstance();
        doAutoWired();
        initHandleMapping();
    }

    private void doDisPatcher(HttpServletRequest req, HttpServletResponse resp){
        try {
            System.out.println(req);
            System.out.println(resp);
            String controllerKey = "/"+req.getRequestURI().split("/")[1];
            Method method = ioc.get(urlControllerMap.get(controllerKey)).getClass().getMethod(urlMethodMap.get(req.getRequestURI()));
            String test = (String)method.invoke(ioc.get(urlControllerMap.get(controllerKey)));

//            String test = (String)method.invoke("test");
//            req.getRequestURI().split("/")
            resp.getOutputStream().print(test);
        }catch (Exception e ){
        }
    }

    private void doLoadConfig(String configContextLocation) {
        InputStream is = this.getClass().getResourceAsStream(configContextLocation);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                doScanner(scanPackage + "." + f.getName());
            } else {
                if (!f.getName().endsWith(".class")) {
                    continue;
                }
                String classPath = scanPackage + "." + f.getName().replace(".class", "");
                classPaths.add(classPath);

            }
        }
    }

    private void doInstance() {
        if (classPaths.isEmpty()) {
            return;
        }
        try {
            for (String className :
                    classPaths) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = clazz.getSimpleName();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Object instance = clazz.newInstance();
                    Class[] classes = clazz.getInterfaces();
                    String beanName = null;
                    if (classes.length == 0) {
                        beanName = clazz.getSimpleName();
                    } else {
                        beanName = classes[0].getSimpleName();
                    }
                    ioc.put(beanName, instance);
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry :
                ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field :
                    fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                String beanName  = field.getType().getSimpleName();
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHandleMapping(){
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry :
                ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)){
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value()[0];
                urlControllerMap.put(baseUrl,clazz.getSimpleName());
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method :
                    methods) {
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                urlMethodMap.put(baseUrl+"/"+annotation.value()[0],method.getName());
            }
        }
    }

}
