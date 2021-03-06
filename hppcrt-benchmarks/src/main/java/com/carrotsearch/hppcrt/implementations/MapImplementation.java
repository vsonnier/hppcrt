package com.carrotsearch.hppcrt.implementations;

import java.util.Random;

import org.openjdk.jmh.infra.Blackhole;

/**
 * Something implementing a map interface (int-int).
 * (or OBJ - int)
 */
public abstract class MapImplementation<IMPLEM>
{
    public static final long METHOD_CALL_CPU_COST = 16;

    public enum HASH_QUALITY
    {
        NORMAL(0),
        BAD(6);

        public final int shift;

        private HASH_QUALITY(final int bitshift)
        {
            this.shift = bitshift;
        }
    }

    /**
     * A Int holder with variable Hash Qualities.
     * @author Vincent
     *
     */
    public static class ComparableInt implements Comparable<ComparableInt>
    {
        public int value;
        public final int bitshift;

        public ComparableInt(final int initValue, final HASH_QUALITY quality)
        {
            this.value = initValue;
            this.bitshift = quality.shift;
        }

        @Override
        public int compareTo(final ComparableInt other)
        {
            //eat some CPU to simulate method cost
            Blackhole.consumeCPU(MapImplementation.METHOD_CALL_CPU_COST);

            if (this.value < other.value)
            {
                return -1;
            }
            else if (this.value > other.value)
            {
                return 1;
            }

            return 0;
        }

        @Override
        public int hashCode()
        {
            //eat some CPU to simulate method cost
            Blackhole.consumeCPU(MapImplementation.METHOD_CALL_CPU_COST);

            return this.value << this.bitshift;
        }

        @Override
        public boolean equals(final Object obj)
        {
            //eat some CPU to simulate method cost
            Blackhole.consumeCPU(MapImplementation.METHOD_CALL_CPU_COST);

            if (obj instanceof ComparableInt)
            {
                return ((ComparableInt) obj).value == this.value;
            }

            return false;
        }
    }

    protected IMPLEM instance;

    protected MapImplementation(final IMPLEM instance)
    {
        this.instance = instance;
    }

    /**
     * Contains bench to run, setup() must prepare the K,V set before
     */
    public abstract int benchContainKeys();

    /**
     * removed bench to run, setup() must prepare the K,V set before
     */
    public abstract int benchRemoveKeys();

    /**
     * put  bench to run, setup() must prepare the K,V set before
     */
    public abstract int benchPutAll();

    /**
     * Preparation of a set of keys before executing the benchXXX() methods
     * @param keysToInsert the array of int or ComparableInts of HASH_QUALITY hashQ
     * to insert in the map on test
     * @param keysForContainsQuery the array of of int or ComparableInts to which the filled map
     * will be queried for contains()
     * @param keysForRemovalQuery the array of of int or ComparableInts to which the filled map
     * will be queried for remove()
     */
    public abstract void setup(int[] keysToInsert, HASH_QUALITY hashQ, int[] keysForContainsQuery, int[] keysForRemovalQuery);

    //// Convenience methods to implement
    //// to ease setup() implementation.
    //// used for setup()

    public abstract void clear();

    public abstract int size();

    /**
     * Re-shuffle the keys set, used in put() (existing keys) benchmark.
     */
    public abstract void reshuffleInsertedKeys(Random rand);

    /**
     * Re-shuffle the values set, used in put() (existing keys) benchmark.
     */
    public abstract void reshuffleInsertedValues(Random rand);

    /**
     * Sort-of clone() the whole implementation of toCloneFrom in its current state, (independent copy !)
     * very convenient to quickly set a current state of an implem from a reference implem
     * in tests.
     * @return
     */
    public abstract void setCopyOfInstance(MapImplementation<?> toCloneFrom);

    /**
     * By default, not an IdentityMap, override if needed
     * @return
     */
    public boolean isIdentityMap() {

        return false;
    }

}