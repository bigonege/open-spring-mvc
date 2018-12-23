package org.springframework.annotation;

import java.lang.annotation.*;

/**
 * 页面交互
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
	String value() default "";
}
