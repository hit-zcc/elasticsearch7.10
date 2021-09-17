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

import com.alibaba.fastjson.JSONArray;
import org.iget.org.elasticsearch.searchPlugin.Factory.IEncodeParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class GzipBase64Parser implements IEncodeParser {
    final Base64.Decoder decoder = Base64.getDecoder();
    @Override
    public HashSet<String> parse(Object val) throws IOException {
        HashSet result = new HashSet();
        String valStr = (String)val;
        byte[] decode = decoder.decode(valStr);
        String resultStr = "";

            resultStr = uncompress(decode);
            List<String> list = JSONArray.parseArray(resultStr,String.class);
            result =new HashSet(list);

        return result;
    }


    public static String uncompress(byte[] bytes) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        GZIPInputStream gunzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];
        int n;
        while ((n = gunzip.read(buffer))>=0) {
            out.write(buffer, 0, n);
        }
        // toString()使用平台默认编码，也可以显式的指定如toString(&quot;GBK&quot;)
        return out.toString();
    }


    public static void main(String[] args) throws IOException {
        new GzipBase64Parser().parse("H4sIAAAAAAAEAHvW2f20a8XLGQ0Aw7dTrgkAAAA=");
    }
}
