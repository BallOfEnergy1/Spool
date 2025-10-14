package com.gamma.spool.asm;

import org.objectweb.asm.Type;

public class Names {

    public static class Targets {

        public static final String INIT = "<init>";

        public static final String MIXINS = "com/gamma/spool/mixin";
        public static final String ASM = "com/gamma/spool/asm";

        public static final String CHUNK = "net/minecraft/world/chunk/Chunk";
        public static final String CHUNK_OBF = "apx";

        public static final String EMPTY_CHUNK = "net/minecraft/world/chunk/EmptyChunk";
        public static final String EMPTY_CHUNK_OBF = "apw";

        public static final String NIBBLE = "net/minecraft/world/chunk/NibbleArray";
        public static final String NIBBLE_OBF = "apv";
        public static final String DATA_FIELD = "data";
        public static final String DATA_FIELD_OBF = "field_76585_a";
        public static final String DATA_FIELD_OBF_MC = "a";

        public static final String EBS = "net/minecraft/world/chunk/storage/ExtendedBlockStorage";
        public static final String EBS_OBF = "apz";

        public static final String ANVIL_CHUNK_LOADER = "net/minecraft/world/chunk/storage/AnvilChunkLoader";
        public static final String ANVIL_CHUNK_LOADER_OBF = "aqk";

        public static final String CHUNK_PROVIDER_SERVER = "net/minecraft/world/gen/ChunkProviderServer";
        public static final String CHUNK_PROVIDER_SERVER_OBF = "ms";
        public static final String CHUNK_PROVIDER_CLIENT = "net/minecraft/client/multiplayer/ChunkProviderClient";
        public static final String CHUNK_PROVIDER_CLIENT_OBF = "bjd";
        public static final String CHUNK_PROVIDER_FLAT = "net/minecraft/world/gen/ChunkProviderFlat";
        public static final String CHUNK_PROVIDER_FLAT_OBF = "aqu";
        public static final String CHUNK_PROVIDER_GENERATE = "net/minecraft/world/gen/ChunkProviderGenerate";
        public static final String CHUNK_PROVIDER_GENERATE_OBF = "aqz";
        public static final String CHUNK_PROVIDER_HELL = "net/minecraft/world/gen/ChunkProviderHell";
        public static final String CHUNK_PROVIDER_HELL_OBF = "aqv";
        public static final String CHUNK_PROVIDER_END = "net/minecraft/world/gen/ChunkProviderEnd";
        public static final String CHUNK_PROVIDER_END_OBF = "ara";
    }

    public static class Destinations {

        public static final String ATOMIC_REF = "java/util/concurrent/atomic/AtomicReference";
        public static final String ATOMIC_BOOLEAN = "java/util/concurrent/atomic/AtomicBoolean";
        public static final String ATOMIC_INTEGER = "java/util/concurrent/atomic/AtomicInteger";
        public static final String ATOMIC_LONG = "java/util/concurrent/atomic/AtomicLong";

        public static final String CONCURRENT_CHUNK = "com/gamma/spool/concurrent/ConcurrentChunk";
        public static final String CONCURRENT_CHUNK_EID = "com/gamma/spool/compat/endlessids/ConcurrentChunkWrapper";

        public static final String ATOMIC_NIBBLE = "com/gamma/spool/concurrent/AtomicNibbleArray";
        public static final String ATOMIC_NIBBLE_DATA = "concurrentData";

        public static final String CONCURRENT_EBS = "com/gamma/spool/concurrent/ConcurrentExtendedBlockStorage";
        public static final String CONCURRENT_EBS_EID = "com/gamma/spool/compat/endlessids/ConcurrentExtendedBlockStorageWrapper";

        public static final String CONCURRENT_ANVIL_CHUNK_LOADER = "com/gamma/spool/concurrent/loaders/ConcurrentAnvilChunkLoader";

        public static final String CONCURRENT_CHUNK_PROVIDER_SERVER = "com/gamma/spool/concurrent/providers/ConcurrentChunkProviderServer";
        public static final String CONCURRENT_CHUNK_PROVIDER_CLIENT = "com/gamma/spool/concurrent/providers/ConcurrentChunkProviderClient";
        public static final String CONCURRENT_CHUNK_PROVIDER_FLAT = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderFlat";
        public static final String CONCURRENT_CHUNK_PROVIDER_GENERATE = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderGenerate";
        public static final String CONCURRENT_CHUNK_PROVIDER_HELL = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderHell";
        public static final String CONCURRENT_CHUNK_PROVIDER_END = "com/gamma/spool/concurrent/providers/gen/ConcurrentChunkProviderEnd";
    }

    @SuppressWarnings("unused")
    public static class DataTypes {

        public static final String BOOLEAN = Type.BOOLEAN_TYPE.getDescriptor();
        public static final String INTEGER = Type.INT_TYPE.getDescriptor();
        public static final String LONG = Type.LONG_TYPE.getDescriptor();
        public static final String FLOAT = Type.FLOAT_TYPE.getDescriptor();
        public static final String DOUBLE = Type.DOUBLE_TYPE.getDescriptor();
        public static final String CHAR = Type.CHAR_TYPE.getDescriptor();
        public static final String BYTE = Type.BYTE_TYPE.getDescriptor();
        public static final String SHORT = Type.SHORT_TYPE.getDescriptor();

        public static final String BOOLEAN_ARRAY = "[" + BOOLEAN;
        public static final String INTEGER_ARRAY = "[" + INTEGER;
        public static final String LONG_ARRAY = "[" + LONG;
        public static final String FLOAT_ARRAY = "[" + FLOAT;
        public static final String DOUBLE_ARRAY = "[" + DOUBLE;
        public static final String CHAR_ARRAY = "[" + CHAR;
        public static final String BYTE_ARRAY = "[" + BYTE;
        public static final String SHORT_ARRAY = "[" + SHORT;
    }
}
