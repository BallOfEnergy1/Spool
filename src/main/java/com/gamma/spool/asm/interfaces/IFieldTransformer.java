package com.gamma.spool.asm.interfaces;

import org.objectweb.asm.tree.MethodNode;

public interface IFieldTransformer extends ITransformer {

    /**
     * Transforms field accessors in a certain method node.
     *
     * @return Was the class changed?
     */
    boolean transformFieldAccesses(String transformedName, MethodNode mn);

    /**
     * The classes to be excluded from field accessor transformation.
     */
    String[] getExcludedClassNodes();
}
