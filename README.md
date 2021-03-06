Sismics Music [![Build Status](https://secure.travis-ci.org/sismics/music.png)](http://travis-ci.org/sismics/music)
=============

What is Music?
---------------

Music is an open source, Web-based music server.

Music is written in Java, and may be run on any operating system with Java support.

Features
--------

- Organize your music collection

See <http://www.sismics.com/music/> for a list of features and benefits.

Downloads
---------

Compiled installers are available here for each new versions: <https://sourceforge.net/projects/sismicsmusic/files/release/>

License
-------

Music is released under the terms of the GPL license. See `COPYING` for more
information or see <http://opensource.org/licenses/GPL-2.0>.

Translations
------------

- English
- French

How to build Music from the sources
------------------------------------

Prerequisites: JDK 7, Maven 3

Music is organized in several Maven modules:

  - music-parent
  - music-core
  - music-web
  - music-web-common
  - music-android

First off, clone the repository: `git clone git://github.com/sismics/music.git`
or download the sources from GitHub.

#### Launch the build

From the `music-parent` directory:

    mvn clean -DskipTests install

#### Run a stand-alone version

From the `music-web` directory:

    mvn jetty:run

#### Build a .war to deploy to your servlet container

From the `music-web` directory:

    mvn -Pprod -DskipTests clean install

You will get your deployable WAR in the `target` directory.
