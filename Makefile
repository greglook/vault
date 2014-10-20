DOCS_DIR=target/doc

.PHONY: clean docs

clean:
	rm -rf target

$(DOCS_DIR):
	mkdir -p $(DOCS_DIR)
	git clone git@github.com:greglook/vault.git $(DOCS_DIR)
	cd doc && git symbolic-ref HEAD refs/heads/gh-pages
	rm $(DOCS_DIR)/.git/index
	cd doc && git clean -fdx

docs: $(DOCS_DIR)
	lein docs
