package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilFormatTest {

    @Test
    void testFormat() {
        assertEquals("hello Ateng", StringUtil.format("hello {}", "Ateng"));
        assertEquals("hello {}", StringUtil.format("hello \\{}", "Ateng"));
        assertEquals("A-B", StringUtil.formatNumbered("{0}-{1}", "A", "B"));
        assertEquals("hello Ateng", StringUtil.formatNamed("hello {name}", Map.of("name", "Ateng")));
        assertEquals("hello Ateng", StringUtil.formatByMap("hello {name}", Map.of("name", "Ateng")));
        assertEquals("hello {}", StringUtil.formatIfArgsPresent("hello {}"));
        assertEquals("hello Ateng", StringUtil.messageFormat("hello {0}", "Ateng"));
        assertEquals("1 + 2 = 3", StringUtil.printfFormat("%d + %d = %d", 1, 2, 3));
        assertEquals("hello Ateng", StringUtil.templateReplace("hello ${name}", Map.of("name", "Ateng")));
        assertEquals("hello ${name}", StringUtil.templateReplaceIgnoreMissing("hello ${name}", Map.of()));
        assertEquals("hello Ateng", StringUtil.templateReplace("hello @name@", Map.of("name", "Ateng"), "@", "@"));
        assertEquals("hello @name@", StringUtil.templateReplaceIgnoreMissing("hello @name@", Map.of(), "@", "@"));
    }

}
