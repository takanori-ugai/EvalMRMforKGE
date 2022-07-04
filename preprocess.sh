rm export/*.txt
rm benchmarks/KGRC-RDF-star/*
java -jar URI2ID.jar kgrc_all_rdf-star.tsv 8 1 1
cp export/*.txt benchmarks/KGRC-RDF-star/
cp benchmarks/FB15K/n-n.py benchmarks/KGRC-RDF-star/
cd benchmarks/KGRC-RDF-star
echo "run n-n.py"
python n-n.py
