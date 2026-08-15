package umc.cockple.demo.domain.exercise.presentation.controller.api;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag(name = "Exercise", description = "운동 관리 API")
public @interface ExerciseApiTag {
}
