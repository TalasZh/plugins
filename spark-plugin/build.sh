#!/usr/bin/env bash
mvn clean install && scp spark-api/target/spark-plugin-api-4.0.0-RC7.jar spark-cli/target/spark-plugin-cli-4.0.0-RC7.jar spark-impl/target/spark-plugin-impl-4.0.0-RC7.jar spark-rest/target/spark-plugin-rest-4.0.0-RC7.jar root@management4.critical-factor.com:/root/kudayar/subutai-4.0.0-RC7/deploy
