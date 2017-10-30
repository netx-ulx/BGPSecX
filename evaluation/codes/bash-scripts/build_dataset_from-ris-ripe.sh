#!/bin/bash
#
if [ $# -eq 3 ]
then
   START_RRC="$1"
   END_RRC="$2"
   EVAL_DAY="$3"
#
   clear
   for i in $(eval echo "{$START_RRC..$END_RRC}")
   do
      cd "$HOME/datasets/data.ris.ripe.net/rrc$i/2017.09" 
      echo "Work directory: $(pwd)"
      for j in `ls -1 updates.*.gz | sort`; do
         echo "Unzip file: $j"  
         gzip -d $j
	 file_name=$(echo $j |  sed "s/.[^.]*$//")
         echo "Parsing the file: $file_name"
	 python ../../../../mrtparse/examples/mrt2exabgp.py -g $file_name >> $file_name-parsed
	 # grep filter only line with "A"nnounces and by cut, get only the 6 and 7 collumn (NLRI and AS_PATH)
         python ../../../../mrtparse/examples/mrt2bgpdump.py $file_name | grep "|A|" | cut -d"|" -f6-7 >> rrc$i-parsed
	 # Get only announces, so discard withdraw and other register types
	 echo "Removing duplicates..."
	 awk '!a[$0]++' temp > rrc$i-parsed-nlri-aspath
	 /bin/rm -rf temp
	 cat rrc$i-parsed-nlri-aspath | cut -d"|" -f2 > rrc$1_$EVAL_DAY.path
	 # Read line by line and build PATH (for OA/PV by BGPSecX) and RPKI (for RPKI simulations) datasets
	 echo "Building routing datasets (PATH and RPKI)"
	 while IFS='' read -r line || [[ -n "$line" ]]; do
		NLRI=$(echo $line | /usr/bin/cut -d"|" -f1)
		PATH=$(echo $line | /usr/bin/cut -d"|" -f2)
  		ORIGIN_ASN=$(echo $PATH | /usr/bin/awk '{print $NF}')
		echo $PATH >> rrc$1_$EVAL_DAY.path
		echo "$ORIGIN_ASN $NLRI" >> rrc$1_$EVAL_DAY.rpki
	 done < "rrc$i-parsed-nlri-aspath"
      #rm -rf updates.*.gz
   done
else
   echo "Wrong number of parameters. You needs to pass two integers, START and END, as parameter."
fi

