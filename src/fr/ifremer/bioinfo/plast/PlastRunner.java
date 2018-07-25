package fr.ifremer.bioinfo.plast;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.inria.genscale.dbscan.api.IRequest;
import org.inria.genscale.dbscan.impl.plast.PlastSystem;

import bzh.plealog.bioinfo.api.data.searchresult.io.SRWriter;
import bzh.plealog.bioinfo.io.searchresult.SerializerSystemFactory;
import bzh.plealog.dbmirror.main.CmdLineUtils;
import bzh.plealog.dbmirror.main.StarterUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import fr.ifremer.bioinfo.bdm.tools.CmdLineCommon;
import fr.ifremer.bioinfo.resources.CmdMessages;

/**
 * A utility class to run PLAST.<br>
 * <br>
 * 
 * Sample uses: <br>
 * PlastRunner -p plastp -i tests/databank/plast/query.fa -d tests/databank/plast/tursiops.fa -o /tmp/plast.res<br>
 * 
 * Use program without any arguments to get help.<br>
 * Note: environment variables are accepted in file path.<br>
 * 
 * A log file called PlastRunner.log is created within ${java.io.tmpdir}. It is
 * worth noting that this log file is not thread safe. Using many PlastRunner jobs
 * in parallel is possible: redirect logs of each job to a dedicated log file, as
 * follows: use JRE variables KL_WORKING_DIR and KL_LOG_FILE, e.g. 
 * -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=plast-001.log<br>
 * <br>
 * 
 * To run the software, you must configure the following JRE argument: 
 * 		-Djava.library.path=${project_loc}/native      (if using Eclipse)
 *      or
 * 		-Djava.library.path=/absolute/path/to/native   (otherwise)
 * 
 * to enable this code to finding the native c++ PLAST library.
 * 
 * Java runtime required: 1.8 or above
 * 
 * @author Patrick G. Durand, Ifremer
 * */
public class PlastRunner {
  protected static final Log  LOGGER     = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".PlastRunner");

  protected static final String DEFAULT_CORES = "4";
  
  // we expose most important PLAST arguments as Java command-line arguments:
  
  // the program
  protected static final String  PRGM_ARG   = "p";
  // the query
  protected static final String  QUERY_ARG  = "i";
  // the reference bank
  protected static final String  DB_ARG     = "d";
  // the output
  protected static final String  OUT_ARG    = "o";
  // the number of cores
  protected static final String  CORES_ARG  = "a";
  // max number of hits per query
  protected static final String  MAX_HITS_ARG   = "maxhits";
  // max number of HSPs per hit
  protected static final String  MAX_HSPS_ARG   = "maxhsps";
  // evalue threshold
  protected static final String  EVALUE_ARG     = "e";
  // seeds-ratio (PLAST specific: tune sensitivity vs. speed)
  // http://plast.inria.fr/user-guide/plast-command-line-arguments/#Optimizing_PLAST_at_runtime_using_seed-ratio
  protected static final String  SEEDS_ARG   = "seeds";
  // filter query sequence for low complexity regions
  protected static final String  FILTER_ARG   = "F";
  
  // ... adapt code if you want to enable passing in matrix, gap costs, etc. Not done for now.
  //     (review output of "plast -h" command line tool)
  
  
  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option prgm = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg1.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg1.desc"))
        .create(PRGM_ARG);
    Option query = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg2.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg2.desc"))
        .create(QUERY_ARG);
    Option subject = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg3.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg3.desc"))
        .create(DB_ARG);
    Option output = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg4.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg4.desc"))
        .create(OUT_ARG);
    String msg = String.format(CmdMessages.getString("Tool.Plast.arg5.desc"), DEFAULT_CORES);
    Option cores = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg5.lbl"))
        .hasArg()
        .withDescription(msg)
        .create(CORES_ARG);
    Option max_hits = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg6.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg6.desc"))
        .create(MAX_HITS_ARG);
    Option max_hsps = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg7.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg7.desc"))
        .create(MAX_HSPS_ARG);
    Option evalue = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg8.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg8.desc"))
        .create(EVALUE_ARG);
    Option seeds = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg9.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg9.desc"))
        .create(SEEDS_ARG);
    Option filter = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Plast.arg10.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Plast.arg10.desc"))
        .create(FILTER_ARG);

    opts = new Options();
    opts.addOption(prgm);
    opts.addOption(query);
    opts.addOption(subject);
    opts.addOption(output);
    opts.addOption(cores);
    opts.addOption(max_hits);
    opts.addOption(max_hsps);
    opts.addOption(evalue);
    opts.addOption(seeds);
    opts.addOption(filter);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  /**
   * Run a PLAST job.
   * 
   * @param prgm PLAST program to use. Mandatory.
   * @param query path to file containing the query. Mandatory.
   * @param subject path to file containing the subject reference bank. Mandatory.
   * @param output path to file that will contain PLAST results. Mandatory.
   * @param cores number of cores to use. Can be null, default is set by DEFAULT_CORES.
   * @param max_hits max number of hits per query. Can be null, default is all.
   * @param max_hsps max number of HSPs per hit. Can be null, default is all.
   * @param evalue evalue threshold. Can be null, default is 10.
   * @param seeds ratio of seeds to be used in the float-based range ]0..1] (default: 1).
   * @param filter query sequence for low complexity regions. Can be null, default is T.
   * 
   * @return true or false whether PLAST succeeded or not. In case of error, check log file.
   * */
	public boolean runPlast(String prgm, String query, String subject, String output, String cores,
	    String max_hits, String max_hsps, String evalue, String seeds, String filter)
	{
	  boolean bRet = true;
	  String msg;
	  
		/** STEP 1: setup the PLAST job*/
	  /* PLAST java API, see http://plast.gforge.inria.fr/docs/java/ */
		/* we setup a Properties object with PLAST mandatory arguments: query, subject and method.*/
		Properties props = new Properties();
		props.setProperty (IRequest.QUERY_URI, query);
		props.setProperty (IRequest.SUBJECT_URI, subject);
		props.setProperty (IRequest.ALGO_TYPE, prgm);
		/*Notice: you can access all PLAST arguments using IRequest interface, as they are available
		 * using PLAST command line tool.*/

		/* Using PLAST Java API implies that output argument is not required. However, it is highly recommended
		 * to set such a file to help PLAST using temporary files during job execution; otherwise it will create
		 * a temporary file of its own in the current directory.*/
		File fOut = null;
		try {
		  fOut = File.createTempFile("plast", ".tmp", new File(DBMSAbstractConfig.getWorkingPath()));
    } catch (IOException e1) {
      msg = String.format(CmdMessages.getString("Tool.Plast.msg3"), DBMSAbstractConfig.getWorkingPath(), e1.toString());
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
		/* as stated just above, we DO NOT use native PLAST output file in this application.
		 * See Step below: we will save a NCBI BLAST like report.*/
		props.setProperty (IRequest.OUTPUT_URI,  fOut.getAbsolutePath());

		/* not required, but useful: set the number of cores to use. If not set, PLAST uses all available
		 * cores on the computer.*/
    props.setProperty (IRequest.NB_PROCESSORS, cores==null ? DEFAULT_CORES : cores);

		/* When the two following parameters are not provided, PLAST returns *ALL* hits/hsps. So, we
		 * restrict the size of the output to 10 hits per query at max and 10 HSPs per hit at max. It is
		 * worth noting that the latter parameter does not exist in BLAST parameter.
		 */
		if (max_hits!=null) {
		  props.setProperty (IRequest.MAX_HIT_PER_QUERY, max_hits);
		}
		if (max_hsps!=null) {
		  props.setProperty (IRequest.MAX_HSP_PER_HIT, max_hsps);
		}

		/* PLAST is a bank to bank sequence comparison tool. Its algorithm is made such that hits are not 
		 * reported using query order as they appear in the query file. Using the following argument forces
		 * PLAST to sort hits following query order. Do not care about "1000" value, it is for internal use.
		 */
		props.setProperty(IRequest.FORCE_QUERY_ORDERING, "1000");

		/*
		 * Additional PLAST options
		 */
    if (evalue!=null) {
      props.setProperty (IRequest.EVALUE, evalue);
    }
    if (seeds!=null) {
      props.setProperty (IRequest.SEEDS_USE_RATIO, seeds);
    }
    if (filter!=null) {
      props.setProperty (IRequest.FILTER_QUERY, filter);
    }
		
		/** STEP 2: create the PLAST request. */

		/* A request is not yet a PLAST job. At this stage, the PLAST engine is only prepared to run a job...*/
		IRequest req = PlastSystem.getRequestManager().createRequest(props);

		/* ... and before starting a job, we need to register a listener. This is the way PLAST will provide
		 * us with results (i.e. hits and HSPs): by way of a IRequestListener */
		PlastHandler pr = new PlastHandler(prgm, subject);
		req.addListener(pr);

		/** STEP3: run PLAST job. */
		try {
			req.execute();
		} catch (Exception e2) {  
      msg = String.format(CmdMessages.getString("Tool.Plast.msg4"), e2.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
		}

		/** STEP 4: provide search summary in log file and save a data file... */
    msg = String.format(CmdMessages.getString("Tool.Plast.msg5"), pr.getQueries());
    LoggerCentral.info(LOGGER, msg);    
    msg = String.format(CmdMessages.getString("Tool.Plast.msg10"), pr.getMatchingQueries());
    LoggerCentral.info(LOGGER, msg);
    msg = String.format(CmdMessages.getString("Tool.Plast.msg6"), pr.getHits());
    LoggerCentral.info(LOGGER, msg);
    msg = String.format(CmdMessages.getString("Tool.Plast.msg7"), pr.getHsps());
    LoggerCentral.info(LOGGER, msg);
	  // setup an NCBI Blast Writer (XML)
    msg = String.format(CmdMessages.getString("Tool.Plast.msg9"), output);
    LoggerCentral.info(LOGGER, msg);
    try {
  		SRWriter ncbiBlastWriter = SerializerSystemFactory.getWriterInstance(SerializerSystemFactory.NCBI_WRITER);
  		ncbiBlastWriter.write(new File(output), pr.getResult());
    } catch (Exception e3) {  
      msg = String.format(CmdMessages.getString("Tool.Plast.msg8"), output, e3.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }

		/** STEP 5: do some cleanup... */
		/* We do not want to keep output file: so, delete it!*/
		fOut.delete();
		/* This is a sample application that is going to terminate now. So removing the listener
		 * from the PLAST engine is not required here, but is is definitively required for real 
		 * applications, such as GUI-based that may run many jobs in the row. Not doing the following
		 * call may results in weird application behavior, including crash!*/
		req.removeListener(pr);
		
		LoggerCentral.info(LOGGER, msg);
		
		return bRet;
	}


  /**
   * Prepare and run a PLAST job.
   * 
   * @param args
   *          command line arguments
   * 
   * @return true if job is ok, false otherwise.
   */
  public static boolean doJob(String[] args) {
    CommandLine cmdLine;
    String msg, toolName, query, subject, prgm, output, cores, max_hits, max_hsps, evalue, seeds, filter;
    Options options;
    
    toolName = CmdMessages.getString("Tool.Plast.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    LoggerCentral.info(LOGGER, "*** Starting " + toolName);

    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }
    
    // control some important user-provided values
    query = cmdLine.getOptionValue(QUERY_ARG);
    if (new File(query).exists()==false) {
      msg = String.format(CmdMessages.getString("Tool.Plast.msg1"), query);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    subject = cmdLine.getOptionValue(DB_ARG);
    if (new File(subject).exists()==false) {
      msg = String.format(CmdMessages.getString("Tool.Plast.msg2"), subject);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    prgm = cmdLine.getOptionValue(PRGM_ARG);
    output = cmdLine.getOptionValue(OUT_ARG);
    cores = cmdLine.getOptionValue(CORES_ARG);
    max_hits = cmdLine.getOptionValue(MAX_HITS_ARG);
    max_hsps = cmdLine.getOptionValue(MAX_HSPS_ARG);
    evalue = cmdLine.getOptionValue(EVALUE_ARG);
    seeds = cmdLine.getOptionValue(SEEDS_ARG);
    filter = cmdLine.getOptionValue(FILTER_ARG);
    
    // GO!
    PlastRunner runner = new PlastRunner();
    long tim = System.currentTimeMillis();
    boolean bRet = runner.runPlast(prgm, query, subject, output, cores, max_hits, max_hsps, evalue, seeds, filter);
    msg = CmdLineCommon.getRunningTime((System.currentTimeMillis()-tim)/1000l);
    msg = String.format(CmdMessages.getString("Tool.Plast.msg11"), msg);
    LoggerCentral.info(LOGGER, msg);
    return bRet;
  }
  
	/**
   * Start application.
   * 
   * @param args
   *          command line arguments
   */
  public static void main(String[] args) {
    CmdLineCommon.informForErrorMsg(!doJob(args));
  }

}
