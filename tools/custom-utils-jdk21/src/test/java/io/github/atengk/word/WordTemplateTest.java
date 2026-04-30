package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WordTemplateTest {

    @Test
    void fillTemplateShouldReplaceDefaultPlaceholders() {
        XWPFDocument document = WordUtilTestSupport.sampleDocument();
        WordUtil.fillTemplate(document, Map.of("name", "李四", "age", 20));
        assertFalse(WordUtil.containsText(document, "${name}"));
        assertTrue(WordUtil.containsText(document, "李四"));
    }

    @Test
    void placeholderValidationShouldFindMissingKeys() {
        XWPFDocument document = WordUtilTestSupport.sampleDocument();
        Set<String> missing = WordUtil.validatePlaceholders(document, Map.of("name", "王五"));
        assertTrue(missing.contains("age"));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.checkPlaceholders(document, Set.of("missing")));
    }

    @Test
    void clearUnusedPlaceholdersShouldRemoveThem() {
        XWPFDocument document = WordUtilTestSupport.sampleDocument();
        WordUtil.clearUnusedPlaceholders(document);
        assertTrue(WordUtil.getPlaceholders(document).isEmpty());
    }
}
