SET project=D:\paradigm19\paradigm20\
cd %project%

SET korneev=info\kgeorgiy\java\advanced\
SET maslov=ru\ifmo\rain\maslov\

SET impl=src\info\kgeorgiy\java\advanced\implementor\
SET package=src\
SET libs=src\
SET res=out\production\

javac -d %res% -cp %libs%; Implementor.java %impl%\*.java %impl%\*

cd %res%

jar xf %res%/%%Impler.class %res%/ImplerException.class %res%/JarImpler.class
jar cmf Implementor.jar %package%\ru\ifmo\rain\maslov\implementor\Manifest.txt

cd %package%