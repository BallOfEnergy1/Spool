package com.gamma.spool.util.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.profiler.Profiler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

// Woo!
public class AsyncProfiler extends Profiler {

    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);

    private static final Logger asyncLogger = LogManager.getLogger();
    /** List of parent sections */
    private final Long2ObjectMap<ObjectArrayList<String>> asyncSectionList = Long2ObjectMaps
        .synchronize(new Long2ObjectArrayMap<>());
    /** Map of timestamps (System.nanoTime) */
    private final Long2ObjectMap<LongArrayList> asyncTimestampList = Long2ObjectMaps
        .synchronize(new Long2ObjectArrayMap<>());
    /** Map of current profiling sections */
    private final Long2ObjectMap<String> asyncProfilingSections = Long2ObjectMaps
        .synchronize(new Long2ObjectArrayMap<>());
    /** Profiling map */
    private final Long2ObjectMap<Object2LongMap<String>> asyncProfilingMap = Long2ObjectMaps
        .synchronize(new Long2ObjectArrayMap<>());

    /**
     * Clear profiling.
     */
    @Override
    public void clearProfiling() {
        this.asyncProfilingMap.clear();
        this.asyncProfilingSections.clear();
        this.asyncSectionList.clear();
        this.asyncTimestampList.clear();
    }

    /**
     * Start section
     */
    @Override
    public void startSection(String name) {
        if (this.profilingEnabled) {

            LOCK.readLock()
                .lock();

            long thisThreadID = Thread.currentThread()
                .getId();

            String thisAsyncSection = asyncProfilingSections.get(thisThreadID);
            if (thisAsyncSection == null) asyncProfilingSections.put(thisThreadID, thisAsyncSection = "");

            if (!thisAsyncSection.isEmpty()) {
                thisAsyncSection = thisAsyncSection + ".";
            }

            thisAsyncSection = thisAsyncSection + name;

            asyncProfilingSections.put(thisThreadID, thisAsyncSection);

            ObjectArrayList<String> sections = this.asyncSectionList.get(thisThreadID);
            if (sections == null) this.asyncSectionList.put(thisThreadID, sections = new ObjectArrayList<>());
            sections.add(thisAsyncSection);

            LongArrayList timestamps = this.asyncTimestampList.get(thisThreadID);
            if (timestamps == null) this.asyncTimestampList.put(thisThreadID, timestamps = new LongArrayList());

            timestamps.add(System.nanoTime());

            LOCK.readLock()
                .unlock();
        }
    }

    /**
     * End section
     */
    @Override
    public void endSection() {
        if (this.profilingEnabled) {

            LOCK.readLock()
                .lock();

            long thisThreadID = Thread.currentThread()
                .getId();

            long currentTime = System.nanoTime();

            LongArrayList thisTimestampList = this.asyncTimestampList.get(thisThreadID);
            long timeThen = thisTimestampList.removeLong(thisTimestampList.size() - 1);

            ObjectArrayList<String> thisSectionList = this.asyncSectionList.get(thisThreadID);
            thisSectionList.remove(thisSectionList.size() - 1);

            long deltaTime = currentTime - timeThen;

            String thisSection = this.asyncProfilingSections.get(thisThreadID);

            Object2LongMap<String> thisProfilingMap = this.asyncProfilingMap.get(thisThreadID);
            if (thisProfilingMap == null)
                this.asyncProfilingMap.put(thisThreadID, thisProfilingMap = new Object2LongArrayMap<>());

            if (thisProfilingMap.containsKey(thisSection)) {
                thisProfilingMap.put(thisSection, thisProfilingMap.getLong(thisSection) + deltaTime);
            } else {
                thisProfilingMap.put(thisSection, deltaTime);
            }

            if (deltaTime > 100000000L) { // 100ms
                asyncLogger.warn(
                    "Something's taking too long! '{}' took aprox {} ms on thread '{}'",
                    thisSection,
                    (double) deltaTime / 1000000.0D,
                    Thread.currentThread()
                        .getName());
            }

            this.asyncProfilingSections
                .put(thisThreadID, !thisSectionList.isEmpty() ? thisSectionList.get(thisSectionList.size() - 1) : "");

            LOCK.readLock()
                .unlock();
        }
    }

    @Override
    public List<Result> getProfilingData(String p_76321_1_) {
        throw new UnsupportedOperationException(
            "AsyncProfiler.getProfilingData(String) is not supported, use AsyncProfiler.getAsyncProfilingData(String) instead.");
    }

    /**
     * Get profiling data
     */
    public AsyncResults getAsyncProfilingData(String section) {
        return getAsyncProfilingData(section, 0, true);
    }

    /**
     * Get profiling data
     */
    public AsyncResults getAsyncProfilingData(String section, long threadID, boolean all) {
        if (!this.profilingEnabled) {
            return null;
        } else {
            ObjectArrayList<AsyncResult> arraylist = new ObjectArrayList<>();
            ObjectArrayList<AsyncResult> sections = new ObjectArrayList<>();
            try {
                LOCK.writeLock()
                    .lock();
                for (Long2ObjectMap.Entry<Object2LongMap<String>> entry : asyncProfilingMap.long2ObjectEntrySet()) {

                    if (!all && threadID != entry.getLongKey()) continue;

                    Object2LongMap<String> thisProfilingMap = entry.getValue();

                    if (!thisProfilingMap.containsKey(section)) continue;

                    long rootTime = thisProfilingMap.getOrDefault("root", 0L);
                    long sectionTime = thisProfilingMap.getOrDefault(section, -1L);

                    String newSection;

                    if (!section.isEmpty()) {
                        // This is required, as we need a new reference to the string instead of using the original
                        // object.
                        // noinspection StringOperationCanBeSimplified
                        newSection = new String(section + ".");
                    } else {
                        newSection = section;
                    }

                    long subsectionTime = 0L;

                    for (String subSection : thisProfilingMap.keySet()) {
                        if (subSection.length() > newSection.length() && subSection.startsWith(newSection)
                            && subSection.indexOf(".", newSection.length() + 1) < 0) {
                            subsectionTime += thisProfilingMap.getLong(subSection);
                        }
                    }

                    double totalSubsectionTime = (double) subsectionTime;

                    if (subsectionTime < sectionTime) {
                        subsectionTime = sectionTime;
                    }

                    if (rootTime < subsectionTime) {
                        rootTime = subsectionTime;
                    }

                    Iterator<String> sectionIterator = thisProfilingMap.keySet()
                        .iterator();
                    String currentSection;

                    while (sectionIterator.hasNext()) {
                        currentSection = sectionIterator.next();

                        if (currentSection.length() > newSection.length() && currentSection.startsWith(newSection)
                            && currentSection.indexOf(".", newSection.length() + 1) < 0) {
                            double currentSectionTime = (double) thisProfilingMap.getLong(currentSection);
                            double percentSubsection = currentSectionTime * 100.0D / (double) subsectionTime;
                            double percentRoot = currentSectionTime * 100.0D / (double) rootTime;
                            String targetSection = currentSection.substring(newSection.length());
                            arraylist.add(
                                new AsyncResult(targetSection, entry.getLongKey(), percentSubsection, percentRoot));
                        }
                    }

                    sectionIterator = thisProfilingMap.keySet()
                        .iterator();

                    while (sectionIterator.hasNext()) {
                        String s2 = sectionIterator.next();
                        thisProfilingMap.put(s2, thisProfilingMap.getLong(s2) * 999L / 1000L); // Reduce by 0.1%.
                    }

                    if ((double) subsectionTime > totalSubsectionTime) {
                        arraylist.add(
                            new AsyncResult(
                                "unspecified",
                                entry.getLongKey(),
                                ((double) subsectionTime - totalSubsectionTime) * 100.0D / (double) subsectionTime,
                                (subsectionTime - totalSubsectionTime) * 100.0D / (double) rootTime));
                    }

                    sections.add(
                        0,
                        new AsyncResult(
                            newSection,
                            entry.getLongKey(),
                            100.0D,
                            (double) subsectionTime * 100.0D / (double) rootTime));
                }

                arraylist.sort(null);
                sections.sort(null);
                arraylist.addAll(0, sections);
            } finally {
                LOCK.writeLock()
                    .unlock();
            }
            return new AsyncResults(arraylist);
        }
    }

    /**
     * End current section and start a new section
     */
    @Override
    public void endStartSection(String name) {
        this.endSection();
        this.startSection(name);
    }

    @Override
    public String getNameOfLastSection() {
        return this.getNameOfLastSection(
            Thread.currentThread()
                .getId());
    }

    public String getNameOfLastSection(long threadID) {
        ObjectArrayList<String> sections = this.asyncSectionList.get(threadID);
        return sections.isEmpty() ? "[UNKNOWN]" : sections.get(sections.size() - 1);
    }

    @Desugar
    public record AsyncResult(String sectionName, long threadID, double percentSection, double percentRoot)
        implements Comparable<AsyncResult> {

        public long getID() {
            return threadID;
        }

        public int compareTo(AsyncResult resultToCompareTo) {
            return resultToCompareTo.percentSection < this.percentSection ? -1
                : (resultToCompareTo.percentSection > this.percentSection ? 1
                    : resultToCompareTo.sectionName.compareTo(this.sectionName));
        }

        @SideOnly(Side.CLIENT)
        public int getColor() {
            return (this.sectionName.hashCode() & 11184810) + 4473924;
        }
    }

    public static final class AsyncResults {

        public final ObjectArrayList<AsyncResult> list;
        public final int numThreadsProfiled;

        public AsyncResults(ObjectArrayList<AsyncResult> list) {
            this.list = list;
            this.numThreadsProfiled = Math.toIntExact(
                list.stream()
                    .mapToLong(AsyncResult::getID)
                    .distinct()
                    .count());
        }
    }
}
