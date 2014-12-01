package org.blaazinsoftware.centaur.annotation;

import java.lang.annotation.*;

/**
 * Defines the assigned field as the Kind field to use with Google App Engine's Data Store
 *
 * @author Randy May
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AppEngineKind {
}
