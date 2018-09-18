package cn.luowq.myspringmvc.annotation;

import java.lang.annotation.*;

/**
 * @Auther: rowan
 * @Date: 2018/9/17 16:33
 * @Description:提供URL地址处理映射
 */
@Documented //JAVADOC文档
@Target({ElementType.TYPE,ElementType.FIELD})//作用于类或方法上
@Retention(RetentionPolicy.RUNTIME)//限制Annotation的生命周期，此定义为运行时保留
public @interface RequestMapping {
    public String value();
}
