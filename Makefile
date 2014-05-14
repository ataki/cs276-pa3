cosine:
	@ant
	@./rank.sh 2013.data/queryDocTrainData cosine 
	@./score.sh ./results.txt 2013.data/queryDocTrainRel

score:
	@./score.sh ./results.txt 2013.data/queryDocTrainRel

bm25:
	@ant
	@./rank.sh 2013.data/queryDocTrainData bm25
	@./score.sh ./results.txt 2013.data/queryDocTrainRel

window:
	@ant
	@./rank.sh 2013.data/queryDocTrainData window
	@./score.sh ./results.txt 2013.data/queryDocTrainRel

