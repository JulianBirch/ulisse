# ulisse

A Codox writer that does syntax highlighting.  Take a look at [an example of the output](http://julianbirch.github.io/arianna/arianna.html).

## Usage

Leiningen

```clj
:profiles {:dev {:dependencies [ [net.colourcoding/ulisse "0.2.3"]]}}
:codox {:writer ulisse.writer/write-docs}
```

After that, just use it as if it were `[codox-md "0.2.0"]` (which it basically is).  Be aware that it does provide its own `codox/css/codox-md.css` file, and you'll need to copy the styles through to your own if you're already using one.

## Easter Eggs

In `ulisse.markdown` you can find functions for converting markdown to HTML with syntax highlighting.  In `ulisse.python` you'll find functions for invoking Jython from Clojure.  One of these days that may be the main focus of the library.  For now it's the bare minimum that `ulisse.markdown` needs.

Basically, there's three things this library does, and it should ultimately only do one.

The hard work is being done by the following libraries:
 * [codox-md](https://github.com/hugoduncan/codox-md)
 * [endophile](https://github.com/theJohnnyBrown/endophile)
 * [pygments](http://pygments.org/)  (Because why should Clojure have all the fun.)

## License

Copyright © 2013 Julian Birch

Distributed under the Eclipse Public License, the same as Clojure.
