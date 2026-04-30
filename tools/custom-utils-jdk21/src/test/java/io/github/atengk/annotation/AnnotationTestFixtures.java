package io.github.atengk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

final class AnnotationTestFixtures {

    private AnnotationTestFixtures() {
    }

    enum Level {
        LOW,
        HIGH
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
    @interface Marker {
        String value() default "default";

        boolean enabled() default true;

        Class<?> type() default Void.class;

        Level level() default Level.LOW;

        String[] tags() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @interface Secondary {
        String value() default "secondary";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface MetaFlag {
        String value() default "meta";
    }

    @MetaFlag("composed-meta")
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @interface Composed {
        String value() default "composed";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface DeepMeta {
    }

    @DeepMeta
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface MiddleMeta {
    }

    @MiddleMeta
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface DeepComposed {
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface InheritedFlag {
        String value() default "inherited";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Tags.class)
    @interface Tag {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Tags {
        Tag[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface FieldFlag {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface MethodFlag {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ParameterFlag {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    @interface ConstructorFlag {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface RecordFlag {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface ClassRetentionFlag {
    }

    @interface NoTargetFlag {
    }

    @Marker(value = "base", enabled = false, type = String.class, level = Level.HIGH, tags = {"base", "root"})
    @InheritedFlag("base-inherited")
    @Tag("base-class-1")
    @Tag("base-class-2")
    static class BaseSample {
        @FieldFlag
        protected String baseField;

        @Marker("baseMethod")
        @Tag("base-method-1")
        @Tag("base-method-2")
        String baseMethod() {
            return "base";
        }
    }

    interface ApiSample {
        @MethodFlag
        default void apiMethod() {
        }
    }

    @Marker("child")
    @Secondary("child-secondary")
    @Composed("child-composed")
    @DeepComposed
    static class ChildSample extends BaseSample implements ApiSample {
        @FieldFlag
        private Integer childField;

        @ConstructorFlag
        ChildSample() {
        }

        @Marker(value = "childMethod", tags = {"method"})
        @Composed("method-composed")
        @MethodFlag
        void childMethod(@ParameterFlag String name) {
        }

        @Override
        public void apiMethod() {
        }

        void noAnnotationMethod() {
        }
    }

    @InheritedFlag("direct-parent")
    static class ParentWithInherited {
    }

    static class ChildWithInherited extends ParentWithInherited {
    }

    @Tag("record-class")
    record UserRecord(@RecordFlag String name, Integer age) {
    }

    static class EmptySample {
        String emptyField;

        void emptyMethod() {
        }
    }
}
