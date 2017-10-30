#!/usr/bin/env gnuplot
reset
#set terminal epslatex color dashed "default" 18
set terminal post eps size 7,3 enhanced color 18
#set terminal png
set encoding utf8
set style data histogram
set style histogram cluster gap 1
set style line 2 lc rgb 'black' lt 1 lw 2
set style fill pattern border 0
set boxwidth 0.8
#set xtic rotate by -8 scale 0
set key outside horizontal top center
set yrange [0:105]
set datafile separator ";"
#set xlabel ""
set ylabel "Percentage of validation (%)" offset 2,0
set xtics font "Bold,," 
set ylabel font "Bold,," 
set offset -0.4,-0.4,0,0
set output "rpki_2Values_2-3.eps"
set grid ytics mytics
set mytics 2
plot 'rpki_2Values_2-3.csv' using 2:xtic(1) t "Valid" ls 2, \
	'' u 0:2:2 with labels center offset -2.2,0.6 font "Bold,," notitle, \
        '' using 3:xtic(1) t "Invalid" ls 2, \
	'' u 0:3:3 with labels center offset 2.2,0.6 font "Bold,," notitle
