package com.losobie.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author kevin.kendall
 */
@Target( { ElementType.METHOD, ElementType.FIELD } )
@Retention( RetentionPolicy.SOURCE )
public @interface BuilderMethod {

}
