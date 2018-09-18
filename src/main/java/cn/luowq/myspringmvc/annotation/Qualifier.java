package cn.luowq.myspringmvc.annotation;

import java.lang.annotation.*;

/**
 * @Auther: rowan
 * @Date: 2018/9/17 16:33
 * @Description:提供依赖注入
 */
@Documented //JAVADOC文档
@Target(ElementType.FIELD)//作用于字段上，实现注入
@Retention(RetentionPolicy.RUNTIME)//限制Annotation的生命周期，此定义为运行时保留
public @interface Qualifier {
    public String value();
}
