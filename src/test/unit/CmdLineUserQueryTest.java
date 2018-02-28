package test.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import fr.ifremer.bioinfo.blast.CmdLineIndexer;
import fr.ifremer.bioinfo.blast.CmdLineUserQuery;

/**
 * A class to test CmdLineIndexer tool. Please note that unit tests for the entire
 * sequence file indexing framework is available from the BeeDeeM project. Here, we 
 * only test the tool layer.
 */
public class CmdLineUserQueryTest {

  // path with test data included in this project
  private static final String DATA_PATH = "tests/databank/fasta_prot/";
  // the data file to index
  private static final String DATA_FILE = "uniprot.faa";
  // the working directory for the test
  private static final String WK_DIR_BASE = 
      EZFileUtils.terminatePath(System.getProperty("java.io.tmpdir"))+
      EZFileUtils.terminatePath(CmdLineUserQueryTest.class.getName());
  // a second directory to check specific arguments of CmdLineIndexer tool
  private static final String WK_DIR_DATA = 
      WK_DIR_BASE+"data"+File.separator;
  
  private static final String RES_FILE_1 = "my-sequences.faa";
  
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
    
    // index the sequence file
    boolean bRet = CmdLineIndexer.doJob(new String[] {
        "-i",WK_DIR_BASE+DATA_FILE});// input file
    
    // indexing OK?
    assertTrue(bRet);
    
    // data index does exist?
    assertTrue(new File(WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT).exists());

  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    assertTrue(EZFileUtils.deleteDirectory(WK_DIR_BASE));
  }

  @Test
  public void test_std_ok() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index, // path to index
        "-i", "M4K2_HUMAN"});// existing sequence ID
    
    // querying OK?
    assertTrue(bRet);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==1);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==1);
  }

  @Test
  public void test_std_ok_outfile() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,            // path to index
        "-i", "M4K2_HUMAN",             // existing sequence ID
        "-o", WK_DIR_BASE+RES_FILE_1}); // where to save sequences ?
    
    // querying OK?
    assertTrue(bRet);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==1);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==1);
    assertTrue(new File(WK_DIR_BASE+RES_FILE_1).exists());
  }

  @Test
  public void test_std_ko() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index, // path to index
        "-i", "TREX"});      // a bad sequence ID
    
    // querying KO?
    assertFalse(bRet);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==1);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==0);
  }
  
  @Test
  public void test_std_manyIds_ok() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                       // path to index
        "-i", "M4K2_HUMAN,Q9NA00 , Q9PU23_TRASC"});// existing sequence IDs 
                                                   // (check also that space char do not matter) 
    
    // querying OK?
    assertTrue(bRet);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==3);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==3);
  }

  @Test
  public void test_std_manyIds_ko() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                       // path to index
        "-i", " M4K2_HUMAN , TREX,Q9PU23_TRASC "});// existing/not-existing sequence IDs
    
    // querying OK?
    assertFalse(bRet);
    assertTrue(CmdLineUserQuery.getInvalidIDs()==1);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==3);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==2);
  }

  @Test
  public void test_std_foIds_ok() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                       // path to index
        "-f", DATA_PATH+"fo-seqids.txt"});// existing/not-existing sequence IDs
    
    // querying OK?
    assertTrue(bRet);
    assertTrue(CmdLineUserQuery.getInvalidIDs()==0);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==5);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==5);
  }

  @Test
  public void test_std_foIds_ko() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                       // path to index
        "-f", DATA_PATH+"fo-seqids2.txt"});// existing/not-existing sequence IDs
    
    // querying OK?
    assertFalse(bRet);
    assertTrue(CmdLineUserQuery.getInvalidIDs()==1);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==5);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==4);
  }

  @Test
  public void test_std_complement_ok() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                       // path to index
        "-i", "M4K2_HUMAN,Q9NA00 , Q9PU23_TRASC",  // existing sequence IDs
        "-c"});                                    // get the complement, i.e. all sequences but provided ones
    // querying OK?
    assertTrue(bRet);
    assertTrue(CmdLineUserQuery.getInvalidIDs()==0);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==3);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==7);
  }

  @Test
  public void test_std_complement_ko() {
    String path_to_index=WK_DIR_BASE+DATA_FILE+LuceneUtils.DIR_OK_FEXT;
    // this is the most basic way of using the query tool: 
    // we provide path to sequence index and a sequence ID
    boolean bRet = CmdLineUserQuery.doJob(new String[] {
        "-d", path_to_index,                   // path to index
        "-i", "M4K2_HUMAN,TREX,Q9PU23_TRASC",  // existing sequence IDs
        "-c"});                                // get the complement, i.e. all sequences but provided ones
    
    // querying OK?
    assertTrue(bRet);//when using "-c", invalid seqID noes not matter
    assertTrue(CmdLineUserQuery.getInvalidIDs()==0);
    assertTrue(CmdLineUserQuery.getProvidedIDs()==3);
    assertTrue(CmdLineUserQuery.getRetrievedIDs()==8);
  }

}
