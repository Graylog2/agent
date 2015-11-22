Graylog Collector ChangeLog
===========================

## v0.5.0 (unreleased)

* Add UDP support to GELF output. (#59)

## v0.4.2 (2016-01-04)

* Fix concurrency issue in MessageBuilder/MessageFields.

## v0.4.1 (2015-09-07)

* Fix file rotation detection on platforms with a slow watchservice
  implementation like Mac OS X and AIX.

## v0.4.0 (2015-07-23)

* Log operating system version and platform on startup.
* More resource usage improvements by using less threads per input/output.
* Add static message-fields support. (#45)

## v0.3.0 (2015-07-14)

* Lots of bug fixes regarding resource usage.
* Wildcard support for file inputs. (#24, #42)
* The GELF output does not send a `level` field for messages from file inputs
  anymore. It was hardcoded to INFO before.
* Improved example configuration file.

## v0.2.5 (2015-07-01)

* Fix classpath for Windows service to unbreak Sigar. (#39)

## v0.2.4 (2015-06-24)

* Fix several quoting issues for Windows start/service scripts. (#28)
* Rename some variables in Linux startup scripts.

## v0.2.3 (2015-06-23)

* Replace readlink usage with manually resolving symbolic links. (#20)
* Do not stop the collector if the configured file does not exist. (#33)

## v0.2.2 (2015-06-02)

* Improve Windows batch script compatibility. (#15)

## v0.2.1 (2015-05-20)

* Fixed problem with release infrastructure.

## v0.2.0 (2015-05-20)

* Support for reading Windows eventlog.
* Connect to Graylog server API and send heartbeats.
* Make content splitter for file input configurable.
* Auto detect line endings in newline content splitter. (CRLF vs. LF)
* Add scripts to run on Windows and to install as a Windows service.
* Add charset support to the file input.
* Improved file input regarding following files.
* Improved performance when reading from files.

## v0.1.1 (2015-02-18)

* Fix validation messages.

## v0.1.0 (2015-02-18)

* Initial release.
