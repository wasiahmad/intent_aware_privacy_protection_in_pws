## Intent-aware Privacy Protection in Personalized Web Search

<p align="justify">
In this work, we propose a client-centered intent-aware query obfuscation solution for privacy protection in a personalized web search scenario, where Bayes-Optimal Privacy is achieved. In our solution, each user query is submitted with k additional cover queries and corresponding clicks, which act as decoys to mask users' genuine search intent from a search engine. The set of cover queries are sampled from a set of hierarchical language models, which encode users' search intent and empower us to tackle sequentially developed user intents in a search task. And the clicks are randomly sampled according to a distribution of click positions. Our approach emphasizes the plausibility of generated cover queries, not only to the current genuine query but also to previous queries in the same task, to increase the complexity for a (malicious) search engine to identify a user's true intent. In addition, client-side re-ranking is performed to compensate the degenerated search quality from the search engine because of the obfuscated search intent. Comprehensive experiments including rigorous comparisons with state-of-the-art query obfuscation techniques are performed on the public AOL search log, and the propitious results substantiated the effectiveness of our solution.
<p align="justify">

<p align="center">
<br>
<img src="https://writelatex.s3.amazonaws.com/jvwbdghbgxtv/uploads/7386/9674788/1.png" width="90%">
<br>Figure: Intent-aware Query-obfuscation for Privacy-protection (IQP) Framework
<p align="center">
