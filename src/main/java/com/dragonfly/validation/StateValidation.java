package com.dragonfly.validation;

import com.dragonfly.anno.State;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/22 20:55
 */
public class StateValidation implements ConstraintValidator<State,String> {//泛型：第一个参数是表示为哪一个注解提供校验规则，第二个校验的数据类型

    /**
     *
     * @param value 将来要检验的数据
     * @param constraintValidatorContext
     *
     * @return 校验结果, true表示校验通过，false表示校验失败
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        //提供校验规则
        if(value==null){return false;}
        if(value.equals("已发布")||value.equals("草稿")||value.equals("已下架")){
            return true;
        }
        return false;
    }
}
