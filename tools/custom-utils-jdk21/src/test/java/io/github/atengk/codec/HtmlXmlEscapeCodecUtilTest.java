package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlXmlEscapeCodecUtilTest {

    @Test
    void shouldEscapeAndUnescapeHtml() {
        String escaped = CodecUtil.htmlEscape("<div class=\"a\">Tom & Jerry's</div>");
        assertEquals("&lt;div class=&quot;a&quot;&gt;Tom &amp; Jerry&#39;s&lt;/div&gt;", escaped);
        assertEquals("<div class=\"a\">Tom & Jerry's</div>", CodecUtil.htmlUnescape(escaped));
    }

    @Test
    void shouldEscapeAndUnescapeXml() {
        String escaped = CodecUtil.xmlEscape("<tag a='1'>&\"</tag>");
        assertEquals("&lt;tag a=&apos;1&apos;&gt;&amp;&quot;&lt;/tag&gt;", escaped);
        assertEquals("<tag a='1'>&\"</tag>", CodecUtil.xmlUnescape(escaped));
    }

    @Test
    void shouldEscapeAttributesAndDetectEntity() {
        assertEquals("a&quot;b", CodecUtil.escapeHtmlAttribute("a\"b"));
        assertEquals("a&apos;b", CodecUtil.escapeXmlAttribute("a'b"));
        assertTrue(CodecUtil.containsHtmlEntity("&lt;"));
        assertTrue(CodecUtil.containsHtmlEntity("&#20013;"));
    }

    @Test
    void shouldStripAndNormalizeHtmlText() {
        String html = "<style>.a{}</style><p>Hello&nbsp;<b>World</b></p>";
        assertEquals("Hello World", CodecUtil.normalizeHtmlText(html));
    }

    @Test
    void shouldHandleNumericEntitiesAndNull() {
        assertEquals("中文", CodecUtil.htmlUnescape("&#20013;&#25991;"));
        assertNull(CodecUtil.htmlEscape(null));
        assertNull(CodecUtil.xmlEscape(null));
    }
}
