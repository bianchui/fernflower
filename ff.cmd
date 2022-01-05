@echo off
setlocal
set FF_VER=v26
set OUT_DIR=%~dpn1_ff_%FF_VER%%2
md %OUT_DIR%
pushd %~dp1
echo %OUT_DIR%
call:fun_timestamp "%date%" "%time%" TM_START

java -jar %~dp0fernflower_%FF_VER%.jar -dgs=true -ind="    " -ren=true -urc=com.github.bianchui.ff.renamer.DebugRenamer -nls=1 %1 %OUT_DIR% > %OUT_DIR%\%~nx1.log 2>&1

call:fun_timestamp "%date%" "%time%" TM_END
set /A TM_TAKEN=%TM_END%-%TM_START%
echo Take %TM_TAKEN% seconds.
goto :eof

:fun_timestamp
set vdate=%~1
set vtime=%~2
if not defined vdate (set vdate=%date%)
if not defined vtime (set vtime=%time%)
call :fun_calcSeconds %vdate:~0,10% %vtime% %3
goto :eof

:fun_calcSeconds
set vdate=%~1
set vtime=%~2
set yy=%vdate:~0,4% & set mm=%vdate:~5,2% & set dd=%vdate:~8,2%
rem 02=>2
set /a dd=100%dd%%%100, mm=100%mm%%%100
rem days from 1970-01-01
set /a mm-=2
if %mm% LEQ 0 (
    set /a mm+=12
    set /a yy-=1
)
set /a days=%yy%/4 - %yy%/100 + %yy%/400 + 367*%mm%/12 + %dd% + %yy%*365 - 719499

rem 9:10:12=>09:10:12
if "%vtime:~1,1%" == ":" (set "vtime=0%vtime%")
set hh=%vtime:~0,2% & set nn=%vtime:~3,2% & set ss=%vtime:~6,2%

rem 09=>9
set /a hh=100%hh%%%100, nn=100%nn%%%100, ss=100%ss%%%100

rem seconds from 1970-01-01 00:00:00, and GMT+8
set /a secs=days*86400 + hh*3600 + nn*60 + ss - 8 * 60 * 60
set "%3=%secs%"
goto :EOF
