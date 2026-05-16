package dev.yanianz.sourbyanticheat.checks.crossapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrossValidationData {

    // PacketEvents layer
    public double pePositionDeltaX;
    public double pePositionDeltaY;
    public double pePositionDeltaZ;
    public double peRotationDeltaYaw;
    public double peRotationDeltaPitch;
    public long pePacketIntervalMs;
    public int peFlyingPacketsPerTick;

    // Netty layer
    public double nettyPacketRatePerSec;
    public double nettyAvgReadBytesPerPacket;
    public double nettyAvgDelayBetweenPacketsMs;

    // Spartan layer
    public int spartanVL;
    public final Map<String, Integer> spartanPerCheckVL = new ConcurrentHashMap<>();
    public double spartanAgreementRate;

    // SACAPI layer (internal prediction)
    public double predictedDeltaX;
    public double predictedDeltaY;
    public double predictedDeltaZ;
    public double offsetFromPrediction;
    public double uncertaintyFactor;

    public void resetTickData() {
        peFlyingPacketsPerTick = 0;
    }

    public void updateSpartanData(int totalVL, Map<String, Integer> perCheckVL, double agreementRate) {
        this.spartanVL = totalVL;
        this.spartanPerCheckVL.clear();
        this.spartanPerCheckVL.putAll(perCheckVL);
        this.spartanAgreementRate = agreementRate;
    }
}