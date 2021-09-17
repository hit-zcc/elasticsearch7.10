/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.iget.org.elasticsearch.searchPlugin.Parser;

import org.iget.org.elasticsearch.searchPlugin.Factory.IEncodeParser;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultParser implements IEncodeParser {
    @Override
    public HashSet<String> parse(Object val) {
        List<String> stringList = Collections.EMPTY_LIST;
        try {
            stringList = (List<String>) ((List) val).stream().map(en->en+"").collect(Collectors.toList());
            return new HashSet(stringList);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new HashSet(stringList);
    }
}
