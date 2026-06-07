package com.school.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在管理端写操作方法上，由 {@link OperationLogAspect} 自动记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作类型，例如“学生管理”“课程管理”。 */
    String type();

    /** 操作动作描述，例如“新增学生”“删除课程”。 */
    String action();
}
