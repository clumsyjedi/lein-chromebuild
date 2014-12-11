# lein-chromebuild

A Leiningen plugin to build Chrome browser extensions.

## Usage

If you're working on Chrome extensions in ClojureScript then this plugin
applies the familiar syntax of lein-cljsbuild to the rest of the Chrome
extension paths.

If you're looking for an entry point to setting up a Chrome extension project,
have a look at the [chrome-extension lein template](https://github.com/clumsyjedi/lein-chrome-extension).

Assuming your non-cljs files are located in resources/ - the following config 
targets them for bundling in target/unpacked alongside the compiled JS.

```clj
(defproject my-project "0.1.0"
  :plugins [[lein-chromebuild "0.2.1"]]
  :cljsbuild {:builds {:main
                        {:source-paths ["src"]
                         :compiler {:output-to "target/unpacked/my_project.js"
                         :output-dir "target/js"
                         :optimizations :whitespace
                         :pretty-print true}}}}
  :chromebuild {:resource-paths ["resources/js" "resources/html" "resources/images"]
                :target-path "target/unpacked"}
  )
```

To create an unpacked Chrome extension once:

    $ lein chromebuild once

To create an unpacked Chrome extension automaticaly when files change:

    $ lein chromebuild auto

To cleanup files created by the above operations:

    $ lein chromebuild clean

## License

Copyright © 2014 Frazer Irving

Released under the Eclipse Public License - v 1.0.
