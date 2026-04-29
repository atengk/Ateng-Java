package io.github.atengk.enums.util;

import io.github.atengk.annotation.EnumDict;
import io.github.atengk.core.FrontendEnum;
import io.github.atengk.enums.sample.scanned.ScannedStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilScanRegistryTest {

    @AfterEach
    void clean() {
        EnumUtil.clearEnumRegistry();
    }

    @Test
    void shouldScanEnumsFromPackage() {
        List<Class<? extends Enum<?>>> enums = EnumUtil.scanEnums("io.github.atengk.enums.sample.scanned");
        assertTrue(enums.contains(ScannedStatusEnum.class));
        assertTrue(EnumUtil.scanEnums(List.of("io.github.atengk.enums.sample.scanned")).contains(ScannedStatusEnum.class));
    }

    @Test
    void shouldScanByInterfaceAndAnnotation() {
        assertTrue(EnumUtil.scanEnumsByInterface("io.github.atengk.enums.sample.scanned", FrontendEnum.class).contains(ScannedStatusEnum.class));
        assertTrue(EnumUtil.scanEnumsByAnnotation("io.github.atengk.enums.sample.scanned", EnumDict.class).contains(ScannedStatusEnum.class));
        assertTrue(EnumUtil.scanEnumClasses("io.github.atengk.enums.sample.scanned", false).contains(ScannedStatusEnum.class));
    }

    @Test
    void shouldScanDictMap() {
        assertFalse(EnumUtil.scanEnumDicts("io.github.atengk.enums.sample.scanned").isEmpty());
        assertTrue(EnumUtil.scanEnumDictMap("io.github.atengk.enums.sample.scanned").containsKey("scanned-status"));
    }

    @Test
    void shouldRegisterEnums() {
        EnumUtil.registerEnum(ScannedStatusEnum.class);
        assertTrue(EnumUtil.getRegisteredEnums().containsKey("scanned-status"));
        Map<String, ?> dictMap = EnumUtil.getRegisteredEnumDictMap();
        assertTrue(dictMap.containsKey("scanned-status"));
        EnumUtil.refreshEnumRegistry();
        assertTrue(EnumUtil.getRegisteredEnums().containsKey("scanned-status"));
        EnumUtil.clearEnumRegistry();
        assertTrue(EnumUtil.getRegisteredEnums().isEmpty());
    }

    @Test
    void shouldRegisterByPackageAndCollection() {
        EnumUtil.registerEnums("io.github.atengk.enums.sample.scanned");
        assertTrue(EnumUtil.getRegisteredEnums().containsKey("scanned-status"));
        EnumUtil.clearEnumRegistry();
        EnumUtil.registerEnums(List.of(ScannedStatusEnum.class, String.class));
        assertTrue(EnumUtil.getRegisteredEnums().containsKey("scanned-status"));
    }

    @Test
    void shouldRejectBlankPackage() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.scanEnumClasses(""));
    }
}
