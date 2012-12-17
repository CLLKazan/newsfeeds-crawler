#!/bin/bash
java -Xmx256m -cp lib/${project.build.finalName}.jar ru.kfu.itis.issst.nfcrawler.Bootstrap "$@"