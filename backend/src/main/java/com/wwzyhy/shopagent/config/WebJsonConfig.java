package com.wwzyhy.shopagent.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class WebJsonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictTextFields() {
        return builder -> builder
                .withCoercionConfig(
                        LogicalType.Textual,
                        coercion -> coercion
                                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(
                        LogicalType.Integer,
                        coercion -> coercion
                                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail));
    }
}