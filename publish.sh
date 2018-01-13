#!/bin/bash
lein clean
lein immutant war
scp target/scraper.war humpre:/srv/wildfly/standalone/deployments/scraper/
