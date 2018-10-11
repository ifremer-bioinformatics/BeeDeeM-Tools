# General idea 

The idea of BeeDeeM-Tools came from that key issue: "using a cluster, how to efficiently compare thousands of sequences/contigs against a huge reference bank, such as Uniprot-TrEMBL?"

For such a job, one has to slice the query file, then run a sequence comparison of each slice against a reference bank; each job would be executed in parallel on separate cluster nodes. Given the results of these first comparisons, we could take sequences of non-matching queries, then run additional comparison jobs using another reference bank. 

All these tasks can be done using BeeDeeM-Tools, as follows:

* slicing sequence files is done using `cut.sh` BeeDeeM-Tool.
* sequence comparisons can be achieved using `plast.sh` that relies on the [PLAST](https://plast.inria.fr/) software made by Inria.
* special sequence retrieval is done using `index.sh` and `query.sh` BeeDeeM-Tool. 
* finally, `dumpcsv.sh` is a companion tool of PLAST to convert standard XML result to a CSV table.


# Tutorial

We'll illustrate the use of BeeDeeM-tools using a "two-step-bank-to-bank-sequence-comparison" strategy. The main idea is: we first compare many queries to a small reference bank, then we complete our results by a second sequence comparison of "non-matching" queries vs. a larger reference bank.

We'll proceed through that strategy as follows:

1. prepare a SwissProt reference bank
1. prepare a query for distributed PLASTx sequence comparisons
1. run PLASTx jobs
1. get non matching query IDs from results
1. use those IDs to get FASTA formatted sequences 
1. run additional PLASTx comparisons against a larger bank, TrEMBL

## Prepare a working directory

Open a Unix terminal and proceed as follows.

First of all, let's prepare a working directory to put all data: query sequences, reference bank and results.

```
cd <a-working-directory>
mkdir bdm-tools-tuto
cd bdm-tools-tuto
```

## Get a reference data

Let's retrieve SwissProt (BLAST format from NCBI). We'll use that BLAST bank as the reference for sequence comparisons.

```
wget ftp://ftp.ncbi.nlm.nih.gov/blast/db/swissprot.tar.gz
gunzip swissprot.tar.gz
tar -xf swissprot.tar
rm swissprot.tar
```

## Get a query data set

To illustrate sequence analysis, let's get the set of CDS of Rattus norvegicus. We'll use that FASTA file as the query.

```
wget ftp://ftp.ncbi.nlm.nih.gov/genomes/genbank/vertebrate_mammalian/Rattus_norvegicus/all_assembly_versions/GCA_000002265.1_ASM226v1/GCA_000002265.1_ASM226v1_cds_from_genomic.fna.gz
gunzip GCA_000002265.1_ASM226v1_cds_from_genomic.fna.gz
grep -c ">" GCA_000002265.1_ASM226v1_cds_from_genomic.fna
```
## Index the query file

Index the query file by sequence ID. 

```
# adapt to your system: replace $SCRATCH by any other valid tmp storage place
<path-to-bdm-tools>/index.sh -w $SCRATCH -d query-idx -i GCA_000002265.1_ASM226v1_cds_from_genomic.fna
```
This index will be used to get query sequences by IDs.

## Slice the query 

How many sequences do we have in the query?

```
grep -c ">" GCA_000002265.1_ASM226v1_cds_from_genomic.fna
```

Answer is: `44059 sequnces`.

So, let's slice the query into smaller FASTA files in order to distribute them on many cluster nodes. In this example, we cut the original FASTA file into slices of 5,000 sequences each.

```
# adapt to your system: replace $SCRATCH by any other valid tmp storage place
<path-to-bdm-tools>/cut.sh -w $SCRATCH -i GCA_000002265.1_ASM226v1_cds_from_genomic.fna -p 5000
```

## Run a PLASTx job

*Note: below, we just show how to run a single PLAST job using a single FASTA slice as the query... we let you adapt this tutorial to automate the process: prepare slices, then submitting PLAST jobs to your cluster.*

```
# adapt to your system: use an appropriate working directory for PLAST
# beware to use multi-thread safe path !!!
export KL_WORKING_DIR=$SCRATCH/plast-job-1
# compare a query slice to Swissprot (background job)
<path-to-bdm-tools>/plast.sh -p plastx -i GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1 -d swissprot.pal -o GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.xml -maxhits 20 -maxhsps 1 -e 1e-3 -a 4 -seeds 1 &
```
It is worth noting that PLAST creates a log file during its execution and you can monitor it as follows:
```
# monitor the PLAST log file: there is a progress monitor
tail -f $KL_WORKING_DIR/PlastRunner.log
# then, wait for PLAST to finish...
```

## Dump result as CSV format and get non-matching query IDs

When PLAST job has terminated, we can convert the standard XML results into a CSV file for further processing.

```
<path-to-bdm-tools>/dumpcsv.sh -i GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.xml -o GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.csv
```
It is worth noting that this CSV file contains both matching and non-matching query IDs. Later ones can be identified as follows:

```
"lcl|CM000231.2_cds_EDL93691.1_13","-","-","-","-","-","-","-","-","-","-","-","-","-"
```

So, it is quite easy to get all of them:

```
# get non matching query IDs
grep -e '"-","-"' GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.csv | cut -d',' -f 1 | uniq | sed -e 's/^"//' -e 's/"$//' > GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.nomatch.uniq.txt
# count non-matching query IDs
wc -l GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.nomatch.uniq.txt
```

Finally, we get the corresponding query sequences by querying the index:

```
<path-to-bdm-tools>/query.sh -w $SCRATCH -d query-idx/GCA_000002265.1_ASM226v1_cds_from_genomic.fna.ld -f GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.nomatch.uniq.txt > GCA_000002265.1_ASM226v1_cds_from_genomic.fna_1.nomatch.fna
```

From there, we could run an additional PLASTx run against a larger bank, e.g. Uniprot_TrEMBL... step easy to do, so it is now shown here.

