package com.gamma.spool.asm;

class Names {

    static class Targets {

        static final String INIT = "<init>";

        static final String MIXINS = "com/gamma/spool/mixin";
        static final String ASM = "com/gamma/spool/asm";

        static final String CHUNK = "net/minecraft/world/chunk/Chunk";
        static final String CHUNK_OBF = "apx";

        static final String EMPTY_CHUNK = "net/minecraft/world/chunk/EmptyChunk";
        static final String EMPTY_CHUNK_OBF = "apw";

        static final String NIBBLE = "net/minecraft/world/chunk/NibbleArray";
        static final String NIBBLE_OBF = "apv";
        static final String DATA_FIELD = "data";
        static final String DATA_FIELD_OBF = "a";

        static final String EBS = "net/minecraft/world/chunk/storage/ExtendedBlockStorage";
        static final String EBS_OBF = "apz";

        static final String CHUNK_PROVIDER_SERVER = "net/minecraft/world/gen/ChunkProviderServer";
        static final String CHUNK_PROVIDER_SERVER_OBF = "ms";
        static final String CHUNK_PROVIDER_CLIENT = "net/minecraft/client/multiplayer/ChunkProviderClient";
        static final String CHUNK_PROVIDER_CLIENT_OBF = "bjd";
        static final String CHUNK_PROVIDER_FLAT = "net/minecraft/world/gen/ChunkProviderFlat";
        static final String CHUNK_PROVIDER_FLAT_OBF = "aqu";
        static final String CHUNK_PROVIDER_GENERATE = "net/minecraft/world/gen/ChunkProviderGenerate";
        static final String CHUNK_PROVIDER_GENERATE_OBF = "aqz";
        static final String CHUNK_PROVIDER_HELL = "net/minecraft/world/gen/ChunkProviderHell";
        static final String CHUNK_PROVIDER_HELL_OBF = "aqv";
        static final String CHUNK_PROVIDER_END = "net/minecraft/world/gen/ChunkProviderEnd";
        static final String CHUNK_PROVIDER_END_OBF = "ara";
    }

    static class Destinations {

        static final String ATOMIC_BOOLEAN = "java/util/concurrent/atomic/AtomicBoolean";
        static final String ATOMIC_INTEGER = "java/util/concurrent/atomic/AtomicInteger";

        static final String CONCURRENT_CHUNK = "com/gamma/spool/concurrent/ConcurrentChunk";

        static final String ATOMIC_NIBBLE = "com/gamma/spool/concurrent/AtomicNibbleArray";
        static final String ATOMIC_DATA_FUNC = "getByteArray";

        static final String CONCURRENT_EBS = "com/gamma/spool/concurrent/ConcurrentExtendedBlockStorage";

        static final String CONCURRENT_CHUNK_PROVIDER_SERVER = "com/gamma/spool/concurrent/providers/ConcurrentChunkProviderServer";
        static final String CONCURRENT_CHUNK_PROVIDER_CLIENT = "com/gamma/spool/concurrent/providers/ConcurrentChunkProviderClient";
        static final String CONCURRENT_CHUNK_PROVIDER_FLAT = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderFlat";
        static final String CONCURRENT_CHUNK_PROVIDER_GENERATE = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderGenerate";
        static final String CONCURRENT_CHUNK_PROVIDER_HELL = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderHell";
        static final String CONCURRENT_CHUNK_PROVIDER_END = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderEnd";
    }

    @SuppressWarnings("unused")
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
