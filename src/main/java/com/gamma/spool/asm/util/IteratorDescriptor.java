package com.gamma.spool.asm.util;

import org.objectweb.asm.tree.AbstractInsnNode;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record IteratorDescriptor(StackRebuilder rebuilder, MethodInformation methodInfo, AbstractInsnNode start,
    AbstractInsnNode comparisonNode, AbstractInsnNode end) {}
