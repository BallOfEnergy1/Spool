package com.gamma.spool.asm.interfaces;

import org.objectweb.asm.tree.ClassNode;

public interface ICheckTransformer extends ITransformer {

    /**
     * Performs ASM checks in a certain class node.
     *
     * @return Was the class changed?
     */
    boolean performCheck(String transformedName, ClassNode mn, byte[] bytecode);

    /**
     * Returns the internal name of the annotation enum that this check falls under (for skipping checks via
     * annotations).
     * 
     * @return The internal name of the annotation this check falls under.
     */
    String getAnnotationCheckName();
}
