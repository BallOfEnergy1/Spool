package com.gamma.spool.asm.interfaces;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public interface ITransformer {

    /**
     * The constant pool to define what classes to target in ASMed classes.
     */
    ClassConstantPoolParser getTargetClasses();
}
