/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jxpath;

/**
 * More complete implementation for the XPath <code>"key()"</code> function.
 * Returns NodeSet results and allows Object values for better compatibility
 * with non-XML graphs.
 *
 * @author Sergey Vladimirov
 * @author Matt Benson
 * @since JXPath 1.3
 * @version $Revision: 1.1.2.1 $ $Date: 2009/11/06 00:24:09 $
 */
public interface ExtendedKeyManager extends KeyManager {

    /**
     * Find a NodeSet by key/value.
     * @param context base
     * @param key String
     * @param value Object
     * @return NodeSet found
     */
    NodeSet getNodeSetByKey(JXPathContext context, String key, Object value);

}