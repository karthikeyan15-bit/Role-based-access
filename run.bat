@echo off
echo ========================================
echo  College Portal RBAC - Build and Run
echo ========================================

:: Compile
echo Compiling Java sources...
if not exist out\classes mkdir out\classes
javac --release 21 -d out\classes src\main\java\com\college\rbac\*.java src\main\java\com\college\rbac\model\*.java src\main\java\com\college\rbac\service\*.java src\main\java\com\college\rbac\controller\*.java src\main\java\com\college\rbac\util\*.java

if %errorlevel% neq 0 (
    echo COMPILATION FAILED!
    pause
    exit /b 1
)
echo Compilation successful!

:: Copy web resources
echo Copying web resources...
if exist out\classes\web rmdir /s /q out\classes\web
xcopy /s /e /i /q src\main\resources\web out\classes\web

:: Run
echo.
echo Starting server at http://localhost:8080
echo Press Ctrl+C to stop.
echo.
java -cp out\classes com.college.rbac.Main
