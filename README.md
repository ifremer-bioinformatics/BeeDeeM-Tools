# *BeeDeeM-Tools*: making easy sequence-based data analysis pipelines 

[![License AGPL](https://img.shields.io/badge/license-Affero%20GPL%203.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.txt) [![](https://img.shields.io/badge/platform-Java--1.8+-yellow.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) [![](https://img.shields.io/badge/run_on-Linux--Mac_OSX-yellowgreen.svg)]()

## What for?

This project is a suite of command-line tools that can be used to setup a sequence annotation pipeline. 

It provides:

* a tool to cut large sequence files into smaller ones (i.e. slice or page of sequences)
* a tool to index sequence (Fasta, Genbank, Genpept, ENA, EMBL, Uniprot and related)
* a tool to query index and get back only sequences of interest
* a tool to run [PLAST](http://plast.inria.fr/) high-performance sequence comparison tool
* a tool to export legacy NCBI BLAST XML results (BLAST or PLAST) as CSV files

All these tools are suited to handle the annotation data available from annotated databanks, among others: NCBI Taxonomy Gene Ontology, InterPro and Enzyme Commission classifications.

## Requirements

* Linux or macOS 64bits computer.
* Java Runtime 1.8+ from Oracle. 

Use a [Java Virtual Machine](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8 (or above) from Oracle. 

*Not tested with any other JVM providers but Oracle... so there is no guarantee that the software will work as expected if not using Oracle's JVM.* 

## Installation
 
### Check your JVM first

On a command-line simply type:

```
java -version
```
An Oracle JVM will display this line:

`Java HotSpot(TM) 64-Bit Server VM`

You do not see that line? Do not go further: we do not guarantee BeeDeeM-Tools be compatible with OpenJDK, gcj, *etc.*

### Procedure

On a command-line simply type the following lines:

```
cd <where-you-want-to-install-the-software>
mkdir beedeem-tools-2.0.0
cd beedeem-tools-2.0.0
wget https://cloud.ifremer.fr/index.php/s/WQGxuP7bbbA5mFH/download -O beedeem-tools-2.0.0.tar.gz
gunzip beedeem-tools-2.0.0.tar.gz
tar -xf beedeem-tools-2.0.0.tar
rm beedeem-tools-2.0.0.tar
chmod +x *.sh
```

That's all folks!

Let's do a little check that software is working fine:

```
./test.sh -w /tmp
```

This will run a complete test of all BeeDeeM-Tools.

## License and dependencies

*BeeDeeM-Tools* itself is released under the GNU Affero General Public License, Version 3.0. [AGPL](https://www.gnu.org/licenses/agpl-3.0.txt)

It depends on several thrid-party libraries as stated in the NOTICE.txt file provided with this project.

--
(c) 2018 - Patrick G. Durand, Ifremer

