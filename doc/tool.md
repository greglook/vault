Vault Command Line Tool
=======================

Vault comes with a command-line tool for interacting with the system. To avoid
long startup costs (and to preserve things such as in-memory indexes) the tool
uses [grenchman](http://leiningen.org/grench.html) to communicate with a running
REPL process.

```bash
$ cd /path/to/vault
$ lein trampoline repl :headless
$ alias vault="$PWD/bin/vault"
$ vault help
```

Use `-h` `--help` or `help` to show usage information for any command. General
usage is similar to git, with nested subcommands for various types of actions.

## Configuration

Vault needs to know a number of things to be useful. All of the configuration is
stored in `$HOME/.config/vault/` by default. Currently, the main configuration
is to specify blob stores in `$HOME/.config/vault/blob-stores.edn`:

```clojure
{:default :local

 :local
 (file-store "/home/USER/var/vault")}
```

## Blobs

The most basic usage of the command line tool is to interact with blobs
directly. Here you can see the blob store contains a number of blobs already:

```
% vault blob list
sha256:2a6e83a925c7dbbe3305be30bae5fceb03328f2a4e18fac18e687c46d5659d96
sha256:2f72cc11a6fcd0271ecef8c61056ee1eb1243be3805bf9a9df98f92f7636b05c
sha256:84a4b19e19aa4e2a562ae0286b1e188ef4f4f9a98a92b8730d20a1e0f2882523
sha256:97df3588b5a3f24babc3851b372f0ba71a9dcdded43b14b9d06961bfc1707d9d
sha256:dc2c12477854b5719356c6413cb6b61880d89c38b7865a98601ea624202234a8
```

The `put` command will read from STDIN until terminated by C-d. Alternately,
pipe some text into it. It returns the blobref to the stored content.

```
% vault blob put
I made a blob for the README!
sha256:e0f2c726eca178f80ba12ff3720ba03c01f02f7a6e979ba78e3f26b9b522056a
```

Similar to git, you only need to give enough characters of the prefix to
uniquely identify a blob:

```
% vault blob stat e0f2
sha256:e0f2c726eca178f80ba12ff3720ba03c01f02f7a6e979ba78e3f26b9b522056a
#vault.tool/blob
{:content nil
 :id #vault/ref "sha256:e0f2c726eca178f80ba12ff3720ba03c01f02f7a6e979ba78e3f26b9b522056a"
 :stat/origin #uri "file:/home/$USER/vault/dev/var/blobs/sha256/e0f/2c7/sha256-e0f2c726eca178f80ba12ff3720ba03c01f02f7a6e979ba78e3f26b9b522056a"
 :stat/size 30
 :stat/stored-at #inst "2014-05-26T04:58:56.000Z"}

% vault blob get e0f
I made a blob for the README!
```
