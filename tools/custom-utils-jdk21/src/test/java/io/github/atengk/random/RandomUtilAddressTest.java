package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilAddressTest {

    @Test
    void shouldGenerateAddressAndCoordinate() {
        assertFalse(RandomUtil.randomProvince().isBlank());
        assertFalse(RandomUtil.randomCity().isBlank());
        assertFalse(RandomUtil.randomAddress().isBlank());

        double longitude = RandomUtil.randomLongitude();
        double latitude = RandomUtil.randomLatitude();
        assertTrue(longitude >= -180.0 && longitude < 180.0);
        assertTrue(latitude >= -90.0 && latitude < 90.0);

        RandomUtil.Coordinate coordinate = RandomUtil.randomCoordinate();
        assertTrue(coordinate.getLongitude() >= -180.0 && coordinate.getLongitude() <= 180.0);
        assertTrue(coordinate.getLatitude() >= -90.0 && coordinate.getLatitude() <= 90.0);
        assertTrue(coordinate.toString().contains("longitude"));
    }

    @Test
    void shouldGenerateCoordinateWithinCustomRange() {
        RandomUtil.Coordinate coordinate = RandomUtil.randomCoordinate(100.0, 101.0, 30.0, 31.0);
        assertTrue(coordinate.getLongitude() >= 100.0 && coordinate.getLongitude() < 101.0);
        assertTrue(coordinate.getLatitude() >= 30.0 && coordinate.getLatitude() < 31.0);
    }

    @Test
    void shouldRejectInvalidCoordinate() {
        assertThrows(IllegalArgumentException.class, () -> new RandomUtil.Coordinate(181.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new RandomUtil.Coordinate(0.0, 91.0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomCoordinate(101.0, 100.0, 30.0, 31.0));
    }
}
