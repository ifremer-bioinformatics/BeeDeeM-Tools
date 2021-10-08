/* Copyright (C) 2018 Patrick G. Durand
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/agpl-3.0.txt
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 */
package test.unit;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import fr.ifremer.bioinfo.bdm.tools.CmdLineDumper;

/**
 * A class to test CmdLineDumper tool.
 */
public class CmdLineDumperTest {

  // path with test data included in this project
  private static final String DATA_PATH = "tests/datafile/";
  // an XML data file
  private static final String DATA_FILE_1 = "hits_only.xml";
  // a ZML data file
  private static final String DATA_FILE_2 = "hits_only.zml";
  // a data file
  private static final String DATA_FILE_3 = "hits_with_bco.zml";
  // a reference file
  private static final String DATA_FILE_1_REF = "hits_only_xml.csv";
  // a reference file
  private static final String DATA_FILE_3_REF = "hits_only_xml_bco.csv";
  
  private static File data_file;
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // Called libraries (e.g. BeeDeeM sequence manager framework) rely on Log4J
    BasicConfigurator.configure();
    data_file = File.createTempFile("dumper", ".tmp", new File(System.getProperty("java.io.tmpdir")));
    //data_file.deleteOnExit();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void test_dumper_xml() {
    // this is the most basic way of using the tool
    boolean bRet = CmdLineDumper.doJob(new String[] {
        "-i", DATA_PATH+DATA_FILE_1,
        "-o", data_file.getAbsolutePath()
        });              
    
    // Job running OK?
    assertTrue(bRet);
    
    // Job generated file OK?
    DBMSExecNativeCommand runner = new DBMSExecNativeCommand();
    String cmd = "diff "+data_file.getAbsolutePath()+" "+DATA_PATH+DATA_FILE_1_REF;
    runner.execute(cmd);
    assertTrue(runner.getExitCode()==0);
  }
  @Test
  public void test_dumper_zml() {
    // this is the most basic way of using the tool with a ZML file
    boolean bRet = CmdLineDumper.doJob(new String[] {
        "-i", DATA_PATH+DATA_FILE_2,
        "-f", "zml",
        "-o", data_file.getAbsolutePath()
        });              
    
    // Job running OK?
    assertTrue(bRet);
    
    // Job generated file OK?
    DBMSExecNativeCommand runner = new DBMSExecNativeCommand();
    String cmd = "diff "+data_file.getAbsolutePath()+" "+DATA_PATH+DATA_FILE_1_REF;
    runner.execute(cmd);
    assertTrue(runner.getExitCode()==0);
  }
  
  @Test
  public void test_dumper_xml_bco() {
    boolean bRet = CmdLineDumper.doJob(new String[] {
        "-i", DATA_PATH+DATA_FILE_3,
        "-f", "zml",
        "-c", "0,22,23,24,25",
        "-o", data_file.getAbsolutePath()
        });              
    
    // Job running OK?
    assertTrue(bRet);
    
    // Job generated file OK?
    DBMSExecNativeCommand runner = new DBMSExecNativeCommand();
    String cmd = "diff "+data_file.getAbsolutePath()+" "+DATA_PATH+DATA_FILE_3_REF;
    System.err.println(cmd);
    runner.execute(cmd);
    assertTrue(runner.getExitCode()==0);
  }

}
