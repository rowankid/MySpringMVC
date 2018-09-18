package cn.luowq.myspringmvc.annotation;

import java.lang.annotation.*;

/**
 * @Auther: rowan
 * @Date: 2018/9/17 16:33
 * @Description:持久化层注解
 */
@Documented //JAVADOC文档
@Target(ElementType.TYPE)//作用于类上
@Retention(RetentionPolicy.RUNTIME)//限制Annotation的生命周期，此定义为运行时保留
public @interface Repository {
    public String value();
}
