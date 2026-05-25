// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

/** Pure geometry helpers for the combat angle checks (unit-testable, no deps). */
public final class KillAuraMath {

    private KillAuraMath() {}

    /**
     * Angle (degrees) between a look vector and a direction-to-target vector.
     * Returns 0 for degenerate (zero-length) inputs. Result is clamped so
     * floating-point drift can never produce NaN from {@code acos}.
     */
    public static double angleDegrees(double lookX, double lookY, double lookZ,
                                      double dx, double dy, double dz) {
        double lookLen = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (lookLen < 1e-6 || dLen < 1e-6) return 0.0;
        double cos = (lookX * dx + lookY * dy + lookZ * dz) / (lookLen * dLen);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return Math.toDegrees(Math.acos(cos));
    }
}
