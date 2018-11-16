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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.api.data.searchresult.io.SRLoader;
import bzh.plealog.bioinfo.io.searchresult.SerializerSystemFactory;
import bzh.plealog.bioinfo.io.searchresult.csv.CSVExportSROutput;
import bzh.plealog.bioinfo.io.searchresult.csv.CSVExportSROutputHandler;
import bzh.plealog.bioinfo.io.searchresult.txt.TxtExportSROutput;
import bzh.plealog.dbmirror.annotator.SRAnnotatorUtils;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTerm;
import bzh.plealog.dbmirror.main.CmdLineUtils;
import bzh.plealog.dbmirror.main.StarterUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import fr.ifremer.bioinfo.resources.CmdMessages;

/**
 * A utility class to dump PLAST/BLAST XML/ZML data file as CSV files.<br>
 * <br>
 * 
 * Sample uses: <br>
 * CmdLineDumper -i tests/datafile/hits_with_full_annot.zml -f zml<br>
 * -> result is dumped on stdout<br>
 * <br>
 *
 * CmdLineDumper -i tests/datafile/hits_with_full_annot.zml -f zml -o results.csv<br>
 * -> result is dumped in file results.csv<br>
 * <br>
 * 
 * CmdLineDumper -i tests/datafile/hits_with_full_annot.zml -f zml -o results.csv -c "22,23,24,25"<br>
 * -> result is dumped in file results.csv using user-defined columns<br>
 * (see program help for more information about -c argument)
 * <br>
 * 
 * CmdLineDumper -i tests/datafile/hits_with_full_annot.zml -f zml -c "22,23,24,25" -ec "/biobank/d/Enzyme/current/Enzyme/Enzyme.ldx"<br>
 * -> result is dumped on stdout using user-defined columns and providing an EC BeeDeeM index.<br>
 * See program help for more information about -c argument; using an BeeDeeM index enables to expand
 * classification data. However it requires PLAST/BLAST XML/ZML results already contains classification data.
 * This can be done using BeeDeeM Annotator Tool.<br>
 * CmdLineDumper also handles NCBI Taxonomy (-tax xxx), Gene Ontology (-go xxx) and InterPro (-ec xxx).<br>
 * <br>
 * 
 * Use program without any arguments to get help.<br>
 * Note: environment variables are accepted in file path.<br>
 * 
 * A log file called TextDump.log is created within ${java.io.tmpdir}.
 * This default log file can be redirected using JRE variables KL_WORKING_DIR
 * and KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path
 * -DKL_LOG_FILE=query.log<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineDumper {
  // format of input file: xml or zml; if not provided: xml
  private static final String                        FORMAT_ARG   = "f";
  // input data file
  protected static final String                      FILE_ARG     = "i";
  // output data format
  protected static final String                      OUTFMT_ARG   = "c";
  // output file
  protected static final String                      OUTFILE_ARG  = "o";
  // path to NCBI Taxonomy BeeDeeM index (.ldx folder)
  protected static final String                      DICO_TAX_ARG = "tax";
  // path to GO BeeDeeM index (.ldx folder)
  protected static final String                      DICO_GO_ARG  = "go";
  // path to Enzyme BeeDeeM index (.ldx folder)
  protected static final String                      DICO_EC_ARG  = "ec";
  // path to InterPro BeeDeeM index (.ldx folder)
  protected static final String                      DICO_IPR_ARG = "ipr";
  // if set only show best hit
  protected static final String                      BEST_HIT_ARG = "bho";
  // if set only show first hsp
  protected static final String                      FIRST_HSP_ARG = "fho";
  
  // constant used to check kind of input file
  private static final String                        NCBI_LEGACY_XML = "xml";
  // constant used if no mapping found between ID and BeeDeeM index
  private static final String                        UNK = "unknown";
  // output format data mapper between user-provided values and software internals
  private static HashMap<String, Integer>            OUTFMP_MAP;
  // default out-fmt
  private static String                              DEFAULT_OUTFMP_MAP = "0,1,2,7,4,6,10,11,12,14,16,17,19";

  // class logger
  private static final Log                           LOGGER     = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".CmdLineDumper");

  static {
    OUTFMP_MAP = new HashMap<>();
    OUTFMP_MAP.put("0", TxtExportSROutput.ACCESSION);
    OUTFMP_MAP.put("1", TxtExportSROutput.DEFINITION);
    OUTFMP_MAP.put("2", TxtExportSROutput.LENGTH);
    OUTFMP_MAP.put("3", TxtExportSROutput.NBHSPS);
    OUTFMP_MAP.put("4", TxtExportSROutput.SCORE);
    OUTFMP_MAP.put("5", TxtExportSROutput.SCORE_BITS);
    OUTFMP_MAP.put("6", TxtExportSROutput.EVALUE);
    OUTFMP_MAP.put("7", TxtExportSROutput.IDENTITY);
    OUTFMP_MAP.put("8", TxtExportSROutput.POSITIVE);
    OUTFMP_MAP.put("9", TxtExportSROutput.GAPS);
    OUTFMP_MAP.put("10", TxtExportSROutput.ALI_LEN);
    OUTFMP_MAP.put("11", TxtExportSROutput.Q_FROM);
    OUTFMP_MAP.put("12", TxtExportSROutput.Q_TO);
    OUTFMP_MAP.put("13", TxtExportSROutput.Q_GAPS);
    OUTFMP_MAP.put("14", TxtExportSROutput.Q_FRAME);
    OUTFMP_MAP.put("15", TxtExportSROutput.Q_COVERAGE);
    OUTFMP_MAP.put("16", TxtExportSROutput.H_FROM);
    OUTFMP_MAP.put("17", TxtExportSROutput.H_TO);
    OUTFMP_MAP.put("18", TxtExportSROutput.H_GAP);
    OUTFMP_MAP.put("19", TxtExportSROutput.H_FRAME);
    OUTFMP_MAP.put("20", TxtExportSROutput.H_COVERAGE);
    OUTFMP_MAP.put("21", TxtExportSROutput.BIO_CLASSIF);
    OUTFMP_MAP.put("22", TxtExportSROutput.BIO_CLASSIF_TAX);
    OUTFMP_MAP.put("23", TxtExportSROutput.BIO_CLASSIF_GO);
    OUTFMP_MAP.put("24", TxtExportSROutput.BIO_CLASSIF_IPR);
    OUTFMP_MAP.put("25", TxtExportSROutput.BIO_CLASSIF_EC);
    OUTFMP_MAP.put("26", TxtExportSROutput.ORGANISM);
  }
  
  /**
   * Internal class used to handle mapping of biological classification.
   * BeeDeeM-based.
   * */
  private static class MyHandler implements CSVExportSROutputHandler{
    private DicoTermQuerySystem dicoTermQuerySystem = null;
    
    private MyHandler(Map<String, String> dicos) {
      if (dicos!=null) {
        dicoTermQuerySystem = DicoTermQuerySystem.getDicoTermQuerySystem(dicos);
      }
    }
    private DicoTermQuerySystem getDicoTermQuerySystem() {
      return dicoTermQuerySystem;
    }
    private void close() {
      if (dicoTermQuerySystem!=null) {
        DicoTermQuerySystem.closeDicoTermQuerySystem();
      }
    }
    private String getDescription(String entryID, DicoTermQuerySystem dicoConnector) {
      DicoTerm term;
      String dicoType, id, desc;
      int i;
      
      // entryID may have the following form: TAX:xxx, GO:xxx, EC:xxx, IPRxxx
      // so we separate bank type and ID
      i= entryID.indexOf(':');
      if (i!=-1) {
        dicoType = entryID.substring(0, i).toLowerCase();
        id = entryID.substring(i+1);
      }
      else {
        dicoType = entryID.toLowerCase();
        id = entryID;
      }
      //query BeeDeeM indexes
      desc=UNK;
      try {
        if (dicoType.startsWith(Dicos.NCBI_TAXONOMY.readerId.toLowerCase())) {
          term = dicoConnector.getTerm(Dicos.NCBI_TAXONOMY, id);
          if (term != null) {
            desc = dicoConnector.getTaxPath(id, true, true, true);
          }
        }
        else if (dicoType.startsWith(Dicos.GENE_ONTOLOGY.readerId.toLowerCase())) {
          term = dicoConnector.getTerm(Dicos.GENE_ONTOLOGY, Dicos.GENE_ONTOLOGY.xrefId+":"+id);
          if (term != null) {
            GeneOntologyTerm goTerm = (GeneOntologyTerm) term.get_dataObject();
            desc = goTerm.get_node_ontology_code() + ":" + goTerm.get_node_name();
          }
        }
        else if (dicoType.startsWith(Dicos.INTERPRO.readerId.toLowerCase())) {
          term = dicoConnector.getTerm(Dicos.INTERPRO, id);
          if (term != null) {
            desc = term.getDataField().toString();
          }
        }
        else if (dicoType.startsWith(Dicos.ENZYME.readerId.toLowerCase())) {
          term = dicoConnector.getTerm(Dicos.ENZYME, id);
          if (term != null) {
            desc = term.getDataField().toString();
          }
        }
      } catch (Exception e) {
        String msg = String.format(CmdMessages.getString("Tool.Dumper.msg4"), entryID, e.toString());
        LoggerCentral.error(LOGGER, msg);
      }
      return desc;
    }
    @Override
    public String handle(String s, int colType) {
      
      if ( ! (colType==TxtExportSROutput.BIO_CLASSIF ||
              colType==TxtExportSROutput.BIO_CLASSIF_TAX ||
              colType==TxtExportSROutput.BIO_CLASSIF_GO ||
              colType==TxtExportSROutput.BIO_CLASSIF_IPR ||
              colType==TxtExportSROutput.BIO_CLASSIF_EC) ) {
        return s;
      }
      else {
        StringBuffer buf = new StringBuffer();
        //remove enclosing quotes of string s
        String dicoIds = s.substring(1, s.length()-1);
        StringTokenizer tokenizer = new StringTokenizer(dicoIds, ";");
        buf.append("\"");
        while(tokenizer.hasMoreTokens()) {
          String token = tokenizer.nextToken();
          buf.append(token);
          if (dicoTermQuerySystem!=null) {
            token = getDescription(token, dicoTermQuerySystem);
            if (token.equals(UNK) == false) {
              buf.append(":");
              buf.append(token);
            }
          }
          if (tokenizer.hasMoreTokens()) {
            buf.append(";");
          }
        }
        buf.append("\"");
        return buf.toString();
      }
    }
    
  }
  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option input = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg1.lbl"))
        .isRequired()
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg1.desc"))
        .create(FILE_ARG);
    Option format = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg2.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg2.desc"))
        .create(FORMAT_ARG);
    Option outfmt = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg3.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg3.desc"))
        .create(OUTFMT_ARG);
    Option taxdico = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg4.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg4.desc"))
        .create(DICO_TAX_ARG);
    Option godico = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg5.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg5.desc"))
        .create(DICO_GO_ARG);
    Option ecdico = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg6.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg6.desc"))
        .create(DICO_EC_ARG);
    Option iprdico = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg7.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg7.desc"))
        .create(DICO_IPR_ARG);
    Option outfile = OptionBuilder
        .withArgName(CmdMessages.getString("Tool.Dumper.arg8.lbl"))
        .hasArg()
        .withDescription(CmdMessages.getString("Tool.Dumper.arg8.desc"))
        .create(OUTFILE_ARG);
    Option bestHitOnly = OptionBuilder
        .withDescription(CmdMessages.getString("Tool.Dumper.arg9.desc"))
        .create(BEST_HIT_ARG);
    Option firstHspOnly = OptionBuilder
        .withDescription(CmdMessages.getString("Tool.Dumper.arg10.desc"))
        .create(BEST_HIT_ARG);

    opts = new Options();
    opts.addOption(input);
    opts.addOption(format);
    opts.addOption(outfmt);
    opts.addOption(taxdico);
    opts.addOption(godico);
    opts.addOption(ecdico);
    opts.addOption(iprdico);
    opts.addOption(outfile);
    opts.addOption(bestHitOnly);
    opts.addOption(firstHspOnly);
    
    CmdLineUtils.setHelpOption(opts);

    return opts;
  }

  /**
   * Get a data loader.
   * 
   * @param format either xml or zml string
   * 
   * @return a SRLoader instance or null if format is unknown
   */
  private static SRLoader getFileLoader(String format) {
    if ("zml".equalsIgnoreCase(format)) {
      return SerializerSystemFactory
          .getLoaderInstance(SerializerSystemFactory.NATIVE_LOADER);
    }
    else if ("xml".equalsIgnoreCase(format)) {
      return SerializerSystemFactory
          .getLoaderInstance(SerializerSystemFactory.NCBI_LOADER);
    }
    else {
      return null;
    }
  }
  
  private static boolean dumpData(boolean ncbiXmlLike,String dataFile, SRLoader loader, String outputFile, int colsIds[],
      String taxPath, String goPath, String ecPath, String iprPath, boolean bestHitOnly, boolean firstHspOnly) {
    String msg;
    boolean bRet = true;
    File f = new File(dataFile);
    MyHandler dataHandler;
    
    if (f.exists()==false) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg2"), dataFile);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    // read XML/ZML plast/blast file
    msg = String.format(CmdMessages.getString("Tool.Dumper.msg8"), f.getAbsolutePath());
    LOGGER.info(msg);
    SROutput bo = loader.load(f);
    LOGGER.info(CmdMessages.getString("Tool.Dumper.msg11"));
    //Prepare CSV export
    CSVExportSROutput exporter = new CSVExportSROutput();
    exporter.showBestHitOnly(bestHitOnly);
    exporter.showFirstHspOnly(firstHspOnly);
    exporter.showColumnHeader(true);
    exporter.showQueryId(true);
    exporter.showQueryLength(false);
    exporter.showQueryName(false);
    
    //ajouter arg pour controller best-hit only
    
    HashMap<String, String> dicos;
    dicos = new HashMap<>();
    
    // Sample Biol. Class. BeeDeeM Lucene indexes used to test the tool:
    //-tax "/biobank/d/NCBI_Taxonomy/current/NCBI_Taxonomy/NCBI_Taxonomy.ldx"
    //-go "/biobank/d/GeneOntology_terms/current/GeneOntology_terms/GeneOntology_terms.ldx"
    //-ipr "/biobank/d/InterPro_terms/current/InterPro_terms/InterPro_terms.ldx"
    //-ec "/biobank/d/Enzyme/current/Enzyme/Enzyme.ldx"

    if (taxPath!=null && new File(taxPath).exists()) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg5"), DicoUtils.READER_NCBI_TAXONOMY.toUpperCase(), taxPath);
      LOGGER.info(msg);
      dicos.put(DicoUtils.READER_NCBI_TAXONOMY, taxPath);
    }
    if (goPath!=null && new File(goPath).exists()) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg5"), DicoUtils.READER_GENE_ONTOLOGY.toUpperCase(), goPath);
      LOGGER.info(msg);
      dicos.put(DicoUtils.READER_GENE_ONTOLOGY, goPath);
    }
    if (iprPath!=null && new File(iprPath).exists()) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg5"), DicoUtils.READER_INTERPRO.toUpperCase(), iprPath);
      LOGGER.info(msg);
      dicos.put(DicoUtils.READER_INTERPRO, iprPath);
    }
    if (ecPath!=null && new File(ecPath).exists()) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg5"), DicoUtils.READER_ENZYME.toUpperCase(), ecPath);
      LOGGER.info(msg);
      dicos.put(DicoUtils.READER_ENZYME, ecPath);
    }
    dataHandler = new MyHandler(dicos.isEmpty() ? null : dicos);
    exporter.setCSVExportSROutputHandler(dataHandler);
    exporter.ssetColumnIds(colsIds);

    // trick to enable handling of biological classification using NCBI XML data file
    if (dataHandler.getDicoTermQuerySystem()!=null && ncbiXmlLike) {
      LOGGER.info(CmdMessages.getString("Tool.Dumper.msg9"));
      SRAnnotatorUtils.extractDbXrefFromHitDefline(bo, dataHandler.getDicoTermQuerySystem());
      LOGGER.info(CmdMessages.getString("Tool.Dumper.msg11"));
    }
    
    LOGGER.info(CmdMessages.getString("Tool.Dumper.msg10"));
    try (BufferedWriter bw = new BufferedWriter(outputFile==null?new OutputStreamWriter(System.out):new FileWriter(outputFile))) {
      exporter.export(bw, bo);
    } catch (Exception ex) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg3"), ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }
    LOGGER.info(CmdMessages.getString("Tool.Dumper.msg11"));
    
    //safely close BeeDeeM Lucene indexes if any are opened
    dataHandler.close();
    
    return bRet;
  }
  
  private static int[] getColumnIDs(String outFmt) {
    String key, msg;
    int colsIds[], maxCols, value;
    
    maxCols = OUTFMP_MAP.size()-1;
    StringTokenizer tokenizer = new StringTokenizer(
        outFmt==null ? DEFAULT_OUTFMP_MAP : outFmt.trim(), ",");
    colsIds = new int[tokenizer.countTokens()];
    int idx=0;
    while(tokenizer.hasMoreTokens()) {
      key = tokenizer.nextToken();
      try {
        value = Integer.valueOf(key);
        if ( ! (value>=0 && value<=maxCols) ){
          throw new NumberFormatException(CmdMessages.getString("Tool.Dumper.msg7"));
        }
      } catch (NumberFormatException e) {
        msg = String.format(CmdMessages.getString("Tool.Dumper.msg6"), key, e.toString());
        LoggerCentral.error(LOGGER, msg);
        return null;
      }
      colsIds[idx] = OUTFMP_MAP.get(key);
      idx++;
    }
    return colsIds;
  }
  
  /**
   * Run cutting job.
   * 
   * @param args
   *          command line arguments
   * 
   * @return true if cutting is ok, false otherwise.
   * @throws FileNotFoundException 
   */
  public static boolean doJob(String[] args) {
    CommandLine cmdLine;
    String msg, toolName, inputFile, format, outputFile;
    Options options;
    SRLoader loader;
    int colsIds[];
    
    toolName = CmdMessages.getString("Tool.Dumper.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    LoggerCentral.info(LOGGER, "*** Starting " + toolName);

    // handle the command-line and control some values
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }
    inputFile = cmdLine.getOptionValue(FILE_ARG);
    
    format = cmdLine.getOptionValue(FORMAT_ARG);
    if (format==null) {
      format=NCBI_LEGACY_XML;
    }
    colsIds = getColumnIDs(cmdLine.getOptionValue(OUTFMT_ARG));
    if (colsIds==null) {
      return false;
    }
    outputFile = cmdLine.getOptionValue(OUTFILE_ARG);
    loader = getFileLoader(format);
    if (loader==null) {
      msg = String.format(CmdMessages.getString("Tool.Dumper.msg1"), format);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    // run job
    return dumpData(
          format.equals(NCBI_LEGACY_XML),
          inputFile, 
          loader, 
          outputFile, 
          colsIds,
          cmdLine.getOptionValue(DICO_TAX_ARG),
          cmdLine.getOptionValue(DICO_GO_ARG),
          cmdLine.getOptionValue(DICO_EC_ARG),
          cmdLine.getOptionValue(DICO_IPR_ARG),
          cmdLine.hasOption(BEST_HIT_ARG),
          cmdLine.hasOption(FIRST_HSP_ARG)
          );
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
