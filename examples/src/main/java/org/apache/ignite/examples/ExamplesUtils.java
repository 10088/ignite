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

package org.apache.ignite.examples;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.cluster.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.events.*;
import org.apache.ignite.streamer.*;

import java.net.*;
import java.util.*;

/**
 *
 */
public class ExamplesUtils {
    /** */
    private static final ClassLoader CLS_LDR = ExamplesUtils.class.getClassLoader();

    /** Events of these types should be recorded in examples. */
    public static final int[] EVTS;

    static {
        EVTS = Arrays.copyOf(EventType.EVTS_CACHE, EventType.EVTS_CACHE.length + EventType.EVTS_TASK_EXECUTION.length);

        System.arraycopy(EventType.EVTS_TASK_EXECUTION, 0, EVTS, EventType.EVTS_CACHE.length,
            EventType.EVTS_TASK_EXECUTION.length);
    }

    /**
     * Exits with code {@code -1} if maximum memory is below 90% of minimally allowed threshold.
     *
     * @param min Minimum memory threshold.
     */
    public static void checkMinMemory(long min) {
        long maxMem = Runtime.getRuntime().maxMemory();

        if (maxMem < .85 * min) {
            System.err.println("Heap limit is too low (" + (maxMem / (1024 * 1024)) +
                "MB), please increase heap size at least up to " + (min / (1024 * 1024)) + "MB.");

            System.exit(-1);
        }
    }

    /**
     * Returns URL resolved by class loader for classes in examples project.
     *
     * @return Resolved URL.
     */
    public static URL url(String path) {
        URL url = CLS_LDR.getResource(path);

        if (url == null)
            throw new RuntimeException("Failed to resolve resource URL by path: " + path);

        return url;
    }

    /**
     * Checks minimum topology size for running a certain example.
     *
     * @param prj Cluster to check size for.
     * @param size Minimum number of nodes required to run a certain example.
     * @return {@code True} if check passed, {@code false} otherwise.
     */
    public static boolean checkMinTopologySize(ClusterGroup prj, int size) {
        int prjSize = prj.nodes().size();

        if (prjSize < size) {
            System.out.println();
            System.out.println(">>> Please start at least " + size + " cluster nodes to run example.");
            System.out.println();

            return false;
        }

        return true;
    }

    /**
     * @param ignite Ignite.
     * @param name Streamer name.
     * @return {@code True} if ignite has streamer with given name.
     */
    public static boolean hasStreamer(Ignite ignite, String name) {
        if (ignite.configuration().getStreamerConfiguration() != null) {
            for (StreamerConfiguration cfg : ignite.configuration().getStreamerConfiguration()) {
                if (name.equals(cfg.getName()))
                    return true;
            }
        }

        return false;
    }
}
