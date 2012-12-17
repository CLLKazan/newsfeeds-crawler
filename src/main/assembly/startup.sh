#!/bin/bash
java -Xmx256m -cp lib/${project.build.finalName}.jar -Dlogback.configurationFile=logback.xml ru.kfu.itis.issst.nfcrawler.Bootstrap "$@"