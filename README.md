# BGPSecX: a Software-Defined Networking approach to enhance BGP security

## Introduction

BGPSecX is an SDN-based architecture to make origin authetication and path validation to enhance BGP security, and have as targets the Internet Exchange Points (IXP). Includes secure communication channels between IXPs to enable collaboration. SDN shifts the computational burden from routers to the logically-centralised controller, while enabling BGP not to be changed. Targeting IXPs and promoting inter-IXP collaboration enables hard-to-obtain security guarantees (such as path validation) and the creation of incentives to foster adoption of BGP security services. To ensure several security aspects, It uses blockchain as main database.

To validate the effectiveness and the potential value of BGPSecX we performed simulations using public BGP and IXP data. The objective is to assess empirically how BGPSECx compares with existing solutions in terms of security. Thus, this document brings information of how to replicate the evaluation tests using simulation process from empiracal data.

## Datasets

For the simulation we uses three datatasets. First, is the peering interconnections. For this, we have gathered IXP data from PeeringDB project. We imported only information from IXPs which had more than 50 peers (the largest IXP in our dataset had 731 participants). This filtering resulted in 68 IXPs, and the total number of ASes connected to these 68 IXPs adds up to 4147. To analyse also partial adoption by ASes, we divide this dataset into six subsets as follows: ASes-1%, ASes-10%, ASes-25%, ASes-50%, ASes-75%, ASes-100%. The first considers that only 1% of the ASes adhere to BGPSecX; the second, 10%; etc. This selection is made randomly (the ASes from the dataset), and the randomisation process is run 10 times. In the following, we call this group of 6 datasets the **IXP-dataset**

The second source of information we need for our simulation is BGP routing data – the **Routing-dataset**. This is the routing information exchanged between BGP peers that is present in the Network Layer Reachability Information (NLRI) attribute of the BGP UPDATE messages. To obtain these data we resorted to the Routing Information Service of RIPE NCC, through the Route Information Service (RIS).

In this repository we brings information to replicate evaluation, as well as some results already done and can be see in "**results_ris-day-20170901**" directory. For this results, we have gathered Routing-dataset from all available collectors on September 1st, 2017, during a 24-hour period. In this dataset we making several filters, as example, to remove repeated BGP updates records and others. We used 18 collectors as shows in the table below:

| Dataset | # Prefixes | Collector | Locality |
| :-: | -: | :- | :- |
| RRC00 | 2529651 | RIPE NCC | Amsterdam/NL |
| RRC01 | 1144225 | LINX | London/UK |
| RRC03 | 1155696 | AMS-IX/NL-IX | Amsterdam/NL |
| RRC04 | 177211 | CIXP/CERN | Geneva/CH |
| RRC05 | 576562 | VIX | Vienna/AT |
| RRC06 | 333994 | JPIX | Otemachi/JP |
| RRC07 | 902634 | NETNOD | Stockholm/SE |
| RRC10 | 1667219 | MIX | Milan/IT |
| RRC11 | 1498630 | NYIIX | New York/US |
| RRC12 | 305586 | DE-CIX | Frankfurt/DE |
| RRC13 | 3211712 | MSK/IX | Moscow/RU |
| RRC14 | 126781 | PAIX | Palo Alto/US |
| RRC15 | 21099355 | PTTMetro-SP | São Paulo/BR |
| RRC16 | 440137 | NOTA | Miami/US |
| RRC18 | 322127 | CATNIX | Barcelona/ES |
| RRC19 | 2185501 | NAP/JB | Johannesburg/ZA |
| RRC20 | 185703  | SwissIX | Zurich/CH |
| RRC21 | 2912439 | FranceIX | Paris/FR |

The last dataset corresponds to data on Route Origin Authorizations (ROAs). This will be used as a base of comparison against BGPSECx. ROAs are digital certificates that associate a network prefix (IP address block) with a given ASN (AS number) They are supported by the Resource Public Key Infrastructure (RPKI) and its creation requires a registration process. The ROA database is maintained by Regional Internet Registries (RIRs). To generate this dataset we make use of a RIPE NCC tool called the RPKI Validator. This tool enables the retrieval of all from the the five RIRs to form this dataset. We made the export on October, 2017, and up to 37572 ROA's. For future reference, this dataset is referred to as **ROA-dataset**.

## Prerequisites

To start the simulation you needs to have a Unix-like operation system or similar environment with resorces below:

- Java JRE 8 or higher ro run JAR files.
- Gnuplot application to plot chart from CSV file generated by the simulation tests.
- Git application to download the BGPSecX datasets and java codes from repository.
- BGPSecX applications and datasets from repository. Due the Routing-dataset to be very large (around 102 Mbytes), we don't put it in the git. Below we get it from using "wget" application (with shorten url).

For reference, the simulations tests was made in a Ubuntu 16.04 and the instalation procedures of artefacts above can be done as below (we consider the home user as the work directory and symbol "$" is the bash prompt):

```sh
$ cd ~
$ sudo apt-get install oracle-java8-installer gnuplot git wget
$ git clone https://www.github.com/netx-ulx/BGPSecX
$ cd BGPSecX/evaluation/datasets
$ wget https://goo.gl/FBStNR
$ tar -zxvf ris_dataset_20170901.zip -C ./
```
If you wish to make evaluation using Routing-dataset of another day, you need to download the file from RIPE/RIS and applied some filters. For it, you need to ignore the last two commands above and make the procecedures as explained at the next subsection. If no, you should to jump to the "Evaluation tests" section.

## Getting and processing a new Routing-dataset

This procedure comprise the procedures to get and to treat the Routing-dataset of a certain day from RIPE/RIS. RIS files uses MRT format defined by the RFC6396 and comprises a binary files. For the our propose, we needs to convert to a clear text format, then to apply some filters. The procedure is as follow:

**a) Downloading file from RIS/RIPE**

There are several ways to download files from RIS/RIPE project. We suggest the commands below to make the download for the all files considering BGPSecX directory structure. In the last command below you should to change letters YYYY, MM, and DD by you desired date, where YYYY is the year, MM is the month (00-12) and DD is the day (01-31).

```sh
$ cd ~
$ cd BGPSecX/evaluation/datasets
$ wget -x -nc http://data.ris.ripe.net/rrc{00..21}/YYYY.MM/updates.YYYYMMDD.{00..23}{00..55..5}.gz
```

**b) Parsing MRT files and building ASN/RPKI datasets**

For this procedure we'll make the dump of all downloaded files by RRC using MRTPARSE tool (it is a pre-requirement and we make its download below). After that we filter lines whose type are route announces and get only AS_PATH and NLRI, then to write two new datasets, one containing per line only the AS_PATH (to validate OA, PV and PEV by BGPSecX) and other with the origin ASN in the AS_PATH and NLRI (to simulate the validation of the RPKI). For it, use the follow commands. The last command is a bash script that automates the process:

```sh
$ cd ~
$ cd BGPSecX
$ git clone https://github.com/t2mune/mrtparse
$ evaluation/codes/bash-scripts/build_dataset_from-ris-ripe.sh <begin_dataset> <end_dataset> <desired_day>
```
In the last command above have three parameters, are: **< begin | end >_dataset** like RCC00 to RRC21. The third paramenter, **desired_day** is the day of deseired dataset to build in the format YYYYMMDD.

## Evaluation tests
### Origin Authetication, Path-validation and Path-end Validation
Done the environment, to start the simulation test we need the files below from BGPSecX repository:

- IXP-Dataset (datasets/peering_4147_asn.txt) and Routing-dataset (datasets/rrc**xx**_20170901.path, where xx is the number of dataset. First, need decompress it).
- Java code to process simulation algorithm and its configuration file (java/jar/BgpSecXValidator and cfg/validate.cfg).
- Java code to make chart and its configuration file (java/jar/BgpSecXChartPlot and cfg/chatplot.cfg).

Procedures to start validation:

```sh
$ cd ~
$ cd BGPSecX
$ java -jar evaluation/codes/java/jar/BgpSecXValidator.jar evaluation/cfg/validate.cfg 3
```
The last command above process all validations, i.e., Origin Authetication, Path-validation and Path-end Validation. But, you may to process one of a time, only changing the last parameter.

The sintax of applications is: **java -jar BGPSecXValidator.jar < cfg_path >  < type_val >**

First parameter must be the path/name of configuration file. In the configuration file, the first line must be the file path of IXP-dataset, and in each others lines, the file path of each Routing-dataset files. Second one must be the type of validation like 0=OA, 1=PV, 2=PEV or 3=ALL.

Finished validations, the generated CSVs files are stored in a new path called "csv". The files are named as rrc**xx**_20160901_**yy**.csv, where xx is the number of routing dataset and yy is the type of validations, i.e., oa, pv or pev. After this, we may to plot the chart as below:

```sh
$ java -jar evaluation/codes/java/jar/BgpSecXGroupedChart.jar evaluation/cfg/grouped-chart.cfg 3 6
```

The sintaxe of last command is: ***java -jar BGPSecXGroupedChart.jar < cfg_path > < val_type > < #data_per_chart >***

First parameter must be the path/name of configuration file. Second one must be the type of validation like 0=OA, 1=PV, 2=PEV or 3=ALL. Third one must be the number of datasets per chart. Charts are generated in "charts" folder.

The plotted charts above put only a type of validation per image, like OA or PV or PEV. If you want to plot a mixed chart like OA, PV and PEV in the same image you should to proceed as below:

```sh
$ java -jar evaluation/codes/java/jar/BgpSecXMixedChart.jar valuation/cfg/mixed-chart.cfg 3
```

The sintaxe of last command is: **java -jar BGPSecXMixedChart.jar < cfg_path > < val_type >**

First parameter must be the path/name of configuration file. Second one must be the type of validation like 0=OA, 1=PV, 2=PEV or 3=ALL.

### RPKI Validation

This validation will be used as a base of comparison against BGPSecX. to start the simulation test we need the files below from BGPSecX repository:

- ROA-Dataset (datasets/roa_rpki_validator_20170701.json - it was exported from RIPE RPKI Validator tool) and Routing-dataset (datasets/rrc**xx**_20160901.rpki, where xx is the number of dataset. It was decompressed in previous, but if not, need decompress it).
- Java code to process simulation algorithm and its configuration file (java/jar/BgpSecXRpkiValidator and cfg/rpki_validate.cfg)
- A bash script to run gnuplot application to generate the charts (gnuplot/BgpSecXChartPlot).

Procedures to start validation:

```sh
$ cd ~
$ cd BGPSecX
$ java -jar evaluation/codes/java/jar/BgpSecXRpkiValidator.jar evaluation/cfg/rpki_validate.cfg
```

The last command above process RPKI validations according the datasets present in configuration file. The sintaxe is simple and need as parameter only the path of configuration file. In the configuration file, the first line must be the file path of ROA-dataset, and in each others lines, the file path of each Routing-dataset files.

As this task spend more time than previous validation, we can run many times the last command. However, it needs to have one configuration file for each execution, and in each configuration file we put a wanted Routing-dataset. This comes to improve the use of computation resources, once which the java application don't implement multithread to validation multiples datasets.

Finished validations, the generated CSVs files are stored in a new path (or already existing) called "csv". The files are named as rpki_gnuplot_**x**-**y**.csv, where x is the number of file and y the total of files. After this, we may to plot the chart as below:

```sh
$ evaluation/codes/bash-scripts/plot_rpki_chart.sh <path/csv_file>
```

**path/csv_file** is the path of CSV file generated by previous validation process. Charts are generated in folder caled charts. Each chart can have up to 6 datasets.

## Final remarks

That is all. However we observe which the processing time of validations depends of computation resource. Normaly, for OA, PV and PEV is about 1-hour for a x86/I7 single core. But for the OA-RPKI simulation, the time can reach days if simulation is done for all datasets using a single process.
