/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.marshaller.optimized.ext;

import org.apache.ignite.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.marshaller.optimized.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.apache.ignite.marshaller.optimized.OptimizedClassDescriptor.*;
import static org.apache.ignite.marshaller.optimized.OptimizedMarshallerUtils.*;

/**
 * TODO
 */
public class OptimizedMarshallerExt extends OptimizedMarshaller {
    /** */
    static final byte EMPTY_FOOTER = -1;

    /** */
    static final byte FOOTER_LEN_OFF = 4;

    /** */
    static final byte VARIABLE_LEN = -1;

    /** */
    private OptimizedMarshallerExtMetaHandler metaHandler;

    /**
     * Creates new marshaller providing whether it should
     * require {@link Serializable} interface or not.
     *
     * @param requireSer Whether to require {@link Serializable}.
     */
    public OptimizedMarshallerExt(boolean requireSer) {
        super(requireSer);
    }

    /**
     * Sets metadata handler.
     *
     * @param metaHandler Metadata handler.
     */
    public void setMetadataHandler(OptimizedMarshallerExtMetaHandler metaHandler) {
        this.metaHandler = metaHandler;
    }

    /**
     * Enables fields indexing for the object of the given {@code cls}.
     *
     * If enabled then a footer will be added during marshalling of an object of the given {@code cls} to the end of
     * its serialized form.
     *
     * @param cls Class.
     * @return {@code true} if fields indexing is enabled.
     * @throws IgniteCheckedException In case of error.
     */
    public boolean enableFieldsIndexing(Class<?> cls) throws IgniteCheckedException {
        assert metaHandler != null;

        try {
            OptimizedClassDescriptor desc = OptimizedMarshallerUtils.classDescriptor(clsMap, cls, ctx, mapper);

            if (desc.fields() != null && desc.fields().fieldsIndexingSupported()) {
                if (metaHandler.metadata(desc.typeId()) != null)
                    return true;

                OptimizedObjectMetadata meta = new OptimizedObjectMetadata();

                for (ClassFields clsFields : desc.fields().fieldsList())
                    for (FieldInfo info : clsFields.fieldInfoList())
                        meta.addMeta(info.id(), info.type());

                metaHandler.addMeta(desc.typeId(), meta);

                return true;
            }
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to put meta for class: " + cls.getName(), e);
        }

        return false;
    }

    /**
     * Checks whether fields indexing is enabled for objects of the given {@code cls}.
     *
     * @param cls Class.
     * @return {@code true} if fields indexing is enabled.
     */
    public boolean fieldsIndexingEnabled(Class<?> cls) {
        if (cls == OptimizedObjectMetadataKey.class)
            return false;

        try {
            OptimizedClassDescriptor desc = OptimizedMarshallerUtils.classDescriptor(clsMap, cls, ctx, mapper);

            return desc.fields() != null && desc.fields().fieldsIndexingSupported() &&
                metaHandler.metadata(desc.typeId()) != null;
        }
        catch (IOException e) {
            throw new IgniteException("Failed to load class description: " + cls);
        }
    }

    /** {@inheritDoc} */
    @Override public void marshal(@Nullable Object obj, OutputStream out) throws IgniteCheckedException {
        assert out != null;

        OptimizedObjectOutputStreamExt objOut = null;

        try {
            objOut = OptimizedObjectStreamExtRegistry.out();

            objOut.context(clsMap, ctx, mapper, requireSer, metaHandler);

            objOut.out().outputStream(out);

            objOut.writeObject(obj);
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to serialize object: " + obj, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeOut(objOut);
        }
    }

    /** {@inheritDoc} */
    @Override public byte[] marshal(@Nullable Object obj) throws IgniteCheckedException {
        OptimizedObjectOutputStreamExt objOut = null;

        try {
            objOut = OptimizedObjectStreamExtRegistry.out();

            objOut.context(clsMap, ctx, mapper, requireSer, metaHandler);

            objOut.writeObject(obj);

            return objOut.out().array();
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to serialize object: " + obj, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeOut(objOut);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T> T unmarshal(InputStream in, @Nullable ClassLoader clsLdr) throws IgniteCheckedException {
        assert in != null;

        OptimizedObjectInputStreamExt objIn = null;

        try {
            objIn = OptimizedObjectStreamExtRegistry.in();

            objIn.context(clsMap, ctx, mapper, clsLdr != null ? clsLdr : dfltClsLdr, metaHandler);

            objIn.in().inputStream(in);

            return (T)objIn.readObject();
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to deserialize object with given class loader: " + clsLdr, e);
        }
        catch (ClassNotFoundException e) {
            throw new IgniteCheckedException("Failed to find class with given class loader for unmarshalling " +
                                             "(make sure same versions of all classes are available on all nodes or " +
                                             "enable peer-class-loading): " + clsLdr, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeIn(objIn);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T> T unmarshal(byte[] arr, @Nullable ClassLoader clsLdr) throws IgniteCheckedException {
        return unmarshal(arr, 0, arr.length, clsLdr);
    }

    /**
     * Unmarshals object from byte array using given class loader and offset with len.
     *
     * @param <T> Type of unmarshalled object.
     * @param arr Byte array.
     * @param off Object's offset in the array.
     * @param len Object's length in the array.
     * @param clsLdr Class loader to use.
     * @return Unmarshalled object.
     * @throws IgniteCheckedException If unmarshalling failed.
     */
     public <T> T unmarshal(byte[] arr, int off, int len, @Nullable ClassLoader clsLdr) throws IgniteCheckedException {
        assert arr != null;

        OptimizedObjectInputStreamExt objIn = null;

        try {
            objIn = OptimizedObjectStreamExtRegistry.in();

            objIn.context(clsMap, ctx, mapper, clsLdr != null ? clsLdr : dfltClsLdr, metaHandler);

            objIn.in().bytes(arr, off, len);

            return (T)objIn.readObject();
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to deserialize object with given class loader: " + clsLdr, e);
        }
        catch (ClassNotFoundException e) {
            throw new IgniteCheckedException("Failed to find class with given class loader for unmarshalling " +
                                                 "(make sure same version of all classes are available on all nodes or" +
                                                 " enable peer-class-loading): " + clsLdr, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeIn(objIn);
        }
    }

    /**
     * Checks whether object, serialized to byte array {@code arr}, has a field with name {@code fieldName}.
     *
     * @param fieldName Field name.
     * @param arr Object's serialized form.
     * @param off Object's start off.
     * @param len Object's len.
     * @return {@code true} if field exists.
     */
    public boolean hasField(String fieldName, byte[] arr, int off, int len) throws IgniteCheckedException {
        assert arr != null && fieldName != null;

        OptimizedObjectInputStreamExt objIn = null;

        try {
            objIn = OptimizedObjectStreamExtRegistry.in();

            objIn.context(clsMap, ctx, mapper, dfltClsLdr, metaHandler);

            objIn.in().bytes(arr, off, len);

            return objIn.hasField(fieldName);
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to find field with name: " + fieldName, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeIn(objIn);
        }
    }

    /**
     * Looks up field with the given name and returns it in one of the following representations. If the field is
     * serializable and has a footer then it's not deserialized but rather returned wrapped by {@link CacheObjectImpl}
     * for future processing. In all other cases the field is fully deserialized.
     *
     * @param fieldName Field name.
     * @param arr Object's serialized form.
     * @param off Object's start offset.
     * @param len Object's len.
     * @param clsLdr Class loader.
     * @param <T> Expected field class.
     * @return Field.
     * @throws IgniteCheckedException In case of error.
     */
    public <T> T readField(String fieldName, byte[] arr, int off, int len, @Nullable ClassLoader clsLdr)
        throws IgniteCheckedException {

        assert arr != null && fieldName != null;

        OptimizedObjectInputStreamExt objIn = null;

        try {
            objIn = OptimizedObjectStreamExtRegistry.in();

            objIn.context(clsMap, ctx, mapper, clsLdr != null ? clsLdr : dfltClsLdr, metaHandler);

            objIn.in().bytes(arr, off, len);

            return objIn.readField(fieldName);
        }
        catch (IOException e) {
            throw new IgniteCheckedException("Failed to find field with name: " + fieldName, e);
        }
        catch (ClassNotFoundException e) {
            throw new IgniteCheckedException("Failed to find class with given class loader for unmarshalling " +
                                             "(make sure same version of all classes are available on all nodes or" +
                                             " enable peer-class-loading): " + clsLdr, e);
        }
        finally {
            OptimizedObjectStreamExtRegistry.closeIn(objIn);
        }
    }
}
