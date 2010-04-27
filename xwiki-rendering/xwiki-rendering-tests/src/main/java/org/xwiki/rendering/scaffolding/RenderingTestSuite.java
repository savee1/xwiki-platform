/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.scaffolding;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestSuite;

/**
 * @version $Id$
 * @since 1.6M1
 */
public class RenderingTestSuite extends TestSuite
{
    private static class Data
    {
        public Map<String, String> inputs = new HashMap<String, String>();

        public Map<String, String> expectations = new HashMap<String, String>();
    }

    public RenderingTestSuite(String name)
    {
        super(name);
    }

    public void addTestsFromResource(String testResourceName, boolean runTransformations) throws Exception
    {
        String resourceName = "/" + testResourceName + ".test";
        Data data;
        try {
            data = readTestData(getClass().getResourceAsStream(resourceName), resourceName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find load test data [" + testResourceName + "]");
        }

        // Create a test case for each input and for each expectation so that each test is executed separately
        // and reported separately by the JUnit test runner.
        for (Map.Entry<String, String> entry : data.inputs.entrySet()) {
            for (String targetSyntaxId : data.expectations.keySet()) {
                String parserId = entry.getKey();
                String input = entry.getValue();

                if ("xhtml/1.0".equals(parserId) && !input.startsWith("<?xml") && !input.startsWith("<!DOCTYPE")) {
                    input =
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
                            + "<html>" + input + "</html>";
                }

                RenderingTestCase testCase =
                    new RenderingTestCase(computeTestName(testResourceName, parserId, targetSyntaxId), input,
                        data.expectations.get(targetSyntaxId), parserId, targetSyntaxId, runTransformations);
                addTest(testCase);
            }
        }
    }

    /**
     * Read test data separated by lines containing ".". For example:
     * 
     * <pre>
     * &lt;code&gt;
     * .input|xwiki/2.0
     * This is a test
     * .expect|XHTML
     * &lt;p&gt;This is a test&lt;/p&gt;
     * &lt;/code&gt;
     * </pre>
     */
    private Data readTestData(InputStream source, String resourceName) throws Exception
    {
        Data data = new Data();

        BufferedReader reader = new BufferedReader(new InputStreamReader(source));

        // Read each line and look for lines starting with ".". When this happens it means we've found a separate
        // test case.
        try {
            Map map = null;
            String keyName = null;
            boolean skip = false;
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".")) {
                    if (line.startsWith(".#")) {
                        // Ignore comments and print it to the stdout if it's a todo.
                        if (line.toLowerCase().contains("todo")) {
                            System.out.println(line);
                        }
                    } else {
                        // If there's already some data, write it to the maps now.
                        if (map != null) {
                            if (!skip) {
                                saveBuffer(buffer, map, data.inputs, keyName);
                            }
                            buffer.setLength(0);
                        }
                        // Parse the directive line starting with "." and with "|" separators.
                        // For example ".input|xwiki/2.0|skip" or ".expect|xhtml"
                        StringTokenizer st = new StringTokenizer(line.substring(1), "|");
                        // First token is "input" or "expect"
                        if (st.nextToken().equalsIgnoreCase("input")) {
                            map = data.inputs;
                        } else {
                            map = data.expectations;
                        }
                        // Second token is either the input syntax id or the expectation renderer short name
                        keyName = st.nextToken();
                        // Third (optional) token is whether the test should be skipped (useful while waiting for
                        // a fix to wikimodel for example).
                        skip = false;
                        if (st.hasMoreTokens()) {
                            skip = true;
                            System.out.println("[WARNING] Skipping test for [" + keyName + "] in file [" + resourceName
                                + "] since it has been marked as skipped in the test. This needs to be reviewed "
                                + "and fixed.");
                        }
                    }
                } else {
                    buffer.append(line).append('\n');
                }
            }

            if (!skip) {
                saveBuffer(buffer, map, data.inputs, keyName);
            }

        } finally {
            reader.close();
        }

        return data;
    }

    private void saveBuffer(StringBuffer buffer, Map map, Map<String, String> inputs, String keyName) throws Exception
    {
        // Remove the last newline since our test format forces an additional new lines
        // at the end of input texts.
        if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '\n') {
            buffer.setLength(buffer.length() - 1);
        }
        map.put(keyName, buffer.toString());
    }

    private String computeTestName(String prefix, String parserId, String targetSyntaxId)
    {
        // Note: For some reason the Eclipse JUnit test runner strips the information found in parenthesis. Thus we use
        // square brackets instead.
        return prefix + " [" + parserId + ", " + targetSyntaxId + "]";
    }
}