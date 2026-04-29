package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilDesignTest {

    @Test
    void shouldBeFinalStaticUtilityClass() throws Exception {
        assertTrue(Modifier.isFinal(FileTypeUtil.class.getModifiers()));

        Constructor<FileTypeUtil> constructor = FileTypeUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }

    @Test
    void shouldBuildPolicyByBuilder() {
        FileTypePolicy policy = FileTypePolicy.builder()
                .allowMimeTypes(Set.of("image/*"))
                .denyMimeTypes(Set.of("application/x-msdownload"))
                .allowExtensions(Set.of("png"))
                .denyExtensions(Set.of("exe"))
                .allowCategories(Set.of(FileTypeCategory.IMAGE))
                .denyCategories(Set.of(FileTypeCategory.EXECUTABLE))
                .checkExtensionMatch(true)
                .rejectUnknownType(true)
                .rejectDangerousType(true)
                .allowWildcardMimeType(true)
                .build();

        assertTrue(policy.getAllowMimeTypes().contains("image/*"));
        assertTrue(policy.getDenyMimeTypes().contains("application/x-msdownload"));
        assertTrue(policy.getAllowExtensions().contains("png"));
        assertTrue(policy.getDenyExtensions().contains("exe"));
        assertTrue(policy.getAllowCategories().contains(FileTypeCategory.IMAGE));
        assertTrue(policy.getDenyCategories().contains(FileTypeCategory.EXECUTABLE));
        assertTrue(policy.isCheckExtensionMatch());
        assertTrue(policy.isRejectUnknownType());
        assertTrue(policy.isRejectDangerousType());
        assertTrue(policy.isAllowWildcardMimeType());
    }

    @Test
    void shouldCreateRuleAndResultObjects() {
        FileTypeRule rule = new FileTypeRule("图片", "image/*", "png", FileTypeCategory.IMAGE, true);
        FileTypeInfo info = FileTypeUtil.toFileTypeInfo("demo.png", "image/png");
        FileTypeCheckResult passed = FileTypeCheckResult.passed(info);
        FileTypeCheckResult failed = FileTypeCheckResult.failed(info, List.of("失败"));

        assertEquals("图片", rule.getName());
        assertEquals("image/*", rule.getMimePattern());
        assertEquals("png", rule.getExtension());
        assertEquals(FileTypeCategory.IMAGE, rule.getCategory());
        assertTrue(rule.isAllow());
        assertTrue(passed.isPassed());
        assertFalse(failed.isPassed());
        assertEquals("失败", failed.getFirstMessage());
    }
}
