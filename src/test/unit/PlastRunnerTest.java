package test.unit;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.bioinfo.api.data.searchresult.SRHit;
import bzh.plealog.bioinfo.api.data.searchresult.SRIteration;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.api.data.searchresult.io.SRLoader;
import bzh.plealog.bioinfo.io.searchresult.SerializerSystemFactory;
import bzh.plealog.dbmirror.indexer.FastaParser;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;
import fr.ifremer.bioinfo.plast.PlastRunner;

/**
 * A class to test PlastRunner tool. Please note that unit tests for the PLAST
 * software are part of the c++ project. Here, we only test the Java tool layer.
 */
public class PlastRunnerTest {

  // path with test data included in this project
  private static final String DATA_PATH = "tests/databank/plast/";
  // the query file
  private static final String QUERY_FILE = "query.fa";
  // the bank file
  private static final String BANK_FILE = "tursiops.fa";
  // the reference file
  private static final String REF_FILE = "plast-reference-v2.3.1.txt";
  
  private static File data_file;
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // Called libraries (e.g. BeeDeeM sequence manager framework) rely on Log4J
    BasicConfigurator.configure();
    data_file = File.createTempFile("plast", ".tmp", new File(System.getProperty("java.io.tmpdir")));
    data_file.deleteOnExit();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void test_simply_run_plast() {
    // this is the most basic way of using the PLAST tool: program, query, subject, result
    boolean bRet = PlastRunner.doJob(new String[] {
        "-p", "plastp",
        "-i", DATA_PATH+QUERY_FILE,
        "-d", DATA_PATH+BANK_FILE,
        "-o", data_file.getAbsolutePath()
        });              
    
    // Job OK?
    assertTrue(bRet);
  }

  @Test
  public void test_check_plast_results() {
    // this is a more extended way of using the PLAST tool.
    // Caution: to compare results with reference data set, we MUST run PLAST on a single core.
    // Here, we'll compare results produced using the Java layer with data produced by PLAST
    // command-line tool.
    boolean bRet = PlastRunner.doJob(new String[] {
        "-p", "plastp",
        "-i", DATA_PATH+QUERY_FILE,
        "-d", DATA_PATH+BANK_FILE,
        "-o", data_file.getAbsolutePath(),
        "-maxhits", "5",
        "-maxhsps", "1", 
        "-e", "1e-5", 
        "-seeds", "0.01", 
        "-F", "F",
        "-a", "1"
        });              
    assertTrue(bRet);
    
    // read PLAST result freshly created
    SRLoader ncbiBlastLoader = SerializerSystemFactory.getLoaderInstance(SerializerSystemFactory.NCBI_LOADER);
    SROutput bo = ncbiBlastLoader.load(data_file);
    assertTrue(bo!=null);
    
    // how many queries do we have and what are their IDs?
    FastaParser spp = new FastaParser();
    FastaParserMonitor monitor = new FastaParserMonitor();
    spp.setVerbose(false);
    spp.setParserMonitor(monitor);
    spp.parse(DATA_PATH+QUERY_FILE, null);
    assertTrue(bo.countIteration()==spp.getEntries());
    
    // check whether or not we have indeed same nb of queries in same order as Fasta query file
    Enumeration<SRIteration> srIterEnum = bo.enumerateIteration();
    ArrayList<String> hitIdsList = new ArrayList<>();
    List<String> idsList = monitor.getIDs();
    SRIteration srIter;
    int count=0;
    long countTotalHits=0, numOfLines=0;
    while (srIterEnum.hasMoreElements()) {
      srIter = srIterEnum.nextElement();
      assertTrue(srIter.getIterationQueryID().equals(idsList.get(count)));
      count++;
      countTotalHits+=srIter.countHit();
      Enumeration<SRHit> srHitEnum = srIter.enumerateHit();
      while(srHitEnum.hasMoreElements()) {
        hitIdsList.add(srHitEnum.nextElement().getHitId());
      }
    }
    
    // now, let's check results
    try (Stream<String> lines = Files.lines(Paths.get(DATA_PATH+REF_FILE), Charset.defaultCharset())) {
      numOfLines = lines.count();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    assertTrue(numOfLines==countTotalHits);
    assertTrue(hitIdsList.size()==numOfLines);

    List<String> transactionIds=null;
    try (Stream<String> lines = Files.lines(Paths.get(DATA_PATH+REF_FILE), Charset.defaultCharset())) {
      transactionIds = lines.map(line -> line.split("\t"))
           .map(arr -> arr[1])
           .collect(Collectors.toList());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    for(count=0;count<numOfLines;count++) {
      assertTrue(transactionIds.get(count).equals(hitIdsList.get(count)));
    }
  }
  
  private class FastaParserMonitor implements ParserMonitor{
    private ArrayList<String> idsList = new ArrayList<>();
    
    public List<String> getIDs(){
      return idsList;
    }
    @Override
    public boolean redundantSequenceFound() {
      return false;
    }

    @Override
    public void seqFound(String id, String name, String fName, long start, long stop, boolean checkRedundancy)
        throws DBMSUniqueSeqIdRedundantException {
      idsList.add(id);
    }

    @Override
    public void startProcessingFile(String fName, long fSize) {
    }

    @Override
    public void stopProcessingFile(String fName, int entries) {
    }
  }
}
