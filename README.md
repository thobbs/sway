# sway

A generative art mural design for the GitHub offices in Bellevue, Washington.

This work was inspired by the ecosystems surrounding Bellevue, both aquatic and terrestrial. It is also a reminder that the new digital environments we build for ourselves must be constructed with love and care.

## dependencies

To run this project, you'll need the standard setup for [Clojure](https://clojure.org/) (i.e. the JDK), and specifically the [lein](https://leiningen.org/) build system.

This project utilizes my [genartlib](https://github.com/thobbs/genartlib) libraryand [Quil](https://github.com/quil/quil), a Clojure wrapper around [Processing](https://processing.org). Those dependencies should all be handled automatically by lein.

## running

I normally work with the lein repl. From the project directory, run:

    lein repl

Once the repl starts up, you can run the code with:

    (use 'sway.core)

This will take about a minute to run. Once it has finished, you should see the artwork on your screen and an image saved in the directory.

After making any code changes to `dynamic.clj`, you can re-run the code by running:

    (refresh)

This will result in a new image being saved.

### tinkering

To adjust the image size, edit `core.clj`. Note that any changes to the aspect ratio may result in a different image being generated.

To change the behavior of the algorithm, you'll want to edit `dynamic.clj`.

If you don't care about matching the original mural image, I recommend switching from having the fixed `seed` and instead letting `(System/nanoTime)` be used to pick the seed.

## license

Copyright Anticlassic Studios LLC, 2021. All rights reserved.
