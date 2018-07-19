plast-reference-v2.3.1.txt produced as follows:

1. PLAST binary from: 

   https://github.com/PLAST-software/plast-library/releases/plastbinary_linux_v2.3.1.tar.gz
   
   There is no pre-compiled release of PLAST 2.3.0 for Linux, so we use 2.3.1. 
   As a reminder, this Java program running PLAST engine relies on 2.3.0 version of PLAST
   Linux library (see /native folder of this project).
   Not a problem: there is NO differences in PLAST engine code between these two releases; 
   review release notes at https://github.com/PLAST-software/plast-library/releases
   
2. command-line:

   plast -p plastp \
         -i /home1/datahome/pgdurand/devel/ifremer/BeeDeeM-Tools/tests/databank/plast/query.fa \
         -d /home1/datahome/pgdurand/devel/ifremer/BeeDeeM-Tools/tests/databank/plast/tursiops.fa \
         -o /home1/datahome/pgdurand/devel/ifremer/BeeDeeM-Tools/tests/databank/plast/plast-reference-v2.3.1.txt \
         -max-hit-per-query 5 -max-hsp-per-hit 1 -force-query-order 1000 -e 1e-5 -seeds-use-ratio 0.01 -F F -a 1

   CAUTION: run PLAST on a single core to generate data file that can be used for comparison!
--
July 2018