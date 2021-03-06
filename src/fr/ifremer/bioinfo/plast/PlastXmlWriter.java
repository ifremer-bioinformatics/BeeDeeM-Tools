package fr.ifremer.bioinfo.plast;

import java.io.IOException;
import java.io.Writer;

public class PlastXmlWriter {

  private Writer _writer;
  
  public void printline(int depth, String str) throws IOException {
    for(int i=0;i<depth;i++) {
      _writer.write(" ");
    }
    _writer.write(str);
    _writer.write("\n");
  }
}

/*

#include <alignment/visitors/impl/XmlOutputVisitor.hpp>

#include <misc/api/PlastStrings.hpp>
#include <misc/api/PlastStringsRepository.hpp>
#include <misc/api/version.hpp>

#include <database/api/ISequenceDatabase.hpp>

#include <stdio.h>
#define DEBUG(a)  //printf a

#define MAX_HEADER_SIZE 2048
#define BUFFER_SIZE 2512

using namespace std;
using namespace database;
using namespace alignment;
using namespace alignment::core;

namespace alignment {
namespace visitors  {
namespace impl      {

XmlOutputVisitor::XmlOutputVisitor (std::ostream* ostream)
    : OstreamVisitor(ostream), _nbQuery (0), _nbSubject (0), _nbAlign(0)
{
    printline (0, "<?xml version=\"1.0\" ?>");
    printline (0, "<BlastOutput>");
    printline (1, "<BlastOutput_iterations>");
}

XmlOutputVisitor::XmlOutputVisitor (const std::string& uri, dp::IProperties *props)
    : OstreamVisitor(uri), _nbQuery (0), _nbSubject (0), _nbAlign(0)
{
    _properties = props;
}

XmlOutputVisitor::~XmlOutputVisitor ()
{
    if (_nbSubject > 0)
    {
        printline (5, "</Hit_hsps>");
        printline (4, "</Hit>");
    }

    if (_nbQuery > 0)
    {
        printline (3, "</Iteration_hits>");
        printline (2, "</Iteration>");
    }

    printline (1, "</BlastOutput_iterations>");
    printline (0, "</BlastOutput>");
}

void XmlOutputVisitor::visitQuerySequence (
    const database::ISequence* seq,
    const misc::ProgressInfo& progress
)
{
    if(_nbQuery==0){

        printline (0, "<?xml version=\"1.0\" ?>");
        printline (0, "<BlastOutput>");
        printline (1, "<BlastOutput_program>%s</BlastOutput_program>",
                   _properties->getProperty (STR_OPTION_ALGO_TYPE)->getValue().c_str());
        printline (1, "<BlastOutput_version>%s %s [%s]</BlastOutput_version>",
                      _properties->getProperty (STR_OPTION_ALGO_TYPE)->getValue().c_str(),
                      PLAST_VERSION,
                      PLAST_BUILD_DATE);
        printline (1, "<BlastOutput_reference>%s</BlastOutput_reference>",
                   STR_PLAST_REFERENCE);

        std::string dbName = _properties->getProperty (STR_OPTION_SUBJECT_URI)->getValue();
        if (dbName.size()>=MAX_HEADER_SIZE){
            dbName = dbName.substr(0, MAX_HEADER_SIZE);
            dbName[MAX_HEADER_SIZE-1]='.';
            dbName[MAX_HEADER_SIZE-2]='.';
            dbName[MAX_HEADER_SIZE-3]='.';
        }
        printline (1, "<BlastOutput_db>%s</BlastOutput_db>", dbName.c_str());
        printline (1, "<BlastOutput_query-ID>Query_1</BlastOutput_query-ID>");
        printline (1, "<BlastOutput_query-def>%s</BlastOutput_query-def>", seq->getDefinition().c_str());
        printline (1, "<BlastOutput_query-len>%d</BlastOutput_query-len>", seq->getLength());
        printline (1, "<BlastOutput_query-seq>not specified</BlastOutput_query-seq>");
        printline (1, "<BlastOutput_param>");
        printline (2, "<Parameters>");
        printParameter(STR_OPTION_SCORE_MATRIX, "matrix");
        printParameter(STR_OPTION_EVALUE, "expect");
        printParameter(STR_OPTION_OPEN_GAP_COST, "gap-open");
        printParameter(STR_OPTION_EXTEND_GAP_COST, "gap-extend");
        printParameter(STR_OPTION_FILTER_QUERY, "filter");
        printline (2, "</Parameters>");
        printline (1, "</BlastOutput_param>");
        printline (1, "<BlastOutput_iterations>");
    }

    if (_nbSubject > 0)
    {
        printline (5, "</Hit_hsps>");
        printline (4, "</Hit>");
    }

    if (_nbQuery > 0)
    {
        printline (3, "</Iteration_hits>");
        printline (2, "</Iteration>");
    }

    _currentQuery = seq;
    _nbQuery++;
    _nbSubject = 0;

    printline (2, "<Iteration>");
    printline (3, "<Iteration_iter-num>%d</Iteration_iter-num>", _nbQuery);
    printline (3, "<Iteration_query-ID>Query_%d</Iteration_query-ID>", _nbQuery);
    printline (3, "<Iteration_query-def>%s</Iteration_query-def>", _currentQuery->getComment().c_str());
    printline (3, "<Iteration_query-len>%d</Iteration_query-len>", _currentQuery->getLength());
    printline (3, "<Iteration_hits>");
}

void XmlOutputVisitor::visitSubjectSequence (
    const database::ISequence* seq,
    const misc::ProgressInfo& progress
)
{
    const char * sID, * sDef;
    std::string subjectName;
    size_t pos;

    if (_nbSubject > 0)
    {
        printline (5, "</Hit_hsps>");
        printline (4, "</Hit>");
    }
    _currentSubject = seq;
    _nbSubject++;
    _nbAlign = 0;

    // get Fasta header of subject sequence, then find out whether or not
    // we have ID and definition. this code was added to fix a bug in XML
    // format generation: using _currentSubject->getDefinition() provides
    // location in blast db volume (plast search against blast formatted db)
    subjectName = _currentSubject->getComment();
    pos = subjectName.find(" ");
    if (pos!=std::string::npos){
        sID = subjectName.substr(0, pos).c_str();
        sDef = subjectName.substr(pos+1).c_str();
    }
    else{
        sID = subjectName.c_str();
        sDef = sID;
    }
    printline (4, "<Hit>");
    printline (5, "<Hit_num>%d</Hit_num>", _nbSubject);
    printline (5, "<Hit_id>%s</Hit_id>", sID);
    printline (5, "<Hit_def>%s</Hit_def>", sDef);
    printline (5, "<Hit_accession>%s</Hit_accession>", sID);
    printline (5, "<Hit_len>%d</Hit_len>", _currentSubject->getLength());
    printline (5, "<Hit_hsps>");
}

void XmlOutputVisitor::visitAlignment (
    core::Alignment* align,
    const misc::ProgressInfo& progress
)
{
    _nbAlign++;

    printline (6, "<Hsp>");

    printline (7, "<Hsp_num>%d</Hsp_num>",                  _nbAlign);
    printline (7, "<Hsp_bit-score>%g</Hsp_bit-score>",      align->getBitScore());
    printline (7, "<Hsp_score>%d</Hsp_score>",              align->getScore());
    printline (7, "<Hsp_evalue>%g</Hsp_evalue>",            align->getEvalue());
    printline (7, "<Hsp_query-from>%d</Hsp_query-from>",    align->getRange(Alignment::QUERY).begin + 1);
    printline (7, "<Hsp_query-to>%d</Hsp_query-to>",        align->getRange(Alignment::QUERY).end   + 1);
    printline (7, "<Hsp_hit-from>%d</Hsp_hit-from>",        align->getRange(Alignment::SUBJECT).begin + 1);
    printline (7, "<Hsp_hit-to>%d</Hsp_hit-to>",            align->getRange(Alignment::SUBJECT).end   + 1);
    printline (7, "<Hsp_query-frame>%d</Hsp_query-frame>",  align->getFrame(Alignment::QUERY));
    printline (7, "<Hsp_hit-frame>%d</Hsp_hit-frame>",      align->getFrame(Alignment::SUBJECT));
    printline (7, "<Hsp_identity>%d</Hsp_identity>",        align->getNbIdentities());
    printline (7, "<Hsp_positive>%d</Hsp_positive>",        align->getNbPositives());
    printline (7, "<Hsp_gaps>%d</Hsp_gaps>",                align->getNbGaps());
    printline (7, "<Hsp_align-len>%d</Hsp_align-len>",      align->getLength());
    printline (7, "<Hsp_qseq/>");
    printline (7, "<Hsp_hseq/>");
    printline (7, "<Hsp_midline/>");
    printline (6, "</Hsp>");
}

void XmlOutputVisitor::printline (size_t depth, const char* format, ...)
{
    for (size_t i=0; i<depth; i++)   { getStream() << "  ";  }

    char buffer[BUFFER_SIZE];

    va_list va;
    va_start (va, format);
    vsprintf (buffer, format, va);
    va_end (va);
    getStream() << buffer << endl;
}

void XmlOutputVisitor::printParameter (const char* key, const char* field)
{
    dp::IProperty* algoProp = _properties->getProperty (key);
    if (algoProp != 0){
        printline (3, "<Parameters_%s>%s</Parameters_%s>",
                   field, algoProp->getValue().c_str(), field);
    }
}


*/