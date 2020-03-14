SET project=D:\paradigm19\paradigm20\
cd %project%

SET korneev=info\kgeorgiy\java\advanced\
SET maslov=ru\ifmo\rain\maslov\

SET javadoc_path=implerdoc
SET doc_link=https://docs.oracle.com/en/java/javase/13/docs/api
SET impl=src\%korneev%implementor\
SET package=src\
SET libs=src\

javadoc -d %javadoc_path% -link %doc_link% -cp %package%;%libs%; -private -author -version ru.ifmo.rain.maslov.implementor %impl%Impler.java %impl%ImplerException.java %impl%JarImpler.java
