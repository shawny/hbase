/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.client;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.WritableHelper;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An extension of the {@link org.apache.hadoop.hbase.HColumnDescriptor} that
 * adds the ability to define indexes on column family qualifiers.
 * <p/>
 */
public class SecondaryIndexColumnDescriptor extends HColumnDescriptor {
    /**
     * The key used to store and retrieve index descriptors.
     */
    public static final ImmutableBytesWritable INDEX_DESCRIPTORS =
            new ImmutableBytesWritable(Bytes.toBytes("INDEX_DESC"));

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor()
     */
    public SecondaryIndexColumnDescriptor() {
        super();
    }

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor(String)
     */
    public SecondaryIndexColumnDescriptor(String familyName) {
        super(familyName);
    }

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor(byte[])
     */
    public SecondaryIndexColumnDescriptor(byte[] familyName) {
        super(familyName);
    }

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor(HColumnDescriptor)
     */
    public SecondaryIndexColumnDescriptor(HColumnDescriptor desc) {
        super(desc);
    }

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor(byte[],
     *      int, String, boolean, boolean, int, String)
     */
    public SecondaryIndexColumnDescriptor(byte[] familyName, int maxVersions,
                               String compression, boolean inMemory,
                               boolean blockCacheEnabled, int timeToLive,
                               String bloomFilter) {
        super(familyName, maxVersions, compression, inMemory, blockCacheEnabled,
                timeToLive, bloomFilter);
    }

    /**
     * Constructor.
     *
     * @see org.apache.hadoop.hbase.HColumnDescriptor#HColumnDescriptor(byte[],
     *      int, String, boolean, boolean, int, int, String, int)
     */
    public SecondaryIndexColumnDescriptor(byte[] familyName, int maxVersions,
                               String compression, boolean inMemory,
                               boolean blockCacheEnabled, int blocksize,
                               int timeToLive, String bloomFilter, int scope) {
        super(familyName, maxVersions, compression, inMemory, blockCacheEnabled,
                blocksize, timeToLive, bloomFilter, scope);
    }

    /**
     * Adds the index descriptor to the column family, replacing the existing
     * index descriptor for the {@link org.apache.hadoop.hbase.client.idx.IdxIndexDescriptor#getQualifierName()
     * qualifier name} if one exists.
     *
     * @param descriptor the descriptor
     * @throws IllegalArgumentException if an index descriptor already exists for
     *                                  the qualifier
     * @throws NullPointerException     if the provided descriptor has a null
     *                                  {@link IdxIndexDescriptor#getQualifierName()}
     * @throws java.io.IOException      if an error occurrs while attempting to
     *                                  write the index descriptors to the values
     */
    public void addIndexDescriptor(SecondaryIndexDescriptor descriptor)
            throws NullPointerException, IllegalArgumentException, IOException {
        if (descriptor.getQualifierName() == null
                || descriptor.getQualifierName().length <= 0) {
            throw new NullPointerException("Qualifier name cannot be null or empty");
        }
        ImmutableBytesWritable qualifierName
                = new ImmutableBytesWritable(descriptor.getQualifierName());
        Map<ImmutableBytesWritable, SecondaryIndexDescriptor> indexDescriptorMap
                = getIndexDescriptors(this);
        if (indexDescriptorMap.containsKey(qualifierName)) {
            throw new IllegalArgumentException("An index already exists on qualifier '"
                    + Bytes.toString(descriptor.getQualifierName()) + "'");
        }
        indexDescriptorMap.put(qualifierName, descriptor);
        setIndexDescriptors(this, indexDescriptorMap);
    }

    /**
     * Removes an index descriptor if one exists for the qualifier name.
     *
     * @param qualifierName the qualifier name
     * @return true if the index descriptor existed and was removed, otherwise
     *         false
     * @throws java.io.IOException if an error occurrs while attempting to
     *                             write the index descriptors to the values
     */
    public boolean removeIndexDescriptor(final byte[] qualifierName) throws IOException {
        return removeIndexDescriptor(new ImmutableBytesWritable(qualifierName));
    }

    /**
     * Removes an index descriptor if one exists for the qualifier name.
     *
     * @param qualifierName the qualifier name
     * @return true if the index descriptor existed and was removed, otherwise
     *         false
     * @throws java.io.IOException if an error occurrs while attempting to
     *                             write the index descriptors to the values
     */
    public boolean removeIndexDescriptor(final ImmutableBytesWritable qualifierName) throws IOException {
        Map<ImmutableBytesWritable, SecondaryIndexDescriptor> indexDescriptorMap
                = getIndexDescriptors(this);
        if (indexDescriptorMap.containsKey(qualifierName)) {
            indexDescriptorMap.remove(qualifierName);
            setIndexDescriptors(this, indexDescriptorMap);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the index descriptor matching the provided qualifier name.
     *
     * @param qualifierName the qualifier name
     * @return the index descriptor or null
     * @throws java.io.IOException if an error occurrs while reading the index descriptor
     */
    public SecondaryIndexDescriptor getIndexDescriptor(final byte[] qualifierName) throws IOException {
        return getIndexDescriptor(new ImmutableBytesWritable(qualifierName));
    }

    /**
     * Returns the index descriptor matching the provided qualifier name.
     *
     * @param qualifierName the qualifier name
     * @return the index descriptor or null
     * @throws java.io.IOException if an error occurrs while reading the index descriptor
     */
    public SecondaryIndexDescriptor getIndexDescriptor(final ImmutableBytesWritable qualifierName) throws IOException {
        return hasIndexDescriptors(this) ? getIndexDescriptors(this).get(qualifierName) : null;
    }

    /**
     * Returns an unmodifiable set of index descriptions associated with this
     * column family.
     *
     * @return the set of index descriptios (never null)
     * @throws java.io.IOException if an error occurrs while reading the index
     *                             descriptors
     */
    public Set<SecondaryIndexDescriptor> getIndexDescriptors() throws IOException {
        Set<SecondaryIndexDescriptor> set = new HashSet<SecondaryIndexDescriptor>();
        if (hasIndexDescriptors(this)) {
            set.addAll(getIndexDescriptors(this).values());
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns a set for the qualifiers that currently have an index.
     *
     * @return the set of indexed qualifiers
     * @throws java.io.IOException if an error occurrs while reading the index
     *                             descriptors
     */
    public Set<ImmutableBytesWritable> getIndexedQualifiers() throws IOException {
        Set<ImmutableBytesWritable> set = new HashSet<ImmutableBytesWritable>();
        if (hasIndexDescriptors(this)) {
            set.addAll(getIndexDescriptors(this).keySet());
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Helper method to check if a column descriptor contains index descriptors.
     *
     * @param columnDescriptor the column descriptor
     * @return true if there are index descriptors, otherwise false
     */
    public static boolean hasIndexDescriptors(HColumnDescriptor columnDescriptor) {
        return columnDescriptor.getValues().containsKey(INDEX_DESCRIPTORS);
    }

    /**
     * Helper method to get a map of index descriptors from the
     * {@link org.apache.hadoop.hbase.HColumnDescriptor#getValues() values} meta-
     * data available on a column descriptpor.
     *
     * @param columnDescriptor the column descriptor
     * @return the map of index descriptors (never null)
     * @throws IOException if an error occurrs while reading the index descriptors
     */
    public static Map<ImmutableBytesWritable, SecondaryIndexDescriptor> getIndexDescriptors(HColumnDescriptor columnDescriptor) throws IOException {
        Map<ImmutableBytesWritable, ImmutableBytesWritable> values = columnDescriptor.getValues();
        if (hasIndexDescriptors(columnDescriptor)) {
            DataInputBuffer in = new DataInputBuffer();
            byte[] bytes = values.get(INDEX_DESCRIPTORS).get();
            in.reset(bytes, bytes.length);

            int size = in.readInt();
            Map<ImmutableBytesWritable, SecondaryIndexDescriptor> indexDescriptors
                    = new HashMap<ImmutableBytesWritable, SecondaryIndexDescriptor>(size);

            for (int i = 0; i < size; i++) {
            	SecondaryIndexDescriptor indexDescriptor
                        = WritableHelper.readInstance(in, SecondaryIndexDescriptor.class);
                indexDescriptors.put(new ImmutableBytesWritable(indexDescriptor.getQualifierName()), indexDescriptor);
            }

            return indexDescriptors;
        } else {
            return new HashMap<ImmutableBytesWritable, SecondaryIndexDescriptor>();
        }
    }

    /**
     * Helper method to set a map of index descriptors on the
     * {@link org.apache.hadoop.hbase.HColumnDescriptor#getValues() values} meta-
     * data available on a column descriptor.
     *
     * @param columnDescriptor   the column descriptor
     * @param indexDescriptorMap the map of index descriptors
     * @throws IOException if an error occurrs while writing the index descriptors
     */
    public static void setIndexDescriptors(HColumnDescriptor columnDescriptor, Map<ImmutableBytesWritable, SecondaryIndexDescriptor> indexDescriptorMap) throws IOException {
        DataOutputBuffer out = new DataOutputBuffer();
        out.writeInt(indexDescriptorMap.size());
        for (SecondaryIndexDescriptor indexDescriptor : indexDescriptorMap.values()) {
            WritableHelper.writeInstance(out, indexDescriptor);
        }

        columnDescriptor.setValue(INDEX_DESCRIPTORS.get(), out.getData());
    }
}
