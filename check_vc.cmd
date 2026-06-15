@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x64
echo VCToolsInstallDir=%VCToolsInstallDir%
where cl
where nmake

