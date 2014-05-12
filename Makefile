basic:
	@ant
	@./rank.sh 2013.data baseline 

cosine:
	@ant
	@./rank.sh 2013.data cosine

bm25:
	@ant
	@./rank.sh 2013.data bm25

window:
	@ant
	@./rank.sh 2013.2013 window
