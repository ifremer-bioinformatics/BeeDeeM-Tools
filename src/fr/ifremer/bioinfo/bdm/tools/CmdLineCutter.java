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
package fr.ifremer.bioinfo.bdm.tools;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.main.CmdLineUtils;
import bzh.plealog.dbmirror.main.StarterUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorPaginate;
import fr.ifremer.bioinfo.resources.CmdMessages;

/**
 * A utility class to cut a sequence file.<br>
 * <br>
 * 
 * Sample uses: <br>
 * CmdLineCutter -i tests/databank/fasta_prot/uniprot.faa -f 3<br>
 * to get 3rd sequence up to the end of input file<br>
 * -> result file will be here: tests/databank/fasta_prot/uniprot_3-end.faa<br>
 * <br>
 *
 * CmdLineCutter -i tests/databank/fasta_prot/uniprot.faa -p 5<br>
 * cut input file into several parts, each of them containing 5 sequences<br>
 * -> result files will be (sample source file contains 10 sequences): <br>
 * a. tests/databank/fasta_prot/uniprot_1-5.faa<br>
 * b. tests/databank/fasta_prot/uniprot_6-10.faa<br>
 * <br>
 * 
 * Use program without any arguments to get help.<br>
 * Note: environment variables are accepted in file path.<br>
 * 
 * A log file called CutSequenceFile.log is created within ${java.io.tmpdir}.
 * This default log file can be redirected using JRE variables KL_WORKING_DIR
 * and KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path
 * -DKL_LOG_FILE=query.log<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineCutter {
  // from: start of a slice (one-based)
  // if not provided: start from 1
  private static final String                        FROM_ARG   = "f";
  // to: end of a slice (one-based)
  // if not provided: stop at file end
  private static final String                        TO_ARG     = "t";
  // part: nb of sequences for a single slice
  // if not provided: must use from/to
  private static final String                        PART_ARG   = "p";
  // input sequence file
  protected static final String                      FILE_ARG   = "i";
  // input sequence file format
  protected static final String                      FORMAT_ARG = "k";
  // where to create the resulting file?
  // if not provided: place the sliced file next to input sequence file
  protected static final String                      DIR_ARG    = "d";

  private static final Log                           LOGGER     = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".CmdLineCutter");

  // a convenient mapping to DatabankFormat format names.
  protected static Hashtable<String, DatabankFormat> formats;
  static {
    formats = new Hashtable<>();
    formats.put("fa", DatabankFormat.fasta);
    formats.put("fq", DatabankFormat.fastQ);
    formats.put("gb", DatabankFormat.genbank);
    formats.put("em", DatabankFormat.swissProt);
  }

  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option part = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg1.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg1.desc"))
        .create(PART_ARG);
    Option from = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg2.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg2.desc"))
        .create(FROM_ARG);
    Option to = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg3.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg3.desc"))
        .create(TO_ARG);
    Option file = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg4.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg4.desc"))
        .create(FILE_ARG);
    Option format = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg5.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg5.desc"))
        .create(FORMAT_ARG);
    Option res_dir = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Cutter.arg6.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Cutter.arg6.desc"))
        .create(DIR_ARG);

    opts = new Options();
    opts.addOption(part);
    opts.addOption(from);
    opts.addOption(to);
    opts.addOption(file);
    opts.addOption(format);
    opts.addOption(res_dir);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  private static void renameCreatedFile(String sequenceFile, String outDir, File filteredFile, int from, int to) {
    // we rename it using from-to values
    String sourceFileName = new File(sequenceFile).getName();
    // get file name and extension in separate strings
    int idx = sourceFileName.lastIndexOf('.');
    String fName = sourceFileName;
    String fExt = "";
    if (idx != -1) {
      fName = sourceFileName.substring(0, idx);
      fExt = "." + sourceFileName.substring(idx + 1);
    }
    // do we have a path ?
    String path = ( outDir != null ? outDir:filteredFile.getParent() );
    // prepare elements to be used to rename file
    String f = from == -1 ? "1" : String.valueOf(from);
    String t = to == -1 ? "end" : String.valueOf(to);
    String resultFile = path != null ? Utils.terminatePath(path) : "";
    resultFile += String.format("%s_%s-%s%s", fName, f, t, fExt);
    // log a little message
    String msg = String.format(CmdMessages.getString("Tool.Cutter.msg1"), resultFile);
    LoggerCentral.info(LOGGER, msg);
    System.out.println(msg);
    // rename !
    filteredFile.renameTo(new File(resultFile));
  }

  /**
   * Cut a sequence file into a slice.
   * 
   * @param sequenceFile
   *          the sequence file to cut
   * @param resultDir
   *          the directory to put the resulting slice. Optional. If not
   *          provided, the resulting file is saved next to sequence file.
   * @param format
   *          the format of the sequence file
   * @param from
   *          index of the first sequence to get from sequence file. Use either
   *          1 or -1 to start from beginning of source file.
   * @param to
   *          index of the last sequence to get from sequence file. Use -1 to
   *          target end of sequence file
   * 
   * @return true if file slicing is ok, false otherwise.
   */
  private static boolean cutFile(String sequenceFile, String resultDir, DatabankFormat format, int from, int to) {
    boolean bRet = true;

    sequenceFile = CmdLineUtils.expandEnvVars(sequenceFile);
    if (resultDir != null) {
      resultDir = CmdLineUtils.expandEnvVars(resultDir);
    }
    if (new File(sequenceFile).exists() == false) {
      String msg = String.format(CmdMessages.getString("Tool.Cutter.msg9"), sequenceFile);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    try {
      // create the sequence manager object
      SequenceFileManager sfm = new SequenceFileManager(sequenceFile, format, null, null);
      if (resultDir != null) {
        sfm.setTmpFileDirectory(resultDir);
      } else {
        String tmpDir = new File(sequenceFile).getParent();
        sfm.setTmpFileDirectory(tmpDir);
      }

      // sequence validator is a cutter
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(from, to);
      sfm.addValidator(validator);
      List<File> filteredFiles = sfm.execute();
      if (sfm.getNbSequencesFound() == 0) {
        throw new RuntimeException(CmdMessages.getString("Tool.Cutter.msg10"));
      }
      // get the single result: a file created by the sequence manager
      // and rename it using 'fom/to' values
      renameCreatedFile(sequenceFile, resultDir, filteredFiles.get(0), from, to);
    } catch (Exception ex) {
      String msg = String.format(CmdMessages.getString("Tool.Cutter.msg2"), ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Cut a sequence file into slices.
   * 
   * @param sequenceFile
   *          the sequence file to cut
   * @param resultDir
   *          the directory to put the resulting slice. Optional. If not
   *          provided, the resulting file is saved next to sequence file.
   * @param format
   *          the format of the sequence file
   * @param part
   *          size of a slice. Number of sequences. Use -1 to target end of
   *          sequence file
   * 
   * @return true if file slicing is ok, false otherwise.
   */
  private static boolean cutFile(String sequenceFile, String resultDir, DatabankFormat format, int part) {
    boolean bRet = true;

    sequenceFile = CmdLineUtils.expandEnvVars(sequenceFile);
    if (resultDir != null) {
      resultDir = CmdLineUtils.expandEnvVars(resultDir);
    }
    if (new File(sequenceFile).exists() == false) {
      String msg = String.format(CmdMessages.getString("Tool.Cutter.msg9"), sequenceFile);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    try {
      // create the sequence manager object
      SequenceFileManager sfm = new SequenceFileManager(sequenceFile, format, null, null);
      if (resultDir != null) {
        sfm.setTmpFileDirectory(resultDir);
      } else {
        String tmpDir = new File(sequenceFile).getParent();
        sfm.setTmpFileDirectory(tmpDir);
      }

      // sequence validator is a 'paginator'
      SequenceValidatorPaginate validator = new SequenceValidatorPaginate(part);
      sfm.addValidator(validator);
      sfm.execute();
      List<File> filteredFiles = sfm.execute();
      // do we get some results ?
      if (validator.getCreatedBatches().size() == 0) {
        throw new RuntimeException(CmdMessages.getString("Tool.Cutter.msg10"));
      }
      // rename generated files
      int from = 0, to = 0, nseqs = 0;
      for (Entry<File, Integer> entry : validator.getCreatedBatches().entrySet()) {
        nseqs = entry.getValue();
        to += nseqs;
        from = to - nseqs + 1;
        renameCreatedFile(sequenceFile, resultDir, entry.getKey(), from, to);
      }
      // do cleanup
      for(File f : filteredFiles) {
        f.delete();
        
      }
    } catch (Exception ex) {
      String msg = String.format(CmdMessages.getString("Tool.Cutter.msg2"), ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Convert a string to a integer.
   * 
   * @param val
   *          value to convert
   * 
   * @return the integer representation of argument
   */
  private static int getValue(String val) {
    if (val == null) {
      return -1;
    } else {
      return Integer.valueOf(val);
    }
  }

  /**
   * 
   * Return a bank format object given a command line bank format argument.
   * 
   * @param format
   *          command line bank format argument
   * 
   * @return a valid bank format object or null if provided command line bank
   *         format argument denotes a unknown bank format.
   */
  protected static DatabankFormat getDatabankFormat(String format) {
    DatabankFormat dbFormat = null;
    if (format == null) {
      dbFormat = DatabankFormat.fasta;// default is Fasta
    } else {
      dbFormat = formats.get(format);
      if (dbFormat == null) {
        String msg = String.format(CmdMessages.getString("Tool.Cutter.msg4"), format, formats.keySet().toString());
        LoggerCentral.error(LOGGER, msg);
        return null;
      }
    }
    return dbFormat;
  }

  /**
   * Run cutting job.
   * 
   * @param args
   *          command line arguments
   * 
   * @return true if cutting is ok, false otherwise.
   */
  public static boolean doJob(String[] args) {
    CommandLine cmdLine;
    String msg, toolName, part, from, to, file, format, resultDir;
    int ipart, ifrom, ito;
    Options options;
    DatabankFormat dbFormat;
    boolean bRet = true;

    toolName = CmdMessages.getString("Tool.Cutter.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    LoggerCentral.info(LOGGER, "*** Starting " + toolName);

    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }

    part = cmdLine.getOptionValue(PART_ARG);
    from = cmdLine.getOptionValue(FROM_ARG);
    to = cmdLine.getOptionValue(TO_ARG);
    file = cmdLine.getOptionValue(FILE_ARG);
    format = cmdLine.getOptionValue(FORMAT_ARG);
    resultDir = cmdLine.getOptionValue(DIR_ARG);

    // add additional controls on cmdline values
    if (part != null && (from != null || to != null)) {
      msg = CmdMessages.getString("Tool.Cutter.msg3");
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    // get input file format
    dbFormat = getDatabankFormat(format);
    if (dbFormat == null) {
      return false;
    }

    // convert Str to int
    ipart = getValue(part);
    ifrom = getValue(from);
    ito = getValue(to);

    // prepare a message for the user
    if (ipart != -1) {
      msg = String.format(CmdMessages.getString("Tool.Cutter.msg5"), file, ipart);
    } else if (ifrom != -1 && ito != -1) {
      msg = String.format(CmdMessages.getString("Tool.Cutter.msg6"), ifrom, ito, file);
    } else if (ito != -1) {
      msg = String.format(CmdMessages.getString("Tool.Cutter.msg7"), ito, file);
    } else {
      msg = String.format(CmdMessages.getString("Tool.Cutter.msg8"), ifrom, file);
    }
    LoggerCentral.info(LOGGER, msg);

    // compute new file
    if (ipart == -1) {
      bRet = cutFile(file, resultDir, dbFormat, ifrom, ito);
    } else {
      bRet = cutFile(file, resultDir, dbFormat, ipart);
    }
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
