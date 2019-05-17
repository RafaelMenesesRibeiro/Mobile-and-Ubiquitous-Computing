@echo off

::
:: Absolute path to the Termite CLI tool
::
setx TERMITE_CLI_PATH "C:\\Users\\rafae\\Desktop\\Mobile-and-Ubiquitous-Computing\\Termite-Cli" /m

::
:: Target platform; one of: mac, linux, or windows
::
setx TERMITE_PLATFORM windows /m

termite.bat