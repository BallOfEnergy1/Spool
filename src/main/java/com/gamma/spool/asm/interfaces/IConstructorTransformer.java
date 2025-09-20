package com.gamma.spool.asm.interfaces;

import org.objectweb.asm.tree.MethodNode;

public interface IConstructorTransformer extends ITransformer {

    /**
     * Transforms constructors in a certain method node.
     * 
     * @return Was the class changed, `init`.
     */
    boolean[] transformConstructors(String transformedName, MethodNode mn);
}
