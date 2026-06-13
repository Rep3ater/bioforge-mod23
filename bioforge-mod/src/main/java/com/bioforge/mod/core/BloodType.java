package com.bioforge.mod.core;

import java.util.Arrays;
import java.util.List;

/**
 * ABO + Rh blood type system.
 * Used for transfusion compatibility checks, organ rejection modifiers,
 * and roleplay identification purposes.
 */
public enum BloodType {

    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-"),
    UNKNOWN("?");

    private final String display;

    BloodType(String display) {
        this.display = display;
    }

    public String getDisplay() { return display; }

    /**
     * Returns true if this blood type can RECEIVE from the given donor type.
     * Uses real ABO/Rh compatibility rules.
     */
    public boolean canReceiveFrom(BloodType donor) {
        if (this == UNKNOWN || donor == UNKNOWN) return true; // unknown = skip check
        return switch (this) {
            case O_NEG  -> donor == O_NEG;
            case O_POS  -> donor == O_NEG || donor == O_POS;
            case A_NEG  -> donor == O_NEG || donor == A_NEG;
            case A_POS  -> donor == O_NEG || donor == O_POS || donor == A_NEG || donor == A_POS;
            case B_NEG  -> donor == O_NEG || donor == B_NEG;
            case B_POS  -> donor == O_NEG || donor == O_POS || donor == B_NEG || donor == B_POS;
            case AB_NEG -> donor == O_NEG || donor == A_NEG || donor == B_NEG || donor == AB_NEG;
            case AB_POS -> true; // universal recipient
            default     -> false;
        };
    }

    /**
     * Returns the rejection risk modifier for organ transplants (0 = none, 1 = max).
     * Incompatible types add 0.4 base rejection; slightly mismatched add 0.2.
     */
    public float transplantRejectionModifier(BloodType donorType) {
        if (canReceiveFrom(donorType)) return 0f;
        // Rh mismatch only (ABO compatible)
        boolean aboSame = aboGroup() == donorType.aboGroup();
        return aboSame ? 0.15f : 0.4f;
    }

    private String aboGroup() {
        return switch (this) {
            case A_POS, A_NEG -> "A";
            case B_POS, B_NEG -> "B";
            case AB_POS, AB_NEG -> "AB";
            case O_POS, O_NEG -> "O";
            default -> "?";
        };
    }

    public static BloodType fromString(String s) {
        for (BloodType bt : values()) {
            if (bt.name().equalsIgnoreCase(s) || bt.display.equalsIgnoreCase(s)) return bt;
        }
        return UNKNOWN;
    }

    /** Random blood type weighted by real-world prevalence */
    public static BloodType random(net.minecraft.util.RandomSource rng) {
        // Approx global frequencies
        double[] weights = {0.28, 0.06, 0.20, 0.02, 0.05, 0.01, 0.36, 0.07};
        BloodType[] types = {A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG};
        double r = rng.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (r < cumulative) return types[i];
        }
        return O_POS;
    }

    public static List<BloodType> all() {
        return Arrays.asList(A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG);
    }
}
