#!/bin/bash
#
if [[ $# -eq 0 ]] ; then
   clear
   echo -e "Sintax: plot_rpki_chart.sh <path/file_of_csv_data_to_plot>\n"
   exit
fi
#
filename=$(basename "$1")
#
gnuplot -persist <<-EOFMarker
reset
set terminal post eps size 6,3 enhanced color 18
#set terminal png
set encoding utf8
set style data histogram
set style histogram cluster gap 1
set style line 2 lc rgb 'black' lt 1 lw 2
set style fill pattern border 0
set boxwidth 0.8
set key outside horizontal top center
set yrange [0:105]
set datafile separator ";"
set ylabel "Percentage of verification (%)" offset 2,0
set xtics font "Bold,," 
set ylabel font "Bold,," 
set offset -0.4,-0.4,0,0
set output "$filename.eps"
set grid ytics mytics
set mytics 2
plot "$1" using 2:xtic(1) t "Verified" ls 2, \
	'' u 0:2:2 with labels center offset -2.8,0.6 font "Bold,," notitle, \
        '' using 3:xtic(1) t "Not verified" ls 2, \
	'' u 0:3:3 with labels center offset 2.8,0.6 font "Bold,," notitle
EOFMarker
