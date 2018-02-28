package test.unit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import fr.ifremer.bioinfo.blast.CmdLineIndexer;

/**
 * A class to test CmdLineIndexer tool. Please note that unit tests for the entire
 * sequence file indexing framework is available from the BeeDeeM project. Here, we 
 * only test the tool layer.
 */
public class CmdLineIndexerTest {

  // path with test data included in this project
  private static final String DATA_PATH = "tests/databank/fasta_prot/";
  // the data file to index
  private static final String DATA_FILE = "uniprot.faa";
  // the working directory for the test
  private static final String WK_DIR_BASE = 
      EZFileUtils.terminatePath(System.getProperty("java.io.tmpdir"))+
      EZFileUtils.terminatePath(CmdLineIndexerTest.class.getName());
  // a second directory to check specific arguments of CmdLineIndexer tool
  private static final String WK_DIR_DATA = 
      WK_DIR_BASE+"data"+File.separator;
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // Called libraries (e.g. BeeDeeM sequence indexer framework) rely on Log4J
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
    // need to delete previous index if already exists
    String idxName = WK_DIR_BASE + DATA_FILE + LuceneUtils.DIR_OK_FEXT;
    if (new File(idxName).exists()) {
      assertTrue(EZFileUtils.deleteDirectory(idxName));
    }
  }

  @Test
  public void test_std() {
    // this is the most basic way of using the indexer tool: we provide a single file
    boolean bRet = CmdLineIndexer.doJob(new String[] {
        "-i",WK_DIR_BASE+DATA_FILE});// input file
    
    // indexing OK?
    assertTrue(bRet);
    
    // data index does exist?
    assertTrue(new File(WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT).exists());
  }
  @Test
  public void test_format() {
    // this is the second way of using the indexer tool: we provide the data file format
    boolean bRet = CmdLineIndexer.doJob(new String[] {
        "-i",WK_DIR_BASE+DATA_FILE, // input file
        "-k", "fa"});               // data format
    
    // indexing OK?
    assertTrue(bRet);

    // data index does exist?
    assertTrue(new File(WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT).exists());
  }
  @Test
  public void test_format_bad() {
    // this is the second way of using the indexer tool: we provide the data file format
    boolean bRet = CmdLineIndexer.doJob(new String[] {
        "-i",WK_DIR_BASE+DATA_FILE, // input file
        "-k", "em"});               // data format: wrong
    
    // indexing OK?
    assertFalse(bRet);

    // data index does exist?
    assertTrue(new File(WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT).exists());
  }
}
