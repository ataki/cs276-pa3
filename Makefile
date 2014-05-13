cosine:
	@ant
	@./rank.sh 2013.data/queryDocTrainData cosine 

score:
	@./score.sh ./fake.result.txt 2013.data/queryDocTrainRel

bm25:
	@ant
	@./rank.sh 2013.data/queryDocTrainData bm25

