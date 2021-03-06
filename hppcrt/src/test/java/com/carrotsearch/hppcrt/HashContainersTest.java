package com.carrotsearch.hppcrt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.RandomizedTest;

@RunWith(RandomizedRunner.class)
public class HashContainersTest
{
    /* */
    @Test
    public void testCapacityCalculations() {

        Assert.assertEquals(HashContainers.MIN_HASH_ARRAY_LENGTH, HashContainers.minBufferSize(0, 0.5f));
        Assert.assertEquals(HashContainers.MIN_HASH_ARRAY_LENGTH, HashContainers.minBufferSize(1, 0.5f));

        Assert.assertEquals(0x20, HashContainers.minBufferSize(0x10 - 1, 0.5f));
        Assert.assertEquals(0x40, HashContainers.minBufferSize(0x10, 0.49f));

        final int maxCapacity = HashContainers.maxElements(HashContainers.MAX_LOAD_FACTOR);

        try {
            // This should be impossible because it'd create a negative-sized array.
            HashContainers.minBufferSize(maxCapacity + 2, HashContainers.MAX_LOAD_FACTOR);
            Assert.fail();
        } catch (final BufferAllocationException e) {
            // Expected.
        }
    }
}
