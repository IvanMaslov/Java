SET javadoc_path=D:\paradigm19\paradigm20\implerdoc
SET impl=D:\paradigm19\paradigm20\src\info\kgeorgiy\java\advanced\implementor\
SET package=D:\paradigm19\paradigm20\src\
SET libs=D:\paradigm19\paradigm20\src\
SET res=D:\paradigm19\paradigm20\out\production\fake

SET korneev=info\kgeorgiy\java\advanced\
SET maslov=ru\ifmo\rain\maslov\

javac -d %res% -cp %libs%; Implementor.java %impl%\*.java %impl%\*

cd %res%

jar xf %res%/%%Impler.class %res%/ImplerException.class %res%/JarImpler.class
jar cmf Implementor.jar %package%\ru\ifmo\rain\maslov\implementor\Manifest.txt

cd %package%\%maslov%implementor