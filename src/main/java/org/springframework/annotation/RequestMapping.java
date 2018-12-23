package org.springframework.annotation;

import java.lang.annotation.*;

/**
 * 请求url
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
	String value() default "";
}
