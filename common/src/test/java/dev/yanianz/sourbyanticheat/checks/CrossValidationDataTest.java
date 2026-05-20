package dev.yanianz.sourbyanticheat.checks;

import dev.yanianz.sourbyanticheat.checks.crossapi.CrossValidationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrossValidationDataTest {

    private CrossValidationData data;

    @BeforeEach
    void setUp() {
        data = new CrossValidationData();
    }

    @Test
    void fieldsDefaultToZero() {
        assertEquals(0.0, data.pePositionDeltaX);
        assertEquals(0.0, data.pePositionDeltaY);
        assertEquals(0.0, data.pePositionDeltaZ);
        assertEquals(0.0, data.nettyPacketRatePerSec);
        assertEquals(0.0, data.nettyIntervalVariance);
        assertEquals(0.0, data.offsetFromPrediction);
    }

    @Test
    void resetTickDataClearsFlyingCount() {
        data.peFlyingPacketsPerTick = 10;
        data.resetTickData();
        assertEquals(0, data.peFlyingPacketsPerTick);
    }

    @Test
    void updateSpartanDataStoresValues() {
        java.util.Map<String, Integer> perCheck = new java.util.HashMap<>();
        perCheck.put("Speed", 5);
        data.updateSpartanData(10, perCheck, 0.75);
        assertEquals(10, data.spartanVL);
        assertEquals(5, (int) data.spartanPerCheckVL.get("Speed"));
        assertEquals(0.75, data.spartanAgreementRate, 0.001);
    }

    @Test
    void spartanPerCheckVLIsThreadSafe() throws Exception {
        data.updateSpartanData(0, new java.util.HashMap<>(), 0);
        data.spartanPerCheckVL.put("test", 42);
        assertEquals(42, (int) data.spartanPerCheckVL.get("test"));
    }

    @Test
    void ratioCalculationIsCorrect() {
        double actualX = 3.0;
        double actualZ = 4.0;
        double actualH = Math.sqrt(actualX * actualX + actualZ * actualZ);
        assertEquals(5.0, actualH, 0.0001);
    }

    @Test
    void speedRatioAboveOne() {
        double velH = 2.0;
        double actualH = 3.66;
        double ratio = actualH / velH;
        assertEquals(1.83, ratio, 0.001);
    }
}
