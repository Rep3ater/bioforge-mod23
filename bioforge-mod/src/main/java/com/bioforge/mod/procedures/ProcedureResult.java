package com.bioforge.mod.procedures;

import com.bioforge.mod.network.PlayProcedureEffectPacket;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable result from executing a surgical procedure.
 * Carries success state, a message for the surgeon, and optional FX type.
 */
public record ProcedureResult(
        boolean success,
        String message,
        @Nullable PlayProcedureEffectPacket.EffectType fxType
) {
    public static ProcedureResult success(String message, @Nullable PlayProcedureEffectPacket.EffectType fxType) {
        return new ProcedureResult(true, message, fxType);
    }

    public static ProcedureResult fail(String message) {
        return new ProcedureResult(false, message, null);
    }
}
