SET project=D:\paradigm19\paradigm20\
cd %project%

SET korneev=info\kgeorgiy\java\advanced\
SET maslov=ru\ifmo\rain\maslov\

SET impl=src\%korneev%implementor\
SET real=src\%maslov%implementor\
SET libs=src\
SET package=src\
SET res=out\production\fake

javac -d %res% -cp %libs%; %real%Implementor.java %impl%\*.java

cd %res%

jar xf %res%/%%Impler.class %res%/ImplerException.class %res%/JarImpler.class
jar cmf Implementor.jar %package%\ru\ifmo\rain\maslov\implementor\Manifest.txt

cd %project%