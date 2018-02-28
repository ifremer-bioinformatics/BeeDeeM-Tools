package test.unit;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.plealog.genericapp.api.file.EZFileUtils;

import fr.ifremer.bioinfo.blast.CmdLineCutter;

/**
 * A class to test CmdLineCutter tool. Please note that unit tests for the entire
 * sequence file manager framework is available from the BeeDeeM project. Here, we 
 * only test the tool layer.
 */
public class CmdLineCutterTest {

  // path with test data included in this project
  private static final String DATA_PATH = "tests/databank/fasta_prot/";
  // the data file to index
  private static final String DATA_FILE = "uniprot.faa";
  // the working directory for the test
  private static final String WK_DIR_BASE = 
      EZFileUtils.terminatePath(System.getProperty("java.io.tmpdir"))+
      EZFileUtils.terminatePath(CmdLineCutterTest.class.getName());
  // a second directory to check specific arguments of CmdLineIndexer tool
  private static final String WK_DIR_DATA = 
      WK_DIR_BASE+"data"+File.separator;

  //expected result files for the various tests
  private static final String RES_FILE_1 = "uniprot_3-end.faa";
  private static final String RES_FILE_2 = "uniprot_1-6.faa";
  private static final String RES_FILE_3 = "uniprot_7-9.faa";
  private static final String RES_FILE_4[] = {
      "uniprot_1-3.faa",
      "uniprot_4-6.faa",
      "uniprot_7-9.faa",
      "uniprot_10-10.faa"};
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // Called libraries (e.g. BeeDeeM sequence manager framework) rely on Log4J
    BasicConfigurator.configure();
    
    // if needed, clean working directory
    if (new File(WK_DIR_BASE).exists()) {
      assertTrue(EZFileUtils.deleteDirectory(WK_DIR_BASE));
    }
    
    // if needed, create working directory
    assertTrue(new File(WK_DIR_DATA).mkdirs());
    
    // copy sequence data file to working directory
    EZFileUtils.copyFile(new File(DATA_PATH+DATA_FILE), new File(WK_DIR_BASE+DATA_FILE));
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    assertTrue(EZFileUtils.deleteDirectory(WK_DIR_BASE));
  }

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void test_simple_range_from() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-f", "3"});                //get 3rd sequence up to the end of input file
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    File f = new File(WK_DIR_BASE+RES_FILE_1);
    assertTrue(f.exists());
    // immediately delete file to avoid pollute other tests
    assertTrue(f.delete());
  }
  @Test
  public void test_simple_range_to() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-t", "6"});                //get sequences from 1 up to 6 from input file
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    File f = new File(WK_DIR_BASE+RES_FILE_2);
    assertTrue(f.exists());
    // immediately delete file to avoid pollute other tests
    assertTrue(f.delete());
  }
  @Test
  public void test_simple_range_from_to() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-f", "7","-t", "9"});      //get sequences from 7 up to 9 from input file
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    File f = new File(WK_DIR_BASE+RES_FILE_3);
    assertTrue(f.exists());
    // immediately delete file to avoid pollute other tests
    assertTrue(f.delete());
  }
  @Test
  public void test_simple_range_from_to_outdir() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-d", WK_DIR_DATA,          // create result file in that directory instead of
                                    // default (next to input file)
        "-f", "7","-t", "9"});      //get sequences from 7 up to 9 from input file
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    File f = new File(WK_DIR_DATA+RES_FILE_3);
    assertTrue(f.exists());
    // immediately delete file to avoid pollute other tests
    assertTrue(f.delete());
  }

  @Test
  public void test_part() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-p", "3"});                // cut input file using slices of 3 sequences
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    for(String slice : RES_FILE_4) {
      File f = new File(WK_DIR_BASE+slice);
      assertTrue(f.exists());
      // immediately delete file to avoid pollute other tests
      assertTrue(f.delete());
    }
  }

  @Test
  public void test_part_to_outdir() {
    // this is the most basic way of using the cutting tool: we provide a single file 
    // and a basic sequence range
    boolean bRet = CmdLineCutter.doJob(new String[] {
        "-i", WK_DIR_BASE+DATA_FILE,// input file (contains 10 sequences)
        "-d", WK_DIR_DATA,          // create result file in that directory instead of
                                    // default (next to input file)
        "-p", "3"});                // cut input file using slices of 3 sequences
    
    // cutting OK?
    assertTrue(bRet);
    
    // data index does exist?
    for(String slice : RES_FILE_4) {
      File f = new File(WK_DIR_DATA+slice);
      assertTrue(f.exists());
      // immediately delete file to avoid pollute other tests
      assertTrue(f.delete());
    }
  }

}
