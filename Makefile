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

# -----------------------------------------------

baselineDev:
	@ant
	@./rank.sh data/pa3.signal.dev baseline
	@./score.sh ./results.txt data/pa3.rel.dev
	
cosineDev:
	@ant
	@./rank.sh data/pa3.signal.dev cosine
	@./score.sh ./results.txt data/pa3.rel.dev

scoreDev:
	@./score.sh ./results.txt data/pa3.rel.dev

bm25Dev:
	@ant
	@./rank.sh data/pa3.signal.dev bm25
	@./score.sh ./results.txt data/pa3.rel.dev

windowDev:
	@ant
	@./rank.sh data/pa3.signal.dev window
	@./score.sh ./results.txt data/pa3.rel.dev


