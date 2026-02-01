@echo off

REM --- Settings ---
set REPO=https://github.com/meowcat767/Shiori.git
set CLONE_DIR=%~dp0ShioriNative
set GRAALVM_ZIP=%~dp0graalvm.zip
set GRAALVM_DIR=%~dp0graalvm
set GRAALVM_URL=https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_windows-x64_bin.zip

REM --- Step 1: Download GraalVM if not exists ---
if not exist "%GRAALVM_ZIP%" (
    echo Downloading GraalVM...
    powershell -Command "Invoke-WebRequest -Uri '%GRAALVM_URL%' -OutFile '%GRAALVM_ZIP%'"
) else (
    echo GraalVM zip already exists.
)

REM --- Step 2: Extract GraalVM ---
if not exist "%GRAALVM_DIR%" (
    echo Extracting GraalVM...
    powershell -Command "Expand-Archive -Path '%GRAALVM_ZIP%' -DestinationPath '%GRAALVM_DIR%'"
) else (
    echo GraalVM already extracted.
)

REM --- Step 3: Set PATH for GraalVM ---
set PATH=%GRAALVM_DIR%\graalvm-jdk-25\bin;%PATH%

REM --- Step 4: Install native-image component ---
echo Installing GraalVM native-image component...
gu install native-image

REM --- Step 5: Clone or update Shiori repository ---
if exist "%CLONE_DIR%" (
    echo Updating existing Shiori repository...
    cd "%CLONE_DIR%"
    git pull
) else (
    echo Cloning Shiori repository...
    git clone %REPO% "%CLONE_DIR%"
    cd "%CLONE_DIR%"
)

REM --- Step 6: Build JAR with Maven ---
echo Building project...
mvn clean package -DskipTests

REM --- Step 7: Find the JAR ---
for %%F in (target\*.jar) do set MAIN_JAR=%%F

if not defined MAIN_JAR (
    echo ERROR: No JAR found in target directory!
    pause
    exit /b 1
)

echo Found JAR: %MAIN_JAR%

REM --- Step 8: Build native image ---
echo Compiling native image...
native-image ^
    --no-fallback ^
    --enable-all-security-services ^
    --report-unsupported-elements-at-runtime ^
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes.graphbuilderconf=ALL-UNNAMED ^
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.nodes=ALL-UNNAMED ^
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.api=ALL-UNNAMED ^
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.util=ALL-UNNAMED ^
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.word=ALL-UNNAMED ^
    -cp "%MAIN_JAR%" ^
    Main

REM --- Done ---
echo Native image created: shiori-native.exe
pause
