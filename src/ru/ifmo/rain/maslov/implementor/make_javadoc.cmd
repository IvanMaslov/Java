SET javadoc_path=D:\paradigm19\paradigm20\implerdoc
SET doc_link=https://docs.oracle.com/en/java/javase/13/docs/api
SET impl=D:\paradigm19\paradigm20\src\info\kgeorgiy\java\advanced\implementor\
SET package=D:\paradigm19\paradigm20\src\
SET libs=D:\paradigm19\paradigm20\src\

javadoc -d %javadoc_path% -link %doc_link% -cp %package%;%libs%; -private -author -version ru.ifmo.rain.maslov.implementor %impl%Impler.java %impl%ImplerException.java %impl%JarImpler.java
