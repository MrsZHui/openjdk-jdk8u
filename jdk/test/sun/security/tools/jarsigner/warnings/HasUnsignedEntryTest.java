/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import jdk.testlibrary.OutputAnalyzer;
import jdk.testlibrary.ProcessTools;
import jdk.testlibrary.JarUtils;

/**
 * @test
 * @bug 8024302 8026037
 * @summary Test for hasUnsignedEntry warning
 * @library /lib/testlibrary ../
 * @run main HasUnsignedEntryTest
 */
public class HasUnsignedEntryTest extends Test {

    /**
     * The test signs and verifies a jar that contains unsigned entries
     * which have not been integrity-checked (hasUnsignedEntry).
     * Warning message is expected.
     */
    public static void main(String[] args) throws Throwable {
        HasUnsignedEntryTest test = new HasUnsignedEntryTest();
        test.start();
    }

    private void start() throws Throwable {
        System.out.println(String.format("Create a %s that contains %s",
                UNSIGNED_JARFILE, FIRST_FILE));
        Utils.createFiles(FIRST_FILE, SECOND_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // create key pair for signing
        createAlias(CA_KEY_ALIAS, "-ext", "bc:c");
        createAlias(KEY_ALIAS);
        issueCert(
                KEY_ALIAS,
                "-validity", Integer.toString(VALIDITY));

        // sign jar
        OutputAnalyzer analyzer = ProcessTools.executeCommand(JARSIGNER,
                "-verbose",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                KEY_ALIAS);

        checkSigning(analyzer);

        System.out.println(String.format("Copy %s to %s, and add %s.class, "
                + "so it contains unsigned entry",
                new Object[]{SIGNED_JARFILE, UPDATED_SIGNED_JARFILE,
                    SECOND_FILE}));

        JarUtils.updateJar(SIGNED_JARFILE, UPDATED_SIGNED_JARFILE, SECOND_FILE);

        // verify jar
        analyzer = ProcessTools.executeCommand(JARSIGNER,
                "-verify",
                "-verbose",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE);

        checkVerifying(analyzer, 0, HAS_UNSIGNED_ENTRY_VERIFYING_WARNING);

        // verify jar in strict mode
        analyzer = ProcessTools.executeCommand(JARSIGNER,
                "-verify",
                "-verbose",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE);

        checkVerifying(analyzer, HAS_UNSIGNED_ENTRY_EXIT_CODE,
                HAS_UNSIGNED_ENTRY_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
