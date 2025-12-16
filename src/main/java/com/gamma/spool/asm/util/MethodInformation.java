package com.gamma.spool.asm.util;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.bsideup.jabel.Desugar;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

@Desugar
public record MethodInformation(MethodNode methodNode, ClassNode classNode,
    Int2ObjectMap<LocalVariableNode> localsMap) {}
