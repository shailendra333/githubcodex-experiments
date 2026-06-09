@echo off
:: ============================================================
:: Start CSV Upload Demo — HARD 500 MB Heap Cap
:: ============================================================
:: Techniques applied at the JVM level:
::   -Xms512m -Xmx512m          → fixed heap: no heap expansion, hard 500 MB cap
::   -XX:+UseG1GC               → G1 handles moderate heap sizes well
::   -XX:MaxGCPauseMillis=100   → target GC pauses under 100 ms
::   -XX:InitiatingHeapOccupancyPercent=30
::                              → trigger GC earlier (30% full vs default 45%)
::                                so heap never reaches danger zone before collecting
::   -XX:G1HeapRegionSize=4m    → 4 MB regions suit a 512 MB heap
::   -XX:+ExplicitGCInvokesConcurrent
::                              → System.gc() calls use concurrent GC (non-blocking)
::   -Xlog:gc*:file=logs/gc.log:time,uptime,level
::                              → GC log written to logs/gc.log for post-run analysis
:: ============================================================

SET JAR=target\csv-upload-demo-1.0-SNAPSHOT.jar

SET JVM_OPTS=-Xms512m -Xmx512m ^
 -XX:+UseG1GC ^
 -XX:MaxGCPauseMillis=100 ^
 -XX:InitiatingHeapOccupancyPercent=30 ^
 -XX:G1HeapRegionSize=4m ^
 -XX:+ExplicitGCInvokesConcurrent ^
 -Xlog:gc*:file=logs/gc.log:time,uptime,level

SET DB_OPTS=--spring.datasource.url="jdbc:sqlite:./data/customers.db?journal_mode=WAL&synchronous=NORMAL&cache_size=10000&temp_store=MEMORY"

if not exist logs mkdir logs

echo ============================================================
echo  CSV Upload Demo  ^|  Heap: 512 MB hard cap
echo  GC: G1GC  ^|  IHOP: 30%%  ^|  GC log: logs\gc.log
echo  Open: http://localhost:8080
echo ============================================================
echo.

java %JVM_OPTS% -jar %JAR% %DB_OPTS%
