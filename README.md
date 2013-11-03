# Vault

A Clojure library and application to store documents in a content-addressable
datastore while maintaining a secure history of entity values. See the docs for
more detailed explanations of the various pieces.

This is heavily inspired by the [Camlistore](http://camlistore.org/) project.
Vault does not aim to be (directly) compatible with Camlistore, but many of the
concepts are similar, and the stored metadata would probably not be that hard
to convert.

## Usage

For now, there's no built in script to call, so just make an alias for
leiningen:

```shell
alias vault='lein run --'
vault help
```

Use `-h` `--help` or `help` to show usage information for any command. General
usage is similar to git, with nested subcommands for various types of actions.

## License

This is free and unencumbered software released into the public domain.
See the UNLICENSE file for more information.
