package com.gamma.spool.asm;

class Names {

    static class Targets {

        static final String INIT = "<init>";

        static final String CHUNK = "net/minecraft/world/chunk/Chunk";
        static final String CHUNK_OBF = "apx";

        static final String EMPTY_CHUNK = "net/minecraft/world/chunk/EmptyChunk";
        static final String EMPTY_CHUNK_OBF = "apw";

        static final String NIBBLE = "net/minecraft/world/chunk/NibbleArray";
        static final String NIBBLE_OBF = "apv";
        static final String DATA_FIELD = "data";
        static final String DATA_FIELD_OBF = "field_76585_a";

        static final String EBS_OBF = "apz";
        static final String EBS = "net/minecraft/world/chunk/storage/ExtendedBlockStorage";
    }

    static class Destinations {

        static final String ATOMIC_BOOLEAN = "java/util/concurrent/atomic/AtomicBoolean";
        static final String ATOMIC_INTEGER = "java/util/concurrent/atomic/AtomicInteger";

        static final String CONCURRENT_CHUNK = "com/gamma/spool/concurrent/ConcurrentChunk";

        static final String ATOMIC_NIBBLE = "com/gamma/spool/concurrent/AtomicNibbleArray";
        static final String ATOMIC_DATA_FUNC = "getByteArray";

        static final String CONCURRENT_EBS = "com/gamma/spool/concurrent/ConcurrentExtendedBlockStorage";
    }

    static class DataTypes {

        static final String BOOLEAN = "Z";
        static final String INTEGER = "I";
        static final String LONG = "J";
        static final String FLOAT = "F";
        static final String DOUBLE = "D";
        static final String CHAR = "C";
        static final String BYTE = "B";
        static final String SHORT = "S";

        static final String BOOLEAN_ARRAY = "[Z";
        static final String INTEGER_ARRAY = "[I";
        static final String LONG_ARRAY = "[J";
        static final String FLOAT_ARRAY = "[F";
        static final String DOUBLE_ARRAY = "[D";
        static final String CHAR_ARRAY = "[C";
        static final String BYTE_ARRAY = "[B";
        static final String SHORT_ARRAY = "[S";
    }
}
