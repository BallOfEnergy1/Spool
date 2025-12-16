package com.gamma.spool.asm.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.BytecodeHelper;
import com.gamma.spool.asm.Names;
import com.gamma.spool.core.SpoolCompat;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class StackRebuilder {

    private final InsnList methodInsnList;
    private final StackRebuilderCache cache = new StackRebuilderCache();
    private final MethodInformation methodInfo;
    private boolean underflow = false;

    public StackRebuilder(MethodInformation methodInfo, InsnList methodInstructions) {
        this.methodInfo = methodInfo;
        methodInsnList = methodInstructions;
    }

    public StackView getStackAtMethod(AbstractInsnNode node) {
        return cache.getStackForNode(node);
    }

    public StackView getStackAtMethod(AbstractInsnNode endNode, StackView startingWith, AbstractInsnNode startingAt) {
        underflow = false;
        if (!methodInsnList.contains(endNode)) return null;

        StackView stack = startingWith == null ? new StackView() : startingWith.clone();
        int idx = startingAt == null ? 0 : (methodInsnList.indexOf(startingAt) + 1);

        int endIdx = methodInsnList.indexOf(endNode);

        if (endIdx <= 0) return null;

        Int2ObjectMap<String> localsToAdd = new Int2ObjectOpenHashMap<>();

        // Starting from a cached entry means we'll have these already prepared, no need to do it again.
        if (startingWith == null) {
            // Local preparations.
            int index = 0;
            if ((methodInfo.methodNode().access & Opcodes.ACC_STATIC) == 0)
                localsToAdd.put(index++, methodInfo.classNode().name);
            ObjectList<String> methodArgs = getParametersFromDesc(methodInfo.methodNode().desc);
            for (String arg : methodArgs) {
                localsToAdd.put(index++, arg);
            }
        }

        stack.localList.putAll(localsToAdd);

        AbstractInsnNode node;

        for (; idx < endIdx; idx++) {

            node = methodInsnList.get(idx); // get next node in method instructions

            if (node instanceof FrameNode frameNode) {
                switch (frameNode.type) {
                    case (Opcodes.F_NEW):
                    case (Opcodes.F_FULL): {
                        stack.clearStack();
                        for (Object s : frameNode.stack) {
                            if (s instanceof Integer) stack.pushToStack(Names.DataTypes.INTEGER);
                            else if (s instanceof String string) stack.pushToStack(string);
                            else throw new IllegalStateException("Unexpected value: " + s + " at index " + idx);
                        }
                        break;
                    }
                    case (Opcodes.F_APPEND): {
                        stack.clearStack();
                        for (int i = 0; i < frameNode.local.size(); i++) {
                            Object obj = frameNode.stack.get(i);
                            if (obj instanceof Integer)
                                stack.localList.put(stack.localList.size() - 1, Names.DataTypes.INTEGER);
                            else if (obj instanceof String string)
                                stack.localList.put(stack.localList.size() - 1, string);
                            else throw new IllegalStateException("Unexpected value: " + obj + " at index " + idx);
                        }
                        break;
                    }
                    case (Opcodes.F_CHOP): {
                        stack.clearStack();
                        for (int i = 0; i < frameNode.local.size(); i++) {
                            stack.localList.remove(stack.localList.size() - 1);
                        }
                        stack.localList.putAll(localsToAdd);
                    }
                    case (Opcodes.F_SAME): {
                        stack.clearStack();
                        break;
                    }
                    case (Opcodes.F_SAME1): {
                        stack.clearStack();
                        Object obj = frameNode.stack.get(0);
                        if (obj instanceof Integer) stack.pushToStack(Names.DataTypes.INTEGER);
                        else if (obj instanceof String string) stack.pushToStack(string);
                        else throw new IllegalStateException("Unexpected value: " + obj + " at index " + idx);
                        break;
                    }
                }
            } else {
                switch (node.getOpcode()) {
                    case (Opcodes.ATHROW): {
                        String throwable = stack.popFromStack();
                        stack.clearStack();
                        stack.pushToStack(throwable);
                        break; // objectRef:[empty], objectRef
                    }
                    case (Opcodes.RETURN):
                    case (Opcodes.ARETURN):
                    case (Opcodes.DRETURN):
                    case (Opcodes.FRETURN):
                    case (Opcodes.IRETURN):
                    case (Opcodes.LRETURN): {
                        stack.clearStack();
                        break; // 0:empty
                    }
                    case (Opcodes.GOTO):
                    case (Opcodes.IINC):
                    case (Opcodes.NOP):
                    case (Opcodes.RET): {
                        break; // 0:0
                    }
                    case (Opcodes.ASTORE):
                    case (Opcodes.DSTORE):
                    case (Opcodes.FSTORE):
                    case (Opcodes.ISTORE):
                    case (Opcodes.LSTORE): {
                        VarInsnNode varInsnNode = (VarInsnNode) node;
                        String value = stack.popFromStack();
                        stack.localList.put(varInsnNode.var, value);
                        break; // objectRef:0
                    }
                    case (Opcodes.MONITORENTER):
                    case (Opcodes.MONITOREXIT):
                    case (Opcodes.IFEQ):
                    case (Opcodes.IFGE):
                    case (Opcodes.IFGT):
                    case (Opcodes.IFLE):
                    case (Opcodes.IFLT):
                    case (Opcodes.IFNE):
                    case (Opcodes.IFNONNULL):
                    case (Opcodes.IFNULL):
                    case (Opcodes.LOOKUPSWITCH):
                    case (Opcodes.POP):
                    case (Opcodes.PUTSTATIC):
                    case (Opcodes.TABLESWITCH): {
                        stack.popFromStack();
                        break; // 1:0
                    }
                    case (Opcodes.IF_ACMPEQ):
                    case (Opcodes.IF_ACMPNE):
                    case (Opcodes.IF_ICMPEQ):
                    case (Opcodes.IF_ICMPGE):
                    case (Opcodes.IF_ICMPGT):
                    case (Opcodes.IF_ICMPLE):
                    case (Opcodes.IF_ICMPLT):
                    case (Opcodes.IF_ICMPNE):
                    case (Opcodes.POP2):
                    case (Opcodes.PUTFIELD): {
                        stack.popFromStack(2);
                        break; // 2:0
                    }
                    case (Opcodes.AASTORE):
                    case (Opcodes.BASTORE):
                    case (Opcodes.CASTORE):
                    case (Opcodes.DASTORE):
                    case (Opcodes.FASTORE):
                    case (Opcodes.IASTORE):
                    case (Opcodes.LASTORE): {
                        stack.popFromStack(3);
                        break; // 3:0
                    }
                    case (Opcodes.ALOAD):
                    case (Opcodes.DLOAD):
                    case (Opcodes.FLOAD):
                    case (Opcodes.ILOAD):
                    case (Opcodes.LLOAD): {
                        VarInsnNode varInsnNode = (VarInsnNode) node;
                        stack.pushToStack(stack.localList.get(varInsnNode.var));
                        break; // 0:1
                    }
                    case (Opcodes.ACONST_NULL): {
                        stack.pushToStack(Type.VOID_TYPE.getDescriptor());
                        break; // 0:1
                    }
                    case (Opcodes.ICONST_M1):
                    case (Opcodes.ICONST_2):
                    case (Opcodes.ICONST_3):
                    case (Opcodes.ICONST_1):
                    case (Opcodes.ICONST_4):
                    case (Opcodes.ICONST_0):
                    case (Opcodes.ICONST_5):
                    case (Opcodes.BIPUSH):
                    case (Opcodes.SIPUSH): {
                        stack.pushToStack(Names.DataTypes.INTEGER);
                        break; // 0:1
                    }
                    case (Opcodes.LCONST_0):
                    case (Opcodes.LCONST_1): {
                        stack.pushToStack(Names.DataTypes.LONG);
                        break; // 0:1
                    }
                    case (Opcodes.DCONST_0):
                    case (Opcodes.DCONST_1): {
                        stack.pushToStack(Names.DataTypes.DOUBLE);
                        break; // 0:1
                    }
                    case (Opcodes.FCONST_0):
                    case (Opcodes.FCONST_2):
                    case (Opcodes.FCONST_1): {
                        stack.pushToStack(Names.DataTypes.FLOAT);
                        break; // 0:1
                    }
                    case (Opcodes.NEW): {
                        TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                        stack.pushToStack(typeInsnNode.desc);
                        break; // 0:1
                    }
                    case (Opcodes.LDC): {
                        LdcInsnNode ldcInsnNode = (LdcInsnNode) node;
                        if (ldcInsnNode.cst instanceof Type) {
                            stack.pushToStack(((Type) ldcInsnNode.cst).getDescriptor());
                        } else if (ldcInsnNode.cst instanceof String) {
                            stack.pushToStack("Ljava/lang/String;");
                        } else if (ldcInsnNode.cst instanceof Integer) {
                            stack.pushToStack(Names.DataTypes.INTEGER);
                        } else if (ldcInsnNode.cst instanceof Float) {
                            stack.pushToStack(Names.DataTypes.FLOAT);
                        } else if (ldcInsnNode.cst instanceof Long) {
                            stack.pushToStack(Names.DataTypes.LONG);
                        } else if (ldcInsnNode.cst instanceof Double) {
                            stack.pushToStack(Names.DataTypes.DOUBLE);
                        }
                        break; // 0:1
                    }
                    case (Opcodes.GETSTATIC): {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                        stack.pushToStack(fieldInsnNode.desc);
                        break; // 0:1
                    }
                    case (Opcodes.JSR): {
                        stack.pushToStack("RETURN");
                        break; // 0:1
                    }
                    case (Opcodes.CHECKCAST):
                    case (Opcodes.ANEWARRAY): {
                        TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                        stack.popFromStack();
                        stack.pushToStack(typeInsnNode.desc);
                        break;
                    }
                    case (Opcodes.L2I):
                    case (Opcodes.F2I):
                    case (Opcodes.D2I):
                    case (Opcodes.INEG):
                    case (Opcodes.ARRAYLENGTH): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.INTEGER);
                        break;
                    }
                    case (Opcodes.I2B): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.BYTE);
                        break;
                    }
                    case (Opcodes.I2C): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.CHAR);
                        break;
                    }
                    case (Opcodes.I2S): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.SHORT);
                        break;
                    }
                    case (Opcodes.I2L):
                    case (Opcodes.F2L):
                    case (Opcodes.D2L):
                    case (Opcodes.LNEG): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.LONG);
                        break;
                    }
                    case (Opcodes.I2F):
                    case (Opcodes.L2F):
                    case (Opcodes.D2F):
                    case (Opcodes.FNEG): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.FLOAT);
                        break;
                    }
                    case (Opcodes.I2D):
                    case (Opcodes.L2D):
                    case (Opcodes.F2D):
                    case (Opcodes.DNEG): {
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.DOUBLE);
                        break;
                    }
                    case (Opcodes.NEWARRAY): {
                        IntInsnNode typeInsnNode = (IntInsnNode) node;
                        String desc = NewArrayType.values()[typeInsnNode.operand].type;
                        stack.popFromStack();
                        stack.pushToStack(desc);
                        break;
                    }
                    case (Opcodes.GETFIELD):
                        FieldInsnNode typeInsnNode = (FieldInsnNode) node;
                        stack.popFromStack();
                        stack.pushToStack(typeInsnNode.desc);
                        break;
                    case (Opcodes.INSTANCEOF):
                        stack.popFromStack();
                        stack.pushToStack(Names.DataTypes.INTEGER);
                        break; // 1:1
                    case (Opcodes.DUP):
                        stack.duplicateTop();
                        break; // 1:2
                    case (Opcodes.AALOAD): {
                        String arrayRef = stack.popFromStack();
                        stack.popFromStack();
                        stack.pushToStack(arrayRef.substring(1));
                        break;
                    }
                    case (Opcodes.BALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.BYTE);
                        break;
                    }
                    case (Opcodes.CALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.CHAR);
                        break;
                    }
                    case (Opcodes.SALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.SHORT);
                        break;
                    }
                    case (Opcodes.IADD):
                    case (Opcodes.IAND):
                    case (Opcodes.IDIV):
                    case (Opcodes.IMUL):
                    case (Opcodes.IOR):
                    case (Opcodes.IREM):
                    case (Opcodes.ISHL):
                    case (Opcodes.ISHR):
                    case (Opcodes.ISUB):
                    case (Opcodes.IUSHR):
                    case (Opcodes.IXOR):
                    case (Opcodes.IALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.INTEGER);
                        break;
                    }
                    case (Opcodes.LADD):
                    case (Opcodes.LAND):
                    case (Opcodes.LCMP):
                    case (Opcodes.LDIV):
                    case (Opcodes.LMUL):
                    case (Opcodes.LOR):
                    case (Opcodes.LREM):
                    case (Opcodes.LSHL):
                    case (Opcodes.LSHR):
                    case (Opcodes.LSUB):
                    case (Opcodes.LUSHR):
                    case (Opcodes.LXOR):
                    case (Opcodes.LALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.LONG);
                        break;
                    }
                    case (Opcodes.FADD):
                    case (Opcodes.FCMPG):
                    case (Opcodes.FCMPL):
                    case (Opcodes.FDIV):
                    case (Opcodes.FMUL):
                    case (Opcodes.FREM):
                    case (Opcodes.FSUB):
                    case (Opcodes.FALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.FLOAT);
                        break;
                    }
                    case (Opcodes.DADD):
                    case (Opcodes.DCMPG):
                    case (Opcodes.DCMPL):
                    case (Opcodes.DDIV):
                    case (Opcodes.DMUL):
                    case (Opcodes.DREM):
                    case (Opcodes.DSUB):
                    case (Opcodes.DALOAD): {
                        stack.popFromStack(2);
                        stack.pushToStack(Names.DataTypes.DOUBLE);
                        break;
                    }
                    case (Opcodes.SWAP): {
                        String[] entries = stack.popFromStack(2);
                        stack.pushToStack(entries[0], entries[1]);
                        break; // 2:2
                    }
                    case (Opcodes.DUP_X1): {
                        stack.duplicateTop();
                        String[] topOfStack = stack.popFromStack(3);
                        stack.pushToStack(topOfStack[0], topOfStack[2], topOfStack[1]);
                        break; // 2:3
                    }
                    case (Opcodes.DUP2): {
                        String top = stack.peekAtTop();
                        boolean type2 = BytecodeHelper.isType2ComputationalType(top);
                        stack.duplicateTop(!type2);
                        break; // 2:4
                    }
                    case (Opcodes.INVOKEDYNAMIC): {
                        InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) node;
                        String internalDesc = invokeDynamicInsnNode.desc
                            .substring(1, invokeDynamicInsnNode.desc.indexOf(")"));
                        ObjectList<String> args = getParametersFromDesc(internalDesc);
                        stack.popFromStack(args.size());
                        String outputDesc = invokeDynamicInsnNode.desc
                            .substring(invokeDynamicInsnNode.desc.indexOf(")") + 1);
                        if (!outputDesc.isEmpty() && !outputDesc.equals("V")) stack.pushToStack(outputDesc);
                        break;
                    }
                    case (Opcodes.INVOKESTATIC): {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                        String internalDesc = methodInsnNode.desc.substring(1, methodInsnNode.desc.indexOf(")"));
                        ObjectList<String> args = getParametersFromDesc(internalDesc);
                        stack.popFromStack(args.size());
                        String outputDesc = methodInsnNode.desc.substring(methodInsnNode.desc.indexOf(")") + 1);
                        if (!outputDesc.isEmpty() && !outputDesc.equals("V")) stack.pushToStack(outputDesc);
                        break; // 3:0
                    }
                    case (Opcodes.MULTIANEWARRAY): {
                        MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) node;
                        stack.popFromStack(multiANewArrayInsnNode.dims);
                        stack.pushToStack(multiANewArrayInsnNode.desc);
                        break; // 3:1
                    }
                    case (Opcodes.DUP_X2): {
                        String value1 = stack.popFromStack();
                        String value2 = stack.popFromStack();
                        if (!BytecodeHelper.isType2ComputationalType(value1)
                            && !BytecodeHelper.isType2ComputationalType(value2)) {
                            String value3 = stack.popFromStack();
                            stack.pushToStack(value1, value3, value2, value1);
                        } else if (BytecodeHelper.isType2ComputationalType(value2)) {
                            stack.pushToStack(value1, value2, value1);
                        } else {
                            // pmo :wilted_rose:
                            throw new IllegalStateException("Attempted to DUP_X2 with illegal operands");
                        }
                        break; // 3:4
                    }
                    case (Opcodes.DUP2_X1): {
                        String value1 = stack.popFromStack();
                        String value2 = stack.popFromStack();
                        if (!BytecodeHelper.isType2ComputationalType(value1)
                            && !BytecodeHelper.isType2ComputationalType(value2)) {
                            String value3 = stack.popFromStack();
                            stack.pushToStack(value2, value1, value3, value2, value1);
                        } else if (BytecodeHelper.isType2ComputationalType(value1)) {
                            stack.pushToStack(value1, value2, value1);
                        } else {
                            // pmo :wilted_rose:
                            throw new IllegalStateException("Attempted to DUP2_X2 with illegal operands");
                        }
                        break; // 3:5
                    }
                    case (Opcodes.INVOKEINTERFACE):
                    case (Opcodes.INVOKESPECIAL):
                    case (Opcodes.INVOKEVIRTUAL): {
                        stack.popFromStack();
                        MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                        String internalDesc = methodInsnNode.desc.substring(1, methodInsnNode.desc.indexOf(")"));
                        ObjectList<String> args = getParametersFromDesc(internalDesc);
                        stack.popFromStack(args.size());
                        String outputDesc = methodInsnNode.desc.substring(methodInsnNode.desc.indexOf(")") + 1);
                        if (!outputDesc.isEmpty() && !outputDesc.equals("V")) stack.pushToStack(outputDesc);
                        break; // 4:1
                    }
                    case (Opcodes.DUP2_X2): {
                        String value1 = stack.popFromStack();
                        String value2 = stack.popFromStack();
                        if (BytecodeHelper.isType2ComputationalType(value1)
                            && BytecodeHelper.isType2ComputationalType(value2)) {
                            stack.pushToStack(value1, value2, value1);
                            break;
                        }
                        String value3 = stack.popFromStack();
                        if (!BytecodeHelper.isType2ComputationalType(value1)
                            && !BytecodeHelper.isType2ComputationalType(value2)
                            && !BytecodeHelper.isType2ComputationalType(value3)) {
                            String value4 = stack.popFromStack();
                            stack.pushToStack(value2, value1, value4, value3, value2, value1);
                        } else if (BytecodeHelper.isType2ComputationalType(value1)
                            && !BytecodeHelper.isType2ComputationalType(value2)
                            && !BytecodeHelper.isType2ComputationalType(value3)) {
                                stack.pushToStack(value1, value3, value2, value1);
                            } else if (!BytecodeHelper.isType2ComputationalType(value1)
                                && !BytecodeHelper.isType2ComputationalType(value2)
                                && BytecodeHelper.isType2ComputationalType(value3)) {
                                    stack.pushToStack(value2, value1, value3, value2, value1);
                                } else {
                                    // pmo :wilted_rose:
                                    throw new IllegalStateException("Attempted to DUP2_X2 with illegal operands");
                                }
                        break; // 4:6
                    }
                }
            }

            if (underflow) {
                cache.putInCache(node, null);
                return null;
            } else cache.putInCache(node, stack.clone());
        }

        if (underflow) {
            cache.putInCache(endNode, null);
            return null;
        } else cache.putInCache(endNode, stack.clone());

        return stack;
    }

    private static ObjectList<String> getParametersFromDesc(String internalDesc) {
        ObjectList<String> args = new ObjectArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean isInReference = false;
        boolean isInArray = false;
        for (char c : internalDesc.toCharArray()) {
            if (isInReference) {
                buffer.append(c);
                if (c == ';') {
                    args.add(buffer.toString());
                    buffer = new StringBuilder();
                    isInReference = false;
                    isInArray = false;
                }
            } else if (isInArray) {
                if (c == 'L') {
                    isInReference = true;
                    buffer.append(c);
                    continue;
                }
                buffer.append(c);
                args.add(buffer.toString());
                buffer = new StringBuilder();
                isInArray = false;
            } else if (c == 'L') {
                isInReference = true;
                buffer.append(c);
            } else if (c == '[') {
                isInArray = true;
                buffer.append(c);
            }
        }
        return args;
    }

    private enum NewArrayType {

        NULL0(null),
        NULL1(null),
        NULL2(null),
        NULL3(null),
        BOOLEAN(Names.DataTypes.BOOLEAN_ARRAY),
        CHAR(Names.DataTypes.CHAR_ARRAY),
        FLOAT(Names.DataTypes.FLOAT_ARRAY),
        DOUBLE(Names.DataTypes.DOUBLE_ARRAY),
        BYTE(Names.DataTypes.BYTE_ARRAY),
        SHORT(Names.DataTypes.SHORT_ARRAY),
        INT(Names.DataTypes.INTEGER_ARRAY),
        LONG(Names.DataTypes.LONG_ARRAY);

        final String type;

        NewArrayType(String type) {
            this.type = type;
        }
    }

    @SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
    public class StackView implements Cloneable {

        private int size;
        private Stack<String> itemsOnStack;
        public Int2ObjectMap<String> localList = new Int2ObjectOpenHashMap<>();

        public StackView() {
            this.size = 0;
            this.itemsOnStack = new ObjectArrayList<>();
        }

        public int size() {
            return size;
        }

        public void clearStack() {
            while (!this.itemsOnStack.isEmpty()) {
                this.itemsOnStack.pop();
            }
            size = 0;
        }

        public void duplicateTop() {
            this.itemsOnStack.push(this.itemsOnStack.peek(0));
            size++;
        }

        public void duplicateTop(boolean top2) {
            if (top2) {
                this.itemsOnStack.push(this.itemsOnStack.peek(1));
                this.itemsOnStack.push(this.itemsOnStack.peek(0));
                size += 2;
            } else {
                duplicateTop();
            }
        }

        private void onStackUnderflow() {
            SpoolCompat.logChange(
                "CHECK",
                "Error during ASM stack rebuilding",
                "Stack underflow during stack rebuild in " + methodInfo.methodNode().name
                    + " in class "
                    + methodInfo.classNode().name
                    + ". Skipping.");
            underflow = true;
        }

        public String popFromStack() {
            if (size - 1 < 0) {
                onStackUnderflow();
                return "ERR";
            }
            size--;
            return this.itemsOnStack.pop();
        }

        public String[] popFromStack(int count) {
            String[] fromStack = new String[count];
            if (size - count < 0) {
                onStackUnderflow();
                Arrays.fill(fromStack, "ERR");
                return fromStack;
            }
            for (int i = 0; i < count; i++) {
                fromStack[i] = this.itemsOnStack.pop();
            }
            size -= count;
            return fromStack;
        }

        public String peekAtTop() {
            return this.itemsOnStack.peek(0);
        }

        public String peekAtSecond() {
            return this.itemsOnStack.peek(1);
        }

        public void pushToStack(String value) {
            this.itemsOnStack.push(value);
            size++;
        }

        public void pushToStack(String... values) {
            for (String value : values) {
                this.itemsOnStack.push(value);
            }
            size += values.length;
        }

        public Stack<String> getStack() {
            return itemsOnStack;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (StackView) obj;
            return this.size == that.size && Objects.equals(this.itemsOnStack, that.itemsOnStack);
        }

        @Override
        public String toString() {
            return "StackView[" + "size=" + size + ", " + "itemsOnStack=" + itemsOnStack + ']';
        }

        @Override
        public StackView clone() {
            try {
                StackView clone = (StackView) super.clone();
                // gruh
                clone.itemsOnStack = new ObjectArrayList<>((ObjectArrayList<String>) this.itemsOnStack);
                clone.localList = new Int2ObjectOpenHashMap<>(this.localList);
                if (this.size != clone.size) throw new IllegalStateException("Failed to clone stack view.");
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public void cleanCache() {
        this.cache.cleanCache();
    }

    @SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
    protected class StackRebuilderCache {

        private final Object2ObjectMap<AbstractInsnNode, StackView> cache = Object2ObjectMaps
            .synchronize(new Object2ObjectOpenHashMap<>());

        protected void cleanCache() {
            this.cache.clear();
        }

        protected void putInCache(AbstractInsnNode node, StackView stack) {
            this.cache.put(node, stack);
        }

        protected StackView getStackForNode(AbstractInsnNode nodeDescriptor) {
            StackView view = cache.get(nodeDescriptor);
            if (view != null) return view;

            int thisNodeIndex = StackRebuilder.this.methodInsnList.indexOf(nodeDescriptor);
            int latest = -1;
            for (Map.Entry<AbstractInsnNode, StackView> cachedEntry : cache.object2ObjectEntrySet()) {
                int index = StackRebuilder.this.methodInsnList.indexOf(cachedEntry.getKey());
                if (index < thisNodeIndex) {
                    latest = Math.max(index, latest);
                }
            }

            if (latest < 0) return StackRebuilder.this.getStackAtMethod(nodeDescriptor, null, null);
            else {
                AbstractInsnNode node = StackRebuilder.this.methodInsnList.get(latest);
                if (cache.get(node) == null) return null;

                return StackRebuilder.this.getStackAtMethod(nodeDescriptor, cache.get(node), node);
            }
        }
    }
}
