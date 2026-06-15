package com.gamma.spool.asm;

public class SpoolNames {

    public static final String SKIP_ASM_CHECKS_ANNOTATION = "com/gamma/spool/api/annotations/SkipSpoolASMChecks";

    public static class Targets {

        public static final String MIXINS = "com/gamma/spool/mixin";
        public static final String ASM = "com/gamma/spool/asm";
        public static final String CORE = "com/gamma/spool/core";

        public static final String CHUNK = "net/minecraft/world/chunk/Chunk";
        public static final String CHUNK_OBF = "apx";

        public static final String EMPTY_CHUNK = "net/minecraft/world/chunk/EmptyChunk";
        public static final String EMPTY_CHUNK_OBF = "apw";
    }

    public static class Destinations {

        public static final String CONCURRENT_CHUNK = "com/gamma/spool/concurrent/ConcurrentChunk";
        public static final String CONCURRENT_CHUNK_EID = "com/gamma/spool/compat/endlessids/ConcurrentChunkWrapper";
    }
}
