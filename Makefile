baseline:
	@ant
	@./rank.sh data/pa3.signal.train baseline
	@./score.sh ./results.txt data/pa3.rel.train

cosine:
	@ant
	@./rank.sh data/pa3.signal.train cosine
	@./score.sh ./results.txt data/pa3.rel.train


score:
	@./score.sh ./results.txt data/pa3.rel.train

bm25:
	@ant
	@./rank.sh data/pa3.signal.train bm25
	@./score.sh ./results.txt data/pa3.rel.train

window:
	@ant
	@./rank.sh data/pa3.signal.train window
	@./score.sh ./results.txt data/pa3.rel.train
