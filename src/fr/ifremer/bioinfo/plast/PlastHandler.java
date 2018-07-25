package fr.ifremer.bioinfo.plast;

import java.io.File;
import java.util.Properties;

import org.inria.genscale.dbscan.api.IHsp;
import org.inria.genscale.dbscan.api.IQueryResult;
import org.inria.genscale.dbscan.api.IRequest;
import org.inria.genscale.dbscan.api.IRequestListener;
import org.inria.genscale.dbscan.api.IRequestResult;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.bioinfo.api.core.config.CoreSystemConfigurator;
import bzh.plealog.bioinfo.api.data.searchresult.SRHit;
import bzh.plealog.bioinfo.api.data.searchresult.SRHsp;
import bzh.plealog.bioinfo.api.data.searchresult.SRHspScore;
import bzh.plealog.bioinfo.api.data.searchresult.SRHspSequence;
import bzh.plealog.bioinfo.api.data.searchresult.SRIteration;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.api.data.searchresult.SRParameters;
import bzh.plealog.bioinfo.api.data.searchresult.SRRequestInfo;
import bzh.plealog.bioinfo.api.data.searchresult.utils.SRFactory;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import fr.ifremer.bioinfo.resources.CmdMessages;

/**
 * Utility class to handle data produced by the PLAST engine. <br>
 * <br>
 * Code adapted from https://github.com/PLAST-software/plast-java-app <br>
 * <br>
 * PLAST java API, see http://plast.gforge.inria.fr/docs/java/
 * 
 * @author Patrick G. Durand, Ifremer
 */
public class PlastHandler implements IRequestListener {
  private int nb_hits;
  private int nb_hsps;
  private int nb_queries;
  private int nb_matching_queries;
  private SROutput _srOutput;
  private String _prgName;
  private String _dbName;
  private boolean _srOutputCreated;
  private boolean _firstTime;
  private SRFactory _srFactory;

  private static final String PLASTP = "plastp";
  private static final String PLASTN = "plastn";
  private static final String PLASTX = "plastx";
  private static final String TPLASTX = "tplastx";
  private static final String TPLASTN = "tplastn";

  /**
   * Constructor.
   */
  private PlastHandler() {
    _srFactory = CoreSystemConfigurator.getSRFactory();
    _srOutputCreated = false;
    nb_hits = nb_hsps = nb_queries = nb_matching_queries = 0;
    _firstTime = true;
  }

  /**
   * Constructor.
   * 
   * @param prgName
   *          name of the PLAST program
   * @param dbName
   *          name of the reference bank
   */
  public PlastHandler(String prgName, String dbName) {
    this();
    this._prgName = prgName;
    this._dbName = dbName;
  }

  /**
   * Get grand total of hits produced by PLAST. Call this method after PLAST job
   * has terminated.
   * 
   * @return number of hits
   */
  public int getHits() {
    return nb_hits;
  }

  /**
   * Get grand total of HSPs produced by PLAST. Call this method after PLAST job
   * has terminated.
   * 
   * @return number of HSPs
   */
  public int getHsps() {
    return nb_hsps;
  }

  /**
   * Get grand total of queries handled by PLAST. Call this method after PLAST job
   * has terminated.
   * 
   * @return number of queries
   */
  public int getQueries() {
    return nb_queries;
  }

  /**
   * Get total of matching queries. Call this method after PLAST job
   * has terminated.
   * 
   * @return number of queries
   */
  public int getMatchingQueries() {
    return nb_matching_queries;
  }

  /**
   * Get PLAST result. Call this method after PLAST job has terminated.
   * 
   * @return PLAST result. Never return null but it may be an empty SROutput
   *         object.
   */
  public SROutput getResult() {
    if (_srOutput == null)
      return CoreSystemConfigurator.getSRFactory().createBOutput();
    else
      return _srOutput;
  }

  /**
   * Return PLAST result type given PLAST request.
   * 
   * @param request
   *          PLAST request
   * 
   * @return one of SROutput.XXX values
   */
  private int getResultType(IRequest request) {

    String prgName = request.getProperties().getProperty(IRequest.ALGO_TYPE);
    if (PLASTP.equals(prgName)) {
      return SROutput.BLASTP;
    } else if (PLASTN.equals(prgName)) {
      return SROutput.BLASTN;
    } else if (PLASTX.equals(prgName)) {
      return SROutput.BLASTX;
    } else if (TPLASTX.equals(prgName)) {
      return SROutput.TBLASTX;
    } else if (TPLASTN.equals(prgName)) {
      return SROutput.TBLASTN;
    } else {
      return SROutput.UNKNOWN_PRGM;
    }
  }

  private SRIteration addIteration(SROutput srOutput, String queryID, String queryDef, int queryLength) {
    // Create Iteration object
    SRIteration iter = CoreSystemConfigurator.getSRFactory().createBIteration();
    iter.setIterationIterNum(srOutput.countIteration() + 1);
    iter.setIterationQueryID(queryID);
    iter.setIterationQueryDesc(queryDef);
    iter.setIterationQueryLength(queryLength);
    iter.setIterationStat(null);
    srOutput.addIteration(iter);
    
    return iter;
  }
  /**
   * Adds a new SRHit on a existing SRIteration object.
   * 
   * @param iteration
   *          the iteration on which to add the new SRHit
   * @param id
   *          the Hit id
   * @param definition
   *          the Hit definition line
   * @param length
   *          the Hit length
   */
  private SRHit addHit(SRIteration iteration, String id, String definition, int length) {
    SRHit hit;

    hit = _srFactory.createBHit();
    hit.setHitId(id);
    hit.setHitAccession(id);
    hit.setHitDef(definition);
    hit.setHitLen(length);
    hit.setHitNum(iteration.countHit() + 1);
    iteration.addHit(hit);
    return hit;
  }

  /**
   * Adds a new SRHsp on a existing SRHit object.
   * 
   * @param hit
   *          the hit on which to add the new SRHsp
   * @param score
   *          the scores
   * @param query
   *          the query sequence
   * @param subject
   *          the subject sequence
   * @param comp
   *          the comparison sequence sequence
   * @param isProteic
   *          pass true if sequence alignment if made of protein sequences, false
   *          otherwise.
   * 
   */
  private SRHsp addHsp(SRHit hit, SRHspScore score, SRHspSequence query, SRHspSequence subject, SRHspSequence comp,
      boolean isProteic) {
    SRHsp hsp;

    hsp = _srFactory.createBHsp();
    hsp.setHspNum(hit.countHsp() + 1);
    hsp.setScores(score);
    hsp.setQuery(query);
    hsp.setHit(subject);
    hsp.setMidline(comp);
    hsp.setProteic(isProteic);
    hit.addHsp(hsp);
    return hsp;
  }

  /**
   * Creates a new Score object.
   */
  private SRHspScore createSRHspScore(double evalue, double score, double bitscore, int identity, int positive,
      int gaps, int alignLength) {
    SRHspScore hscore;

    hscore = _srFactory.createBHspScore();
    hscore.setEvalue(evalue);
    hscore.setScore(score);
    hscore.setBitScore(bitscore);
    hscore.setIdentity(identity);
    hscore.setPositive(positive);
    hscore.setGaps(gaps);
    hscore.setAlignLen(alignLength);
    return hscore;
  }

  /**
   * Creates a new SRHspSequence object.
   * 
   * @param sequence
   *          the sequence. Can be null.
   * @param from
   *          starting coordinate of the alignment on the sequence.
   * @param to
   *          ending coordinate of the alignment on the sequence.
   * @param frame
   *          the frame.
   * @param seqFullSize
   *          the full size of the sequence
   * @param isCompSequence
   *          pass true if the sequence is the comparison sequence of the
   *          alignment.
   */
  private SRHspSequence createSRHspSequence(String sequence, int from, int to, int frame, int seqFullSize,
      boolean isCompSequence) {
    SRHspSequence seq;
    int gaps = 0, i, size;
    char ch;

    seq = _srFactory.createBHspSequence();
    seq.setSequence(sequence);
    if (frame < 0) {
      seq.setFrom(Math.max(from, to));
      seq.setTo(Math.min(from, to));
    } else {
      seq.setFrom(Math.min(from, to));
      seq.setTo(Math.max(from, to));
    }
    seq.setFrame(frame);
    seq.setSeqFullSize(seqFullSize);
    if (sequence != null && !isCompSequence) {
      size = sequence.length();
      for (i = 0; i < size; i++) {
        ch = sequence.charAt(i);
        if ((Character.isLetter(ch) || ch == '*') == false) {
          gaps++;
        }
      }
      seq.setGaps(gaps);
    }
    if (isCompSequence)
      seq.setType(SRHspSequence.TYPE_MIDLINE);
    else
      seq.setType(SRHspSequence.TYPE_ALIGNED_SEQ);
    return seq;
  }

  /**
   * Create a new SROutput object. In the context of a PLAST result, we create a
   * single SROutput containing a single SRIteration much like a standard BLAST
   * result. In turn, that SRIteration contains as many hits as queries provided
   * by the user to compare with reference banks.
   * 
   * @param request
   *          the PLAST request
   * @param query
   *          the first Query Result produce by PLAST
   * @param resultType
   *          one of SROutput.XXX values
   */
  private SROutput createSROutput(IRequest request, IQueryResult query, int resultType) {
    // Create Request Information Object
    SRRequestInfo ri = CoreSystemConfigurator.getSRFactory().createBRequestInfo();
    ri.setValue(SRRequestInfo.QUERY_ID_DESCRIPTOR_KEY, query.getSequence().getId());
    ri.setValue(SRRequestInfo.QUERY_DEF_DESCRIPTOR_KEY, query.getSequence().getDefinition());
    ri.setValue(SRRequestInfo.QUERY_LENGTH_DESCRIPTOR_KEY, query.getSequence().getLength());
    ri.setValue(SRRequestInfo.DATABASE_DESCRIPTOR_KEY, EZFileUtils.getFileName(new File(_dbName)));
    // switch plast name to blast to conform to Bioinformatics-Core-API SROutput API
    // otherwise will get into trouble with all other programs such as annotator
    ri.setValue(SRRequestInfo.PROGRAM_DESCRIPTOR_KEY, "b"+_prgName.substring(1));
    ri.setValue(SRRequestInfo.PRGM_VERSION_DESCRIPTOR_KEY, CmdMessages.getString("Tool.Plast.version"));
    ri.setValue(SRRequestInfo.PRGM_REFERENCE_DESCRIPTOR_KEY,  _prgName.toUpperCase() + ": " + 
        CmdMessages.getString("Tool.Plast.reference"));

    // Create Parameters Object
    SRParameters params = CoreSystemConfigurator.getSRFactory().createBParameters();
    for (Object key : request.getProperties().keySet()) {
      String value = request.getProperties().get(key).toString();
      params.setValue(key.toString(), value);
    }
    // following added to produced well-formed NCBI Blast XML
    params.setValue(SRParameters.EXPECT_DESCRIPTOR_KEY, request.getProperties().get(IRequest.EVALUE));
    params.setValue(SRParameters.GAPEXTEND_DESCRIPTOR_KEY, request.getProperties().get(IRequest.EXTEND_GAP_COST));
    params.setValue(SRParameters.GAPOPEN_DESCRIPTOR_KEY, request.getProperties().get(IRequest.OPEN_GAP_COST));
    params.setValue(SRParameters.MATRIX_DESCRIPTOR_KEY, request.getProperties().get(IRequest.SCORE_MATRIX));

    // create Result Object
    SROutput srOutput = CoreSystemConfigurator.getSRFactory().createBOutput();
    srOutput.setRequestInfo(ri);
    srOutput.setBlastOutputParam(params);
    srOutput.setBlastType(resultType);

    return srOutput;
  }

  /**
   * Notification received when the Plast algorithm has finished a part.
   * 
   * In the context of a PLAST job execution, results may be provided in several
   * parts, i.e. this method can be called several times by the underlying c++
   * native PLAST execution engine.
   * 
   * @param request
   *          the ongoing request
   * @param result
   *          result of the query
   */
  @Override
  public void requestResultAvailable(IRequest request, IRequestResult result) {
    /*
     * This is the key part of the software: each time this method is called by the
     * PLAST Java/c++ gateway, some results are available. During a single job,
     * PLAST slices the query and report results for each slice. So this method is
     * usually called several times by PLAST to report all results, even for small
     * queries.
     */
    int resultType = getResultType(request);
    boolean isProteic = resultType != SROutput.BLASTN;
    SRIteration srIter = null;
    SRHit srHit = null;
    SRHspScore srHspScores = null;
    SRHspSequence srHspQSeq = null;
    SRHspSequence srHspHSeq = null;

    /* a results may contain hits for one or more queries... */
    while (result.hasNext()) {
      IQueryResult query = result.next();
      nb_queries++;

      /* the following test: see method comment. */
      if (_srOutputCreated == false) {
        _srOutputCreated = true;
        _srOutput = createSROutput(request, query, resultType);
      }

      srIter = addIteration(_srOutput, query.getSequence().getId(), 
          query.getSequence().getDefinition(), query.getSequence().getLength());
      
      if (query.hasNext()) {
        nb_matching_queries++;
      }
      /* ... for each query we may have several hits... */
      while (query.hasNext()) {
        org.inria.genscale.dbscan.api.IHit hit = query.next();
        nb_hits++;

        srHit = addHit(srIter, hit.getSequence().getId(), hit.getSequence().getDefinition(),
            hit.getSequence().getLength());

        /* ... for each hit, we may have several HSPs. */
        while (hit.hasNext()) {
          IHsp hsp = hit.next();
          nb_hsps++;

          srHspScores = createSRHspScore(hsp.getEvalue(), hsp.getScore(), hsp.getBitScore(), hsp.getIdentity(),
              hsp.getPositive(), hsp.getGaps(), hsp.getAlignLen());
          srHspQSeq = createSRHspSequence(null, hsp.getQueryFrom(), hsp.getQueryTo(), hsp.getQueryFrame(),
              hsp.getQuerySequence().getLength(), false);
          srHspHSeq = createSRHspSequence(null, hsp.getHitFrom(), hsp.getHitTo(), hsp.getHitFrame(),
              hsp.getHitSequence().getLength(), false);
          addHsp(srHit, srHspScores, srHspQSeq, srHspHSeq, null, isProteic);
        }
      }
    }
  }

  /**
   * Implementation of IRequestListener interface.
   */
  @Override
  public void requestCancelled(IRequest request) {
  }

  /**
   * Implementation of IRequestListener interface.
   */
  @Override
  public void requestExecInfoAvailable(IRequest request, Properties execInfo) {
    if (_firstTime) {
      _firstTime = false;
      PlastRunner.LOGGER.info("Parameters used by PLAST are:");
      PlastRunner.LOGGER.info(request.getProperties());
    }
    // provide a Double value in 0..1 range
    String prop = request.getExecInfo().getProperty("exec_percent");
    if (prop != null) {
      Double progress = Double.valueOf(prop) * 100.d;
      String msg = String.format(CmdMessages.getString("Tool.Plast.msg4"), progress);
      LoggerCentral.info(PlastRunner.LOGGER, msg);
    }
  }

  /**
   * Implementation of IRequestListener interface.
   */
  @Override
  public void requestFinished(IRequest request) {
    _srOutput.initialize();
    PlastRunner.LOGGER.info("PLAST execution done!");
  }

  /**
   * Implementation of IRequestListener interface.
   */
  @Override
  public void requestStarted(IRequest request) {
    PlastRunner.LOGGER.info("PLAST execution started!");
  }

}
